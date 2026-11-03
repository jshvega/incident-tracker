from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).with_name(".env.prod"))

from sweep import get_conn

cursor = get_conn("preflight").cursor()
cursor.execute("SELECT 1, current_user")
print(cursor.fetchone())