#!/usr/bin/env bash
# Compiles and runs the ORIGINAL POSTTRAN step (CBTRN02C) with GnuCOBOL against the
# ASCII sample data in app/data/ASCII and dumps every file it writes or updates.
#
# Usage: run-cobol-baseline.sh [DALYTRAN] [CARDXREF]
#   DALYTRAN defaults to app/data/ASCII/dailytran.txt, CARDXREF to app/data/ASCII/cardxref.txt.
# Outputs land in $OUT_DIR (default: ./out) as line-sequential text:
#   posttran-acctdata.txt  account file after posting       (ACCTFILE)
#   posttran-tcatbal.txt   category balances after posting  (TCATBALF)
#   posttran-transact.txt  posted transactions, key order   (TRANFILE)
#   posttran-dalyrejs.txt  rejected transactions + trailer  (DALYREJS)
#   posttran-sysout.txt    DISPLAY output, then "RETURN-CODE n"
# Requires: cobc (GnuCOBOL 3.x with an indexed-file handler, e.g. BDB).
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/../../.." && pwd)"
DATA="$APP/data/ASCII"
LOADER_SRC="$APP/java/interest-calculator/cobol-harness/CDLOADER.cbl"
DALYTRAN_IN="$(realpath "${1:-$DATA/dailytran.txt}")"
CARDXREF_IN="$(realpath "${2:-$DATA/cardxref.txt}")"
OUT_DIR="$(realpath -m "${OUT_DIR:-$HERE/out}")"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# CardDemo sample data uses EBCDIC-style overpunch signs ('{', 'A'..'I', '}', 'J'..'R').
COBC_FLAGS=(-x -fsign=EBCDIC -I "$APP/cpy")

echo "== compiling (GnuCOBOL $(cobc --version | head -1 | awk '{print $3}'))"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cdloader" "$LOADER_SRC"
cobc "${COBC_FLAGS[@]}" -o "$WORK/posttran" "$APP/cbl/CBTRN02C.cbl"

cd "$WORK"
load() { INFILE="$2" "$WORK/cdloader" "$1"; }

echo "== loading sample data into indexed files"
export ACCTFILE="$WORK/acct.ksds" TCATBALF="$WORK/tcat.ksds" XREFFILE="$WORK/xref.ksds" TRANFILE="$WORK/tran.ksds"
load LOADACCT "$DATA/acctdata.txt"
load LOADTCAT "$DATA/tcatbal.txt"
load LOADXREF "$CARDXREF_IN"

echo "== POSTTRAN (CBTRN02C) on $(basename "$DALYTRAN_IN")"
mkdir -p "$OUT_DIR"
# DALYTRAN and DALYREJS are fixed-length sequential files (LRECL 350 / 430) with no line breaks.
tr -d '\r\n' < "$DALYTRAN_IN" > "$WORK/dalytran.dat"
rc=0
DALYTRAN="$WORK/dalytran.dat" DALYREJS="$WORK/dalyrejs.dat" \
  "$WORK/posttran" > "$OUT_DIR/posttran-sysout.txt" || rc=$?
echo "RETURN-CODE $rc" >> "$OUT_DIR/posttran-sysout.txt"
grep -E "PROCESSED|REJECTED|RETURN-CODE" "$OUT_DIR/posttran-sysout.txt"

OUTFILE="$OUT_DIR/posttran-acctdata.txt" "$WORK/cdloader" DUMPACCT
OUTFILE="$OUT_DIR/posttran-tcatbal.txt" "$WORK/cdloader" DUMPTCAT
OUTFILE="$OUT_DIR/posttran-transact.txt" "$WORK/cdloader" DUMPTRAN
if [[ -s "$WORK/dalyrejs.dat" ]]; then
  fold -b -w 430 "$WORK/dalyrejs.dat" | awk '{ sub(/ +$/, ""); print }' > "$OUT_DIR/posttran-dalyrejs.txt"
else
  : > "$OUT_DIR/posttran-dalyrejs.txt"
fi
echo "== done: outputs in $OUT_DIR"
