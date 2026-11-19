import zipfile
from pathlib import Path

NOTIFY_DIR = Path(__file__).resolve().parent
BUILD_DIR = NOTIFY_DIR / "build"
ZIP_PATH = BUILD_DIR / "notify.zip"

if __name__ == "__main__":
    BUILD_DIR.mkdir(exist_ok=True)
    with zipfile.ZipFile(ZIP_PATH, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(NOTIFY_DIR / "notify.py", arcname="notify.py")
    print(f"built {ZIP_PATH}")