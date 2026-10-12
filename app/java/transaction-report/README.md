# CardDemo transaction report (Java port of TRANREPT / CBTRN03C)

Java 17 port of the `TRANREPT` batch job (`app/jcl/TRANREPT.jcl`): the SORT step that keeps the
transactions processed in a date range and orders them by card number, and `app/cbl/CBTRN03C.cbl`, which
prints the 133-byte daily transaction report with account, page and grand totals. Its input is the
transaction master written by transaction posting (`../transaction-posting`).

| COBOL | Java |
| --- | --- |
| STEP05R SORT (`INCLUDE` processed date, `SORT FIELDS` card number) | `TransactionReportJob.select` |
| main loop, `1100-WRITE-TRANSACTION-REPORT` | `TransactionReportJob.run` |
| `1110-WRITE-PAGE-TOTALS`, `1120-WRITE-ACCOUNT-TOTALS`, `1110-WRITE-GRAND-TOTALS` | `TransactionReportJob.Report` |
| `1120-WRITE-HEADERS`, `1120-WRITE-DETAIL`, copybook CVTRA07Y | `ReportLines`, `io.EditedAmount` |
| `MOD(WS-LINE-COUNTER, WS-PAGE-SIZE) = 0` | `PageBreakRule` |
| `1500-A/B/C-LOOKUP-*`, `9999-ABEND-PROGRAM` | `TransactionReportException` (exit code 12, no report written) |
| copybooks CVTRA05Y, CVACT03Y, CVTRA03Y, CVTRA04Y, DATEPARM | `model.*`, `io.CardDemoRecords`, `io.ZonedDecimal` |

## Fixed: the last card's totals

At end of file CBTRN03C adds `TRAN-AMT`, still holding the last transaction, to the page total a second
time and never writes the last card's account total. On the shipped data the COBOL grand total is
78,720.36, which is 765.66 (the last transaction) more than the transactions add up to. The Java report
writes the last account total and counts every transaction once (grand total 77,954.70).

## Fixed: skipped page breaks

CBTRN03C starts a new page only when the line counter is exactly a multiple of 20 before a detail line. An
account total adds two lines, so whenever it moves the counter from 19 to 21 (or 39 to 41, ...) the break
is skipped and the page runs on to the next multiple: on the shipped data COBOL prints 15 pages, 5 of them 40 or 60
lines long. The Java report breaks at the first detail line after each multiple of 20 (26 pages, none longer
than 23 lines). `PageBreakRule` holds both rules; the COBOL one is used only by the tests.

## Behaviour kept from COBOL

- Zero amounts print as blanks (every digit position of `ZZZ,ZZZ,ZZZ.ZZ` is suppressed).
- No transaction in the range (or an empty `DATEPARM`) gives an empty report, with no headers or totals.
- A card, transaction type or category that is not on file stops the run (`INVALID ... : key`,
  `ABENDING PROGRAM`, exit code 12); transactions outside the date range are not looked up.

## Build and test

```sh
mvn test        # unit tests + comparison against the GnuCOBOL baseline
mvn package
printf '%-80s\n' "2026-10-11 2026-10-11" > dateparm.txt
java -jar target/transaction-report-1.0.0-SNAPSHOT.jar \
  TRANSACT XREF TRANTYPE TRANCATG dateparm.txt OUT-TRANREPT
```

Input and output files are the ASCII, one-record-per-line versions of the datasets (as in `app/data/ASCII`);
`TRANSACT` can be in any order. The `DATEPARM` record is `start-date end-date` (YYYY-MM-DD).

## COBOL baseline

`cobol-harness/run-cobol-baseline.sh TRANSACT START-DATE END-DATE [CARDXREF]` compiles the original
`CBTRN03C` with GnuCOBOL 3.x, loads the cross-reference, transaction types and categories into indexed files
with `../interest-calculator/cobol-harness/CDLOADER.cbl` (`LOADXREF`, `LOADTTYP`, `LOADTCTG`), emulates the
SORT step with `awk` and `sort`, and writes the report and DISPLAY output to `cobol-harness/out/`. The
checked-in copies in `src/test/resources/cobol-baseline/` come from two runs:

- `sample/`: `scenarios/sample-transact.txt`, the transaction master that COBOL POSTTRAN wrote for the shipped
  daily transactions (262 transactions, all processed 2026-10-11).
- `edge/`: `scenarios/edge-transact.txt` (42 hand-made transactions in January 2026): range boundaries and
  out-of-range dates, unsorted cards, zero / 5-cent / million-dollar / negative amounts, every transaction
  type, the interest and over-limit fee categories, a card that nets to zero and a skipped page break.

```sh
OUT_DIR=src/test/resources/cobol-baseline/sample cobol-harness/run-cobol-baseline.sh \
  src/test/resources/scenarios/sample-transact.txt 2026-10-11 2026-10-11
OUT_DIR=src/test/resources/cobol-baseline/edge cobol-harness/run-cobol-baseline.sh \
  src/test/resources/scenarios/edge-transact.txt 2026-01-01 2026-01-31
```

`CobolComparisonTest` runs the Java job per scenario. With the COBOL page-break rule plugged in (test code
only), the report matches COBOL line for line except the end: the extra last account total and the final page
and grand totals without the double-counted transaction. With the fixed rule only page totals and headers
move; the detail and account-total lines are unchanged.

Abend cases can't be compared under GnuCOBOL: `CEE3ABD` is an IBM Language Environment routine, so the
original program prints its `INVALID ...` message and then fails with `module 'CEE3ABD' not found`.
