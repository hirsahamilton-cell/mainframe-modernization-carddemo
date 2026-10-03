#!/usr/bin/env bash
# Compiles and runs the ORIGINAL COBOL job chain with GnuCOBOL against the
# ASCII sample data in app/data/ASCII:
#   1. POSTTRAN (CBTRN02C) posts dailytran.txt into TCATBAL + ACCTDATA,
#      giving non-zero category balances (the shipped tcatbal.txt is all zero).
#   2. Credit-limit scenario: no sample account is over its limit (POSTTRAN
#      rejects over-limit transactions), so two accounts are adjusted using
#      only their own field values:
#        00000000010  ACCT-CURR-BAL := ACCT-CREDIT-LIMIT (5401.00),
#                     ACCT-CREDIT-LIMIT := ACCT-CASH-CREDIT-LIMIT (4442.00) -> over
#        00000000020  ACCT-CURR-BAL := ACCT-CREDIT-LIMIT (3767.00)          -> at limit
#   3. INTCALC  (CBACT04C) computes interest from the posted balances.
# Outputs land in $OUT_DIR (default: ./out) as line-sequential text:
#   intcalc-input-acctdata.txt / intcalc-input-tcatbal.txt  (INTCALC inputs)
#   intcalc-transact.txt                          (CBACT04C SYSTRAN output)
#   intcalc-acctdata.txt                          (account file after CBACT04C)
#   intcalc-sysout.txt                            (CBACT04C DISPLAY output)
# Requires: cobc (GnuCOBOL 3.x with an indexed-file handler, e.g. BDB).
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/../../.." && pwd)"
DATA="$APP/data/ASCII"
PARM_DATE="${PARM_DATE:-2022071800}"
OUT_DIR="${OUT_DIR:-$HERE/out}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# CardDemo sample data uses EBCDIC-style overpunch signs ('{', 'A'..'I', '}', 'J'..'R').
COBC_FLAGS=(-x -fsign=EBCDIC -I "$APP/cpy")

echo "== compiling (GnuCOBOL $(cobc --version | head -1 | awk '{print $3}'))"
cobc "${COBC_FLAGS[@]}" -o "$WORK/cdloader" "$HERE/CDLOADER.cbl"
cobc "${COBC_FLAGS[@]}" -o "$WORK/posttran" "$APP/cbl/CBTRN02C.cbl"
cobc "${COBC_FLAGS[@]}" -o "$WORK/intcalc" "$HERE/RUNINTC.cbl" "$APP/cbl/CBACT04C.cbl"

cd "$WORK"
load() { INFILE="$2" "$WORK/cdloader" "$1"; }

echo "== loading sample data into indexed files"
export ACCTFILE="$WORK/acct.ksds" TCATBALF="$WORK/tcat.ksds" DISCGRP="$WORK/disc.ksds"
load LOADACCT "$DATA/acctdata.txt"
load LOADTCAT "$DATA/tcatbal.txt"
load LOADDISC "$DATA/discgrp.txt"
XREFFILE="$WORK/xref.ksds" load LOADXREF "$DATA/cardxref.txt"

echo "== POSTTRAN (CBTRN02C)"
tr -d '\r\n' < "$DATA/dailytran.txt" > "$WORK/dalytran.dat"
DALYTRAN="$WORK/dalytran.dat" TRANFILE="$WORK/posted.ksds" \
  XREFFILE="$WORK/xref.ksds" DALYREJS="$WORK/dalyrejs.dat" \
  "$WORK/posttran" > "$WORK/posttran-sysout.txt" || true
grep -E "PROCESSED|REJECTED" "$WORK/posttran-sysout.txt" || true

mkdir -p "$OUT_DIR"
OUTFILE="$WORK/posttran-acctdata.txt" "$WORK/cdloader" DUMPACCT
OUTFILE="$OUT_DIR/intcalc-input-tcatbal.txt" "$WORK/cdloader" DUMPTCAT

echo "== applying credit-limit scenario (accounts 10 over, 20 at limit)"
awk '{
  if (substr($0,1,11) == "00000000010")
    $0 = substr($0,1,12) substr($0,25,12) substr($0,37,12) substr($0,37)
  else if (substr($0,1,11) == "00000000020")
    $0 = substr($0,1,12) substr($0,25,12) substr($0,25)
  print
}' "$WORK/posttran-acctdata.txt" > "$OUT_DIR/intcalc-input-acctdata.txt"
rm -rf "$WORK/acct.ksds"*
load LOADACCT "$OUT_DIR/intcalc-input-acctdata.txt"

echo "== INTCALC (CBACT04C PARM='$PARM_DATE')"
XREFFILE="$WORK/xrefa.ksds" load LOADXRFA "$DATA/cardxref.txt"
XREFFILE="$WORK/xrefa.ksds" TRANSACT="$WORK/systran.dat" \
  "$WORK/intcalc" "$PARM_DATE" > "$OUT_DIR/intcalc-sysout.txt"
tail -1 "$OUT_DIR/intcalc-sysout.txt"
fold -b -w 350 "$WORK/systran.dat" > "$OUT_DIR/intcalc-transact.txt"
echo >> "$OUT_DIR/intcalc-transact.txt"
sed -i '/^$/d' "$OUT_DIR/intcalc-transact.txt"
OUTFILE="$OUT_DIR/intcalc-acctdata.txt" "$WORK/cdloader" DUMPACCT

echo "== done: $(wc -l < "$OUT_DIR/intcalc-transact.txt") interest transactions written to $OUT_DIR"
