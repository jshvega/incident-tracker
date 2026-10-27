import os
import pg8000.dbapi

LADDER = {"low":"high", "medium":"high", "high":"critical", "critical":"critical"}

CANDIDATES_SQL = """
    SELECT id, title, incident_severity, due_at
    FROM incidents
    WHERE incident_status IN ('open', 'investigating')
    AND due_at < now()
    AND NOT sla_breached
    ORDER BY id
"""

ESCALATE_SQL = """
    UPDATE incidents
    SET incident_severity = %s::severity,
        sla_breached = true,
        updated_at = now()
    WHERE id = %s
    AND incident_status IN ('open', 'investigating')
    AND due_at < now()
    AND NOT sla_breached
    AND incident_severity = %s::severity
    RETURNING id
"""

INSERT_ESCALATION_SQL = """
    INSERT INTO escalations (incident_id, previous_severity, new_severity, reason)
    VALUES (%s, %s::severity, %s::severity, %s)
"""

def load_config() -> dict:
    return {
        "database": os.environ["DB_NAME"],
        "host": os.environ["DB_HOST"],
        "port": int(os.environ["DB_PORT"]),
        "user": os.environ["DB_USER"],
        "password": os.environ["DB_PASSWORD"]
    }

def fetch_candidates(conn) -> list:
    cursor = conn.cursor()
    cursor.execute(CANDIDATES_SQL)
    results = cursor.fetchall()
    conn.commit()
    return results

def escalate_one(conn, incident_id:int, severity:str) -> str:
    new_severity = LADDER[severity]
    cursor = conn.cursor()
    cursor.execute(ESCALATE_SQL, (new_severity, incident_id, severity))
    row = cursor.fetchone()

    if row is None:
        conn.rollback()
        return "skipped"
    if new_severity != severity:
        reason = f"SLA breached, {severity} -> {new_severity}"
        cursor.execute(INSERT_ESCALATION_SQL, (incident_id, severity, new_severity, reason))
        conn.commit()
        return "escalated"
    else:
        conn.commit()
        return "breach_only"
    
def run_sweep(conn):
    candidates = fetch_candidates(conn)
    counts = {"escalated":0, "breach_only":0, "skipped":0, "failed":0}

    for row in candidates:
        incident_id, title, severity, due_at = row
        try:
            outcome = escalate_one(conn, incident_id, severity)
            counts[outcome] += 1
            print(incident_id, outcome)
        except Exception as e:
            conn.rollback()
            counts["failed"] += 1
            print(incident_id, "failed:", e)

    print(counts)
    return counts


if __name__ == '__main__':
    from dotenv import load_dotenv
    load_dotenv()

    conn = pg8000.dbapi.connect(**load_config())

    try:
        run_sweep(conn)
    finally:
        conn.close()