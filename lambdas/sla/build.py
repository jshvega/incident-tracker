import shutil
import subprocess
import sys
import zipfile
from pathlib import Path




SLA_DIR = Path(__file__).resolve().parent
BUILD_DIR = SLA_DIR / "build"
PACKAGE_DIR = BUILD_DIR / "package"
ZIP_PATH = BUILD_DIR / "sla-sweep.zip"
FILES_TO_COPY = ["sweep.py", "prod-ca-2021.crt"]




def clean() -> None:
    shutil.rmtree(BUILD_DIR, ignore_errors=True)

def install_deps() -> None:
    subprocess.run([
        sys.executable, "-m", "pip", "install",
        "-r", str(SLA_DIR / "requirements.txt"),
        "--target", str(PACKAGE_DIR),
        "--platform", "manylinux2014_aarch64",
        "--implementation", "cp",
        "--python-version", "3.12",
        "--only-binary=:all:",
        "--no-compile",
    ], check=True)

def copy_source() -> None:
    for name in FILES_TO_COPY:
        shutil.copy2(SLA_DIR / name, PACKAGE_DIR)

def write_zip() -> None:
    with zipfile.ZipFile(ZIP_PATH, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for path in PACKAGE_DIR.rglob("*"):
            if not path.is_file():
                continue
            if "__pycache__" in path.parts:
                continue

            arcname = path.relative_to(PACKAGE_DIR).as_posix()
            archive.write(path, arcname=arcname)




if __name__ == "__main__":
    clean()
    install_deps()
    copy_source()
    write_zip()
    print(f"built {ZIP_PATH}")