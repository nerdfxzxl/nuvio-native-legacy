#!/bin/bash
# Netflix detail action stack replaces the former horizontal pill geometry.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tests/detail_netflix.py
