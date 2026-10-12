package com.aws.carddemo.batch.report;

import com.aws.carddemo.batch.report.io.CardDemoRecords;
import com.aws.carddemo.batch.report.model.DateRange;
import com.aws.carddemo.batch.report.model.Transaction;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

final class SampleData {

    /** CBTRN03C's page break check: MOD(WS-LINE-COUNTER, WS-PAGE-SIZE) = 0. */
    static final PageBreakRule COBOL_PAGE_BREAK = (lineCounter, lineCounterAtLastBreak) ->
            lineCounter % PageBreakRule.PAGE_SIZE == 0;

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

    static TransactionReportJob job(PageBreakRule rule) throws IOException {
        return new TransactionReportJob(
                CardDemoRecords.readAll(ascii("cardxref.txt"), CardDemoRecords::parseCrossReference),
                CardDemoRecords.readAll(ascii("trantype.txt"), CardDemoRecords::parseTransactionType),
                CardDemoRecords.readAll(ascii("trancatg.txt"), CardDemoRecords::parseTransactionCategory),
                rule);
    }

    /**
     * "sample" is the COBOL POSTTRAN output for the shipped daily transactions (all processed on one day);
     * "edge" is hand-made: out-of-range dates, unsorted cards, zero/small/large/negative amounts, interest and
     * fee categories, a card that nets to zero and a layout where CBTRN03C skips a page break.
     */
    record Scenario(String name, List<Transaction> transactions, DateRange range) {

        static Scenario load(String name) throws IOException {
            DateRange range = name.equals("sample")
                    ? new DateRange("2026-10-11", "2026-10-11")
                    : new DateRange("2026-01-01", "2026-01-31");
            return new Scenario(name, CardDemoRecords.readAll(resource("scenarios/" + name + "-transact.txt"),
                    CardDemoRecords::parseTransaction), range);
        }

        Path transactFile() {
            return resource("scenarios/" + name + "-transact.txt");
        }
    }
}
