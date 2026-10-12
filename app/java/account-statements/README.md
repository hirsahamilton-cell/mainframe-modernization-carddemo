# CardDemo account statements (Java port of CREASTMT / CBSTM03A + CBSTM03B)

Java 17 port of the `CREASTMT` batch job (`app/jcl/CREASTMT.JCL`): the SORT step that orders the transaction
master by card number and transaction id, and `app/cbl/CBSTM03A.CBL` (with its file-access subroutine
`app/cbl/CBSTM03B.CBL`), which writes one text statement (80-byte records, `STMTFILE`) and one HTML statement
(100-byte records, `HTMLFILE`) for every card in the cross-reference.

| COBOL | Java |
| --- | --- |
| STEP010 SORT (`SORT FIELDS=(263,16,CH,A,1,16,CH,A)`, `OUTREC`) and STEP020 REPRO | `StatementJob.statements` |
| `8500-READTRNX-READ` (in-memory card / transaction table), `4000-TRNXFILE-GET` | `StatementJob.statements` |
| `1000-MAINLINE`, `1000-XREFFILE-GET-NEXT`, `2000-CUSTFILE-GET`, `3000-ACCTFILE-GET` | `StatementJob.run` |
| `5000-CREATE-STATEMENT`, `6000-WRITE-TRANS` (text), `STATEMENT-LINES` | `StatementText` |
| `5100-WRITE-HTML-HEADER`, `5200-WRITE-HTML-NMADBS`, `6000-WRITE-TRANS` (HTML), `HTML-LINES` | `StatementHtml` |
| `9999-ABEND-PROGRAM` | `StatementException` (exit code 12, no output written) |
| CBSTM03B (open / sequential read / keyed read / close) | `io.CardDemoRecords` |
| copybooks COSTM01 / CVTRA05Y, CVACT03Y, CUSTREC, CVACT01Y | `model.*`, `io.CardDemoRecords`, `io.ZonedDecimal` |

The PSA / TCB / TIOT walk at the start of CBSTM03A only DISPLAYs the job's DD names on z/OS and is not ported.

## Fixed: transactions beyond the 10th on a card

CBSTM03A loads every transaction into `WS-TRNX-TABLE`, which holds 51 cards of 10 transactions each, with no
bounds check. An 11th transaction on a card overwrites the next card's entry, and the statement then prints
that memory as transactions (the next card's number as a transaction id, garbled descriptions, zero amounts)
and leaves the real ones out of the total. The Java job has no limit on cards or transactions per card.

## Fixed: names and city cut at the first space

The name and city lines are built with `STRING ... DELIMITED BY ' '`, so each part stops at its first space:
"New Aricchester" prints as "New", a last name "Van Der Berg" as "Van". A blank middle name leaves two spaces
("Mary  Jones"), and the HTML copy, which stops at the first two spaces, then prints only "Mary". On the
shipped data 24 of the 50 city lines are cut. The Java statements print every part in full, separated by
single spaces, and leave out blank parts.

## Fixed: unescaped HTML

CBSTM03A copies names, addresses and transaction descriptions into the HTML as they are, so `<`, `>` and `&`
break the page (e.g. "Return at &lt;Outlet&gt;" becomes a tag). The Java HTML escapes them. A value whose escaped
line would be longer than 100 bytes loses characters from its end.

## Fixed: empty transaction file

CBSTM03A reads the first transaction before the main loop and abends if there is none (`ERROR READING
TRNXFILE`, `RETURN CODE: 10`). The Java job writes every statement with a zero total.

## Fixed: amounts too large for the statement

`ACCT-CURR-BAL` (`S9(10)V99`) is printed as `9(9).99-`, and the card total is summed in `S9(9)V99`, so a
balance or total of 1,000,000,000.00 or more silently loses its leading digit. The Java job stops instead
(`... DOES NOT FIT ON THE STATEMENT`, exit code 12). No shipped data comes close.

## Behaviour kept from COBOL

- One statement per cross-reference card, in card-number order; transactions in transaction-id order.
  Transactions on cards that are not in the cross-reference are not printed.
- Cards without transactions get a statement with a `$         .00` total; two cards on one account get one
  statement each.
- Transaction descriptions are cut to 49 characters; amounts print as `Z(9).99-`, the balance as `9(9).99-`.
- A customer or account that is not on file stops the run (`ERROR READING CUSTFILE` / `ACCTFILE`,
  `RETURN CODE: 23`, `ABENDING PROGRAM`, exit code 12).
- Every statement is a complete HTML document, written one after another into the same file.

## Build and test

```sh
mvn test        # unit tests + comparison against the GnuCOBOL baseline
mvn package
java -jar target/account-statements-1.0.0-SNAPSHOT.jar \
  TRANSACT XREF ACCTDATA CUSTDATA OUT-STMTFILE OUT-HTMLFILE
```

Input and output files are the ASCII, one-record-per-line versions of the datasets (as in `app/data/ASCII`);
`TRANSACT` is the transaction master (e.g. the transaction posting output) in any order. Output records are
written at their full width (80 and 100 bytes).

## COBOL baseline

`cobol-harness/run-cobol-baseline.sh TRANSACT [ACCTDATA] [CUSTDATA] [CARDXREF]` compiles the original
`CBSTM03A` and `CBSTM03B` with GnuCOBOL 3.x, emulates the SORT / OUTREC step with `awk` and `sort`, loads the
transactions, cross-reference, customers and accounts into indexed files with
`../interest-calculator/cobol-harness/CDLOADER.cbl` (`LOADTRXF`, `LOADXREF`, `LOADCUST`, `LOADACCT`), and
writes the statements and DISPLAY output to `cobol-harness/out/`. Two changes are needed to compile: the TIOT
walk is removed from a copy of CBSTM03A, and `app/cpy/CUSTREC.cpy`, which is indented with tabs, is compiled
from a copy with each tab replaced by a space. The checked-in copies in `src/test/resources/cobol-baseline/`
come from two runs:

- `sample/`: `scenarios/sample-transact.txt` (the transaction master POSTTRAN wrote for the shipped daily
  transactions, 262 transactions on 50 cards) and `scenarios/sample-acctdata.txt` (the accounts POSTTRAN
  updated), with the shipped `cardxref.txt` and `custdata.txt`.
- `edge/`: hand-made `scenarios/edge-*.txt`: a card with 12 transactions, zero / 5-cent / largest / negative
  amounts, a negative balance, blank middle names, multi-word names, HTML special characters, a
  100-character description, two cards on one account, a card without transactions and a transaction on a
  card that is not in the cross-reference.

```sh
S=src/test/resources/scenarios D=../../data/ASCII
OUT_DIR=src/test/resources/cobol-baseline/sample cobol-harness/run-cobol-baseline.sh \
  $S/sample-transact.txt $S/sample-acctdata.txt $D/custdata.txt $D/cardxref.txt
OUT_DIR=src/test/resources/cobol-baseline/edge cobol-harness/run-cobol-baseline.sh \
  $S/edge-transact.txt $S/edge-acctdata.txt $S/edge-custdata.txt $S/edge-cardxref.txt
```

`CobolComparisonTest` runs the Java job per scenario and compares every text and HTML line with COBOL after
undoing exactly the fixes above: the name and city lines are mapped to the cut COBOL versions, HTML escaping
is undone, and on the card with 12 transactions the 11th and 12th transactions and the total are checked to
differ (the COBOL total is the sum of the first 10).

Abend cases can't be compared under GnuCOBOL: `CEE3ABD` is an IBM Language Environment routine, so the
original program prints its error message and then fails with `module 'CEE3ABD' not found`.
