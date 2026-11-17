#!/bin/bash
# The old season/episode carousel has been removed. Validate the replacement,
# including ascending seasons, watch progress, source routing, and spoof guards.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tests/detail_netflix.py
