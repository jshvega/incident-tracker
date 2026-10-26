import importlib.metadata
import sys

if __name__ == '__main__':

    version_py = sys.version
    print(version_py)

    version_pg = importlib.metadata.version('pg8000')
    print(version_pg)