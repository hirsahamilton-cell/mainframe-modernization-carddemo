# CardDemo interest calculator (Java port of CBACT04C)

Java 17 port of the `INTCALC` batch step (`app/cbl/CBACT04C.cbl`). It reads the transaction category
balance file and posts monthly interest and over-limit fees as transaction records.

| COBOL paragraph | Java |
| --- | --- |
| main loop, `1050-UPDATE-ACCOUNT` | `InterestCalculationJob`, `Account.withCyclePosted` |
| `1200-GET-INTEREST-RATE` / `1200-A-GET-DEFAULT-INT-RATE` | `InterestRateTable` |
| `1300-COMPUTE-INTEREST` | `InterestCalculator` (now rounds half-up to cents) |
| `1300-B-WRITE-TX` | `TransactionFactory` |
| `1400-COMPUTE-FEES` (a stub in COBOL) | `OverLimitFeePolicy` ($29.00 when `ACCT-CURR-BAL > ACCT-CREDIT-LIMIT`) |
| copybook layouts CVACT01Y, CVACT03Y, CVTRA01Y, CVTRA02Y, CVTRA05Y | `model.*`, `io.CardDemoRecords`, `io.ZonedDecimal` |

## Build and test

```sh
mvn test        # unit tests + comparison against the GnuCOBOL baseline
mvn package
java -jar target/interest-calculator-1.0.0-SNAPSHOT.jar \
  2022071800 TCATBAL XREF ACCTDATA DISCGRP OUT-TRANSACT OUT-ACCTDATA
```

Input and output files are the ASCII, one-record-per-line versions of the datasets (as in `app/data/ASCII`).
After `mvn test`, `target/cobol-vs-java-*.txt` contain the side-by-side comparison.

## COBOL baseline

`cobol-harness/run-cobol-baseline.sh` compiles the original `CBTRN02C` (POSTTRAN) and `CBACT04C` with
GnuCOBOL 3.x (`apt install gnucobol3`), runs them against `app/data/ASCII` and writes the results to
`cobol-harness/out/`. The checked-in copies in `src/test/resources/cobol-baseline/` came from this script.
Re-run it and copy `out/intcalc-*.txt` there if the sample data or the COBOL source changes.
