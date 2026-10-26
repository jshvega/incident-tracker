import os
import pg8000.dbapi

CANDIDATES_SQL = """
    SELECT id, title, incident_severity, due_at
    FROM incidents
    WHERE incident_status IN ('open', 'investigating')
    AND due_at < now()
    AND NOT sla_breached
    ORDER BY id
"""

def load_config() -> dict:
    return {
        "database": os.environ["DB_NAME"],
        "host": os.environ["DB_HOST"],
        "port": int(os.environ["DB_PORT"]),
        "user": os.environ["DB_USER"],
        "password": os.environ["DB_PASSWORD"]
    }

def fetch_candidates(config: dict) -> list:
    conn = pg8000.dbapi.connect(**config)
    try:
        cursor = conn.cursor()
        cursor.execute(CANDIDATES_SQL)
        results = cursor.fetchall()
        return results
    finally:
        conn.close()
    

if __name__ == '__main__':
    from dotenv import load_dotenv
    load_dotenv()

    rows = fetch_candidates(load_config())
    for row in rows:
        print(row)
    print(len(rows))