#!/usr/bin/env bash
# Stage the exact official dual-ABI FFmpeg decoder. The AAR contains both the
# Java renderer and its matching JNI, including the native downmix controls.
set -euo pipefail
dest=${1:?output AAR required}
ref=dcfa36170127bfa1026a773ed6e3802fd5e7357d
digest=cbc9ed9c655a7cf64493de0e6e3ee28cd4b3972de258ae06c61c98db194f9f95
cache_dir="${NUVIO_ANDROID_CACHE:-$HOME/.cache/nuvio-android}/media3-ffmpeg"
mkdir -p "$cache_dir" "$(dirname "$dest")"
cached="$cache_dir/official-$digest.aar"
verified() {
  python3 - "$1" "$digest" <<'PY'
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1])
sys.exit(0 if p.is_file() and hashlib.sha256(p.read_bytes()).hexdigest() == sys.argv[2] else 1)
PY
}
if ! verified "$cached"; then
  tmp=$(mktemp "$cache_dir/official.XXXXXXXX")
  trap 'rm -f "$tmp"' EXIT
  curl -fsSL --retry 3 "https://raw.githubusercontent.com/NuvioMedia/NuvioTV/$ref/app/libs/lib-decoder-ffmpeg-release.aar" -o "$tmp"
  verified "$tmp" || { echo 'Official FFmpeg AAR checksum mismatch' >&2; exit 1; }
  mv "$tmp" "$cached"
fi
if ! verified "$dest"; then
  tmp=$(mktemp "${dest}.XXXXXXXX")
  trap 'rm -f "$tmp"' EXIT
  cp "$cached" "$tmp"
  mv "$tmp" "$dest"
fi
