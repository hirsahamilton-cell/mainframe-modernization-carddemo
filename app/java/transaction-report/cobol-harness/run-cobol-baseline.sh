#!/usr/bin/env bash
# Compiles and runs the ORIGINAL TRANREPT job (SORT step + CBTRN03C) with GnuCOBOL and writes the report.
#
# Usage: run-cobol-baseline.sh TRANSACT START-DATE END-DATE [CARDXREF]
#   TRANSACT is a posted transaction file (one 350-byte record per line, e.g. POSTTRAN output).
#   START-DATE / END-DATE (YYYY-MM-DD) go to both the SORT INCLUDE and the DATEPARM record, as in TRANREPT.jcl.
#   CARDXREF defaults to app/data/ASCII/cardxref.txt; TRANTYPE / TRANCATG come from app/data/ASCII.
#   SORT_FILTER=0 skips the SORT INCLUDE (records are still sorted), so CBTRN03C sees out-of-range records.
# Outputs land in $OUT_DIR (default: ./out):
#   tranrept-report.txt   the 133-byte report lines, trailing spaces removed  (TRANREPT)
#   tranrept-sysout.txt   DISPLAY output, then "RETURN-CODE n"
# Requires: cobc (GnuCOBOL 3.x with an indexed-file handler, e.g. BDB).
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "usage: $0 TRANSACT START-DATE END-DATE [CARDXREF]" >&2
  exit 8
fi
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/../../.." && pwd)"
DATA="$APP/data/ASCII"
LOADER_SRC="$APP/java/interest-calculator/cobol-harness/CDLOADER.cbl"
TRANSACT_IN="$(realpath "$1")"
START_DATE="$2"
END_DATE="$3"
CARDXREF_IN="$(realpath "${4:-$DATA/cardxref.txt}")"
OUT_DIR="$(realpath -m "${OUT_DIR:-$HERE/out}")"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# CardDemo sample data uses EBCDIC-style overpunch signs ('{', 'A'..'I', '}', 'J'..'R').
COBC_FLAGS=(-x -fsign=EBCDIC -I "$APP/cpy")

echo "== compiling (GnuCOBOL $(cobc --version | head -1 | awk '{print $3}'))"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cdloader" "$LOADER_SRC"
cobc "${COBC_FLAGS[@]}" -o "$WORK/tranrept" "$APP/cbl/CBTRN03C.cbl"

cd "$WORK"
load() { INFILE="$2" "$WORK/cdloader" "$1"; }

echo "== loading reference data into indexed files"
export CARDXREF="$WORK/xref.ksds" TRANTYPE="$WORK/ttyp.ksds" TRANCATG="$WORK/tctg.ksds"
XREFFILE="$CARDXREF" load LOADXREF "$CARDXREF_IN"
load LOADTTYP "$DATA/trantype.txt"
load LOADTCTG "$DATA/trancatg.txt"

# STEP05R SORT: INCLUDE TRAN-PROC-DT (cols 305-314) in [START, END], SORT FIELDS=(TRAN-CARD-NUM,A) (cols 263-278).
# Equal card numbers keep their input (transaction id) order.
echo "== SORT $(basename "$TRANSACT_IN") by card number, processed date $START_DATE..$END_DATE"
tr -d '\r' < "$TRANSACT_IN" | awk 'NF' \
  | awk -v s="$START_DATE" -v e="$END_DATE" -v f="${SORT_FILTER:-1}" \
      '{ d = substr($0, 305, 10); if (f == "0" || (d >= s && d <= e)) printf "%-350s\n", $0 }' \
  | LC_ALL=C sort -s -k1.263,1.278 | tr -d '\n' > "$WORK/tranfile.dat"
printf '%-80s' "$START_DATE $END_DATE" > "$WORK/dateparm.dat"

echo "== TRANREPT (CBTRN03C)"
mkdir -p "$OUT_DIR"
rc=0
TRANFILE="$WORK/tranfile.dat" DATEPARM="$WORK/dateparm.dat" TRANREPT="$WORK/tranrept.dat" \
  "$WORK/tranrept" > "$OUT_DIR/tranrept-sysout.txt" 2>&1 || rc=$?
echo "RETURN-CODE $rc" >> "$OUT_DIR/tranrept-sysout.txt"
if [[ -s "$WORK/tranrept.dat" ]]; then
  fold -b -w 133 "$WORK/tranrept.dat" | awk '{ sub(/ +$/, ""); print }' > "$OUT_DIR/tranrept-report.txt"
else
  : > "$OUT_DIR/tranrept-report.txt"
fi
echo "== done: RC $rc, $(wc -l < "$OUT_DIR/tranrept-report.txt") report lines in $OUT_DIR"
