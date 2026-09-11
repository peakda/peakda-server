#!/usr/bin/env bash
set -euo pipefail

infra_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 "$infra_dir/scripts/package_db_credential_refresh.py"
exec terraform -chdir="$infra_dir/envs/prod" "$@"
