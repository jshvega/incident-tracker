import os
import pg8000.dbapi
import json
import logging
import sys
import time
import uuid




log = logging.getLogger("sweep")

class JsonFormatter(logging.Formatter):
    def format(self, record):
        entry = {"level": record.levelname, "msg": record.getMessage()}
        entry.update(getattr(record, "fields", {}))   # custom keys
        if record.exc_info:
            entry["exc"] = self.formatException(record.exc_info)
        return json.dumps(entry, default=str)




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
    
def run_sweep(conn, run_id:str) -> dict:
    start = time.perf_counter()
    candidates = fetch_candidates(conn)
    counts = {"escalated":0, "breach_only":0, "skipped":0, "failed":0}

    for row in candidates:
        incident_id, title, severity, due_at = row
        try:
            outcome = escalate_one(conn, incident_id, severity)
            counts[outcome] += 1
            log.info(outcome, extra={"fields": {"run_id": run_id, "incident_id": incident_id, "severity": severity}})
        except Exception as e:
            conn.rollback()
            counts["failed"] += 1
            log.exception("failed", extra={"fields": {"run_id": run_id, "incident_id": incident_id, "severity": severity, "error": str(e)}})

    duration_ms = int((time.perf_counter() - start) * 1000)
    log.info("sweep_complete", extra={"fields": {"run_id": run_id, "candidates": len(candidates), **counts, "duration_ms": duration_ms}})
    return counts

def setup_logging() -> None:
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(JsonFormatter())
    logging.basicConfig(level=logging.INFO, handlers=[handler], force=True)

def get_conn(run_id:str):
    global conn
    if conn is None:
        conn = pg8000.dbapi.connect(**load_config())
        log.info("db_connected", extra={"fields": {"run_id": run_id}})
    return conn

def handler(event, context):
    global conn
    run_id = context.aws_request_id if context else uuid.uuid4().hex

    try:
        counts = run_sweep(get_conn(run_id), run_id)
        return counts
    except Exception as e:
        if isinstance(e, pg8000.dbapi.InterfaceError):
            conn = None
        log.exception("sweep_failed", extra={"fields": {"run_id": run_id, "error": str(e)}})
        raise




setup_logging()
conn = None

if __name__ == '__main__':
    from dotenv import load_dotenv
    load_dotenv()

    handler(None, None)
    handler(None, None)