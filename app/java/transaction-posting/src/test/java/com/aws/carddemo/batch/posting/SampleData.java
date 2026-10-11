package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.io.CardDemoRecords;
import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.CardCrossReference;
import com.aws.carddemo.batch.posting.model.Transaction;
import com.aws.carddemo.batch.posting.model.TransactionCategoryBalance;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

final class SampleData {

    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2022-07-18T01:02:03.45Z"), ZoneOffset.UTC);
    static final String FIXED_TIMESTAMP = "2022-07-18-01.02.03.450000";

    /** The literal CBTRN02C check: ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT > ACCT-CREDIT-LIMIT. */
    static final TransactionValidator.CreditLimitCheck COBOL_CREDIT_LIMIT_CHECK = (account, amount) ->
            account.currentCycleCredit().subtract(account.currentCycleDebit()).add(amount)
                    .compareTo(account.creditLimit()) > 0;

    private SampleData() {
    }

    /** The shipped CardDemo ASCII sample files (app/data/ASCII). */
    static Path ascii(String fileName) {
        return Path.of(System.getProperty("carddemo.data.dir", "../../data/ASCII")).resolve(fileName);
    }

    static Path resource(String path) {
        try {
            return Path.of(SampleData.class.getResource("/" + path).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Outputs of the original COBOL run, produced by cobol-harness/run-cobol-baseline.sh. */
    static Path cobolBaseline(String scenario, String fileName) {
        return resource("cobol-baseline/" + scenario + "/" + fileName);
    }

    /** Inputs for a scenario: "sample" is app/data/ASCII as shipped, "edge" adds hand-made edge cases. */
    record Inputs(List<Transaction> daily, List<CardCrossReference> xrefs, List<Account> accounts,
                  List<TransactionCategoryBalance> balances) {

        static Inputs load(String scenario) throws IOException {
            boolean sample = scenario.equals("sample");
            Path daily = sample ? ascii("dailytran.txt") : resource("scenarios/edge-dailytran.txt");
            Path xref = sample ? ascii("cardxref.txt") : resource("scenarios/edge-cardxref.txt");
            return new Inputs(
                    CardDemoRecords.readAll(daily, CardDemoRecords::parseTransaction),
                    CardDemoRecords.readAll(xref, CardDemoRecords::parseCrossReference),
                    CardDemoRecords.readAll(ascii("acctdata.txt"), CardDemoRecords::parseAccount),
                    CardDemoRecords.readAll(ascii("tcatbal.txt"), CardDemoRecords::parseCategoryBalance));
        }

        TransactionPostingJob.Result run(TransactionValidator validator, List<String> sysout) {
            return TransactionPostingMain.run(daily, xrefs, accounts, balances, validator, FIXED_CLOCK, sysout::add);
        }
    }
}
