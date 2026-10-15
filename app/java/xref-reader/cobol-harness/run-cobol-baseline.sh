#!/usr/bin/env bash
# Compile and run the original CBACT03C against a temporary indexed XREFFILE.
# Usage: run-cobol-baseline.sh [CARDXREF]
# Output: $OUT_DIR/readxref-sysout.txt (DISPLAY output followed by RETURN-CODE n).
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/../../.." && pwd)"
DATA="$APP/data/ASCII"
CARDXREF="${1:-$DATA/cardxref.txt}"
if [[ -e "$CARDXREF" ]]; then
  CARDXREF="$(realpath "$CARDXREF")"
else
  CARDXREF="$(realpath -m "$CARDXREF")"
fi
OUT_DIR="$(realpath -m "${OUT_DIR:-$HERE/out}")"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
COBC_FLAGS=(-x -fsign=EBCDIC -I "$APP/cpy")

echo "== compiling (GnuCOBOL $(cobc --version | head -1 | awk '{print $3}'))"
cobc "${COBC_FLAGS[@]}" -o "$WORK/xrefload" "$HERE/XREFLOAD.cbl"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cbact03c" "$APP/cbl/CBACT03C.cbl"

mkdir -p "$OUT_DIR"
cd "$WORK"
if [[ -e "$CARDXREF" ]]; then
  echo "== loading $(basename "$CARDXREF") into indexed XREFFILE"
  INFILE="$CARDXREF" XREFFILE="$WORK/xreffile.ksds" "$WORK/xrefload"
else
  echo "== input missing; running CBACT03C with a nonexistent XREFFILE"
fi

echo "== CBACT03C"
rc=0
XREFFILE="$WORK/xreffile.ksds" "$WORK/cbact03c" > "$OUT_DIR/readxref-sysout.txt" || rc=$?
echo "RETURN-CODE $rc" >> "$OUT_DIR/readxref-sysout.txt"
echo "== done: RC $rc, sysout in $OUT_DIR/readxref-sysout.txt"
