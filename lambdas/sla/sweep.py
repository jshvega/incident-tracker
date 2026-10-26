import os
import pg8000.dbapi

OPEN_INCIDENTS_SQL = """
    SELECT id, title, incident_severity, incident_status, due_at
    FROM incidents
    WHERE incident_status IN ('open', 'investigating')
    ORDER BY due_at
"""

def load_config() -> dict:
    return {
        "database": os.environ["DB_NAME"],
        "host": os.environ["DB_HOST"],
        "port": int(os.environ["DB_PORT"]),
        "user": os.environ["DB_USER"],
        "password": os.environ["DB_PASSWORD"]
    }

def fetch_open_incidents(config: dict) -> list:
    conn = pg8000.dbapi.connect(**config)
    try:
        cursor = conn.cursor()
        cursor.execute(OPEN_INCIDENTS_SQL)
        results = cursor.fetchall()
        return results
    finally:
        conn.close()
    

if __name__ == '__main__':
    from dotenv import load_dotenv
    load_dotenv()

    rows = fetch_open_incidents(load_config())
    for row in rows:
        print(row)
    print(len(rows))