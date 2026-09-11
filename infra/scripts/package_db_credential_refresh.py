"""Build the Lambda ZIP with the Python standard library, before Terraform plan."""

from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


def package():
    root = Path(__file__).resolve().parents[1]
    source = root / "scripts/db_credential_refresh.py"
    destination = root / "envs/prod/build/db-credential-refresh.zip"
    destination.parent.mkdir(parents=True, exist_ok=True)
    entry = ZipInfo(source.name, date_time=(2020, 1, 1, 0, 0, 0))
    entry.compress_type = ZIP_DEFLATED
    entry.external_attr = 0o644 << 16
    with ZipFile(destination, "w") as archive:
        archive.writestr(entry, source.read_bytes())
    return destination


if __name__ == "__main__":
    print(package())
