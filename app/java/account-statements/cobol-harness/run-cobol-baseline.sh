#!/usr/bin/env bash
# Compiles and runs the ORIGINAL CREASTMT job (SORT step + CBSTM03A / CBSTM03B) with GnuCOBOL and writes the
# text and HTML statements.
#
# Usage: run-cobol-baseline.sh TRANSACT [ACCTDATA] [CUSTDATA] [CARDXREF]
#   TRANSACT is a posted transaction file (one 350-byte record per line, e.g. POSTTRAN output).
#   ACCTDATA, CUSTDATA and CARDXREF default to acctdata.txt, custdata.txt and cardxref.txt in app/data/ASCII.
# Outputs land in $OUT_DIR (default: ./out):
#   creastmt-statement.txt  the 80-byte statement lines, trailing spaces removed   (STMTFILE)
#   creastmt-html.txt       the 100-byte HTML lines, trailing spaces removed       (HTMLFILE)
#   creastmt-sysout.txt     DISPLAY output, then "RETURN-CODE n"
# app/cpy/CUSTREC.cpy is indented with tab characters, which push its PIC clauses past column 72 under
# GnuCOBOL's tab expansion; the harness compiles against a copy with each tab replaced by one space.
# CBSTM03A starts by walking the z/OS TIOT control blocks (PSA -> TCB -> TIOT) to DISPLAY the job's DD names.
# Those addresses don't exist outside z/OS, so the harness compiles a copy with those lines removed; nothing
# else in the program is changed.
# Requires: cobc (GnuCOBOL 3.x with an indexed-file handler, e.g. BDB).
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 TRANSACT [ACCTDATA] [CUSTDATA] [CARDXREF]" >&2
  exit 8
fi
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/../../.." && pwd)"
DATA="$APP/data/ASCII"
LOADER_SRC="$APP/java/interest-calculator/cobol-harness/CDLOADER.cbl"
TRANSACT_IN="$(realpath "$1")"
ACCTDATA_IN="$(realpath "${2:-$DATA/acctdata.txt}")"
CUSTDATA_IN="$(realpath "${3:-$DATA/custdata.txt}")"
CARDXREF_IN="$(realpath "${4:-$DATA/cardxref.txt}")"
OUT_DIR="$(realpath -m "${OUT_DIR:-$HERE/out}")"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

mkdir "$WORK/cpy"
sed 's/\t/ /g' "$APP/cpy/CUSTREC.cpy" > "$WORK/cpy/CUSTREC.cpy"

# CardDemo sample data uses EBCDIC-style overpunch signs ('{', 'A'..'I', '}', 'J'..'R').
COBC_FLAGS=(-x -fsign=EBCDIC -I "$WORK/cpy" -I "$APP/cpy")

# Drop the TIOT walk: from the first SET ADDRESS OF PSA-BLOCK up to (not including) OPEN OUTPUT STMT-FILE.
awk '/SET ADDRESS OF PSA-BLOCK/ { skip = 1 } /OPEN OUTPUT STMT-FILE/ { skip = 0 } !skip' \
  "$APP/cbl/CBSTM03A.CBL" > "$WORK/CBSTM03A.CBL"

echo "== compiling (GnuCOBOL $(cobc --version | head -1 | awk '{print $3}'))"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cdloader" "$LOADER_SRC"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cbstm03a" "$WORK/CBSTM03A.CBL" "$APP/cbl/CBSTM03B.CBL"

cd "$WORK"
load() { INFILE="$2" "$WORK/cdloader" "$1"; }

# STEP010 SORT: SORT FIELDS=(263,16,CH,A,1,16,CH,A) (card number, transaction id),
# OUTREC FIELDS=(1:263,16,17:1,262,279:279,50), then STEP020 REPRO into the TRXFL KSDS (key 32 bytes).
echo "== SORT $(basename "$TRANSACT_IN") by card number and transaction id"
tr -d '\r' < "$TRANSACT_IN" | awk 'NF' \
  | awk '{ r = sprintf("%-350s", $0); printf "%-350s\n", substr(r, 263, 16) substr(r, 1, 262) substr(r, 279, 50) }' \
  | LC_ALL=C sort -k1.1,1.32 > "$WORK/trxfl.txt"

echo "== loading indexed files"
export TRNXFILE="$WORK/trxfl.ksds" XREFFILE="$WORK/xref.ksds" ACCTFILE="$WORK/acct.ksds" CUSTFILE="$WORK/cust.ksds"
load LOADTRXF "$WORK/trxfl.txt"
load LOADXREF "$CARDXREF_IN"
load LOADACCT "$ACCTDATA_IN"
load LOADCUST "$CUSTDATA_IN"

echo "== CREASTMT (CBSTM03A + CBSTM03B)"
mkdir -p "$OUT_DIR"
rc=0
STMTFILE="$WORK/stmt.dat" HTMLFILE="$WORK/html.dat" \
  "$WORK/cbstm03a" > "$OUT_DIR/creastmt-sysout.txt" 2>&1 || rc=$?
echo "RETURN-CODE $rc" >> "$OUT_DIR/creastmt-sysout.txt"
# STMTFILE (LRECL 80) and HTMLFILE (LRECL 100) are fixed-length sequential files with no line breaks.
unload() {
  if [[ -s "$1" ]]; then
    fold -b -w "$2" "$1" | awk '{ sub(/ +$/, ""); print }' > "$3"
  else
    : > "$3"
  fi
}
unload "$WORK/stmt.dat" 80 "$OUT_DIR/creastmt-statement.txt"
unload "$WORK/html.dat" 100 "$OUT_DIR/creastmt-html.txt"
echo "== done: RC $rc, $(wc -l < "$OUT_DIR/creastmt-statement.txt") statement lines," \
  "$(wc -l < "$OUT_DIR/creastmt-html.txt") HTML lines in $OUT_DIR"
