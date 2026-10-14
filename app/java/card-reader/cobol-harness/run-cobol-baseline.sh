#!/usr/bin/env bash
# Compile and run the original CBACT02C against a temporary indexed CARDFILE.
# Usage: run-cobol-baseline.sh [CARDDATA]
# Output: $OUT_DIR/readcard-sysout.txt (DISPLAY output followed by RETURN-CODE n).
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/../../.." && pwd)"
DATA="$APP/data/ASCII"
CARDDATA="${1:-$DATA/carddata.txt}"
if [[ -e "$CARDDATA" ]]; then
  CARDDATA="$(realpath "$CARDDATA")"
else
  CARDDATA="$(realpath -m "$CARDDATA")"
fi
OUT_DIR="$(realpath -m "${OUT_DIR:-$HERE/out}")"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
COBC_FLAGS=(-x -fsign=EBCDIC -I "$APP/cpy")

echo "== compiling (GnuCOBOL $(cobc --version | head -1 | awk '{print $3}'))"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cardload" "$HERE/CARDLOAD.cbl"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cbact02c" "$APP/cbl/CBACT02C.cbl"

mkdir -p "$OUT_DIR"
cd "$WORK"
if [[ -e "$CARDDATA" ]]; then
  echo "== loading $(basename "$CARDDATA") into indexed CARDFILE"
  INFILE="$CARDDATA" CARDFILE="$WORK/cardfile.ksds" "$WORK/cardload"
else
  echo "== input missing; running CBACT02C with a nonexistent CARDFILE"
fi

echo "== CBACT02C"
rc=0
CARDFILE="$WORK/cardfile.ksds" "$WORK/cbact02c" > "$OUT_DIR/readcard-sysout.txt" || rc=$?
echo "RETURN-CODE $rc" >> "$OUT_DIR/readcard-sysout.txt"
echo "== done: RC $rc, sysout in $OUT_DIR/readcard-sysout.txt"
