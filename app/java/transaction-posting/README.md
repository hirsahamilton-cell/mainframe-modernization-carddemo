# CardDemo transaction posting (Java port of CBTRN02C)

Java 17 port of the `POSTTRAN` batch step (`app/cbl/CBTRN02C.cbl`, `app/jcl/POSTTRAN.jcl`). It reads the
daily transaction file and, for each transaction, either posts it (to the category balance, the account and
the transaction master) or writes it to the rejects file with a reason code. Its category balances are the
input to the interest calculator (`../interest-calculator`).

| COBOL paragraph | Java |
| --- | --- |
| main loop, `2000-POST-TRANSACTION` | `TransactionPostingJob` |
| `1500-A-LOOKUP-XREF`, `1500-B-LOOKUP-ACCT` | `TransactionPostingJob.validate`, `TransactionValidator` |
| `2500-WRITE-REJECT-REC` | `RejectedTransaction`, `CardDemoRecords.formatRejectedTransaction` |
| `2700-UPDATE-TCATBAL` (create / update) | `TransactionPostingJob.updateCategoryBalance` |
| `2800-UPDATE-ACCOUNT-REC` | `Account.withPosted` |
| `Z-GET-DB2-FORMAT-TIMESTAMP` | `ProcessingTimestamp` |
| `9999-ABEND-PROGRAM` | `TransactionPostingException` (exit code 12, no output written) |
| copybooks CVACT01Y, CVACT03Y, CVTRA01Y, CVTRA05Y, CVTRA06Y | `model.*`, `io.CardDemoRecords`, `io.ZonedDecimal` |

Reject reasons: 100 invalid card number, 101 account not found, 102 over limit, 103 received after the
account expiration date. When a transaction is both over the limit and too late, 103 is reported (as in COBOL).

## Fixed: refunds counted against the credit limit

CBTRN02C rejects a transaction when `ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT` exceeds
`ACCT-CREDIT-LIMIT`. Refunds are negative amounts added to `ACCT-CURR-CYC-DEBIT`, so subtracting it adds the
refunds back: a refund made later purchases *more* likely to be rejected. The Java check
(`TransactionValidator.NET_CYCLE_CHARGES`) uses `credit + debit + amount`, the net amount charged this cycle.
On the shipped sample data this changes 19 of 300 outcomes (17 rejects become posts, 2 posts become rejects).
The comparison test lists them.

## Behaviour kept from COBOL

- `TRANFILE` is opened `OUTPUT`, so the transaction master holds only this run's postings.
- The check against the credit limit uses the cycle totals, not `ACCT-CURR-BAL`.
- Exit code 4 when anything was rejected.

## Build and test

```sh
mvn test        # unit tests + comparison against the GnuCOBOL baseline
mvn package
java -jar target/transaction-posting-1.0.0-SNAPSHOT.jar \
  DALYTRAN XREF ACCTDATA TCATBAL OUT-TRANSACT OUT-DALYREJS OUT-ACCTDATA OUT-TCATBAL
```

Input and output files are the ASCII, one-record-per-line versions of the datasets (as in `app/data/ASCII`).
After `mvn test`, `target/cobol-vs-java-{sample,edge}.txt` list every transaction whose outcome differs
from COBOL, with the affected account balances.

## COBOL baseline

`cobol-harness/run-cobol-baseline.sh [DALYTRAN] [CARDXREF]` compiles the original `CBTRN02C` with GnuCOBOL 3.x
(it reuses `../interest-calculator/cobol-harness/CDLOADER.cbl` to load and dump indexed files), runs it and
writes every output file to `cobol-harness/out/`. The checked-in copies in `src/test/resources/cobol-baseline/`
come from two runs:

- `sample/`: the shipped `app/data/ASCII` files.
- `edge/`: `src/test/resources/scenarios/edge-dailytran.txt` (12 hand-made transactions covering every
  reject reason, the expiry-date boundary, the credit-limit boundary, a refund and a new category balance)
  with `edge-cardxref.txt` (the shipped cross-reference plus a card whose account does not exist).

```sh
OUT_DIR=src/test/resources/cobol-baseline/sample cobol-harness/run-cobol-baseline.sh
OUT_DIR=src/test/resources/cobol-baseline/edge cobol-harness/run-cobol-baseline.sh \
  src/test/resources/scenarios/edge-dailytran.txt src/test/resources/scenarios/edge-cardxref.txt
```

`CobolComparisonTest` runs the Java job twice per scenario. With the original credit-limit formula plugged in
(test code only), every output file matches COBOL line for line (processed timestamps excepted). With the fix,
only transactions after a refund on the same card change outcome, and only those accounts differ.
