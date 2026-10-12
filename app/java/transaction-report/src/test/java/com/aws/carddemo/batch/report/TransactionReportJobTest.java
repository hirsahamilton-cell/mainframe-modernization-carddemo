package com.aws.carddemo.batch.report;

import com.aws.carddemo.batch.report.model.CardCrossReference;
import com.aws.carddemo.batch.report.model.DateRange;
import com.aws.carddemo.batch.report.model.Transaction;
import com.aws.carddemo.batch.report.model.TransactionCategory;
import com.aws.carddemo.batch.report.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionReportJobTest {

    private static final DateRange JANUARY = new DateRange("2026-01-01", "2026-01-31");
    private static final String CARD_A = "1111000011110000";
    private static final String CARD_B = "2222000022220000";

    private final TransactionReportJob job = new TransactionReportJob(
            List.of(new CardCrossReference(CARD_A, 1, 101), new CardCrossReference(CARD_B, 2, 202)),
            List.of(new TransactionType("01", "Purchase"), new TransactionType("02", "Payment")),
            List.of(new TransactionCategory("01", 1, "Regular Sales Draft"),
                    new TransactionCategory("01", 6, "Over-limit Fee"),
                    new TransactionCategory("02", 1, "Payment")));

    @Test
    void selectsTheDateRangeInclusiveAndSortsByCardKeepingInputOrder() {
        List<Transaction> selected = TransactionReportJob.select(List.of(
                tx("T1", CARD_B, "01", 1, "1.00", "2026-01-31"),
                tx("T2", CARD_A, "01", 1, "2.00", "2025-12-31"),
                tx("T3", CARD_A, "01", 1, "3.00", "2026-01-01"),
                tx("T4", CARD_B, "01", 1, "4.00", "2026-02-01"),
                tx("T5", CARD_A, "01", 1, "5.00", "2026-01-15")), JANUARY);
        assertEquals(List.of("T3", "T5", "T1"), selected.stream().map(t -> t.id().strip()).toList());
    }

    @Test
    void writesHeadersDetailsAndAllTotals() {
        List<String> report = trimmed(job.run(List.of(
                tx("T1", CARD_B, "02", 1, "-50.00", "2026-01-02"),
                tx("T2", CARD_A, "01", 1, "10.00", "2026-01-02"),
                tx("T3", CARD_A, "01", 6, "25.00", "2026-01-03")), JANUARY));
        String dots86 = ".".repeat(86);
        assertEquals(List.of(
                "DALYREPT                              Daily Transaction Report                 "
                        + "Date Range: 2026-01-01 to 2026-01-31",
                "",
                "Transaction ID   Account ID  Transaction Type   Tran Category                      "
                        + "Tran Source            Amount",
                "-".repeat(133),
                "T2               00000000101 01-Purchase        0001-Regular Sales Draft           "
                        + "POS TERM                10.00",
                "T3               00000000101 01-Purchase        0006-Over-limit Fee                "
                        + "POS TERM                25.00",
                "Account Total" + ".".repeat(84) + "+         35.00",
                "-".repeat(133),
                "T1               00000000202 02-Payment         0001-Payment                       "
                        + "POS TERM      -         50.00",
                "Account Total" + ".".repeat(84) + "-         50.00",
                "-".repeat(133),
                "Page Total " + dots86 + "-         15.00",
                "-".repeat(133),
                "Grand Total" + dots86 + "-         15.00"), report);
    }

    @Test
    void everyLineIs133Bytes() {
        job.run(List.of(tx("T1", CARD_A, "01", 1, "0.00", "2026-01-02")), JANUARY)
                .forEach(line -> assertEquals(133, line.length(), line));
    }

    @Test
    void writesNothingWhenNoTransactionIsInTheRange() {
        assertEquals(List.of(), job.run(List.of(), JANUARY));
        assertEquals(List.of(), job.run(List.of(tx("T1", CARD_A, "01", 1, "1.00", "2026-02-01")), JANUARY));
    }

    @Test
    void startsANewPageEvery20Lines() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            transactions.add(tx("T" + i, i < 15 ? CARD_A : CARD_B, "01", 1, "1.00", "2026-01-02"));
        }
        List<String> report = job.run(transactions, JANUARY);
        List<Integer> pageStarts = pageStarts(report);
        // 4 headers + 15 details + account total (lines 19-20): the line counter jumps from 19 to 21, which
        // CBTRN03C's MOD(counter, 20) = 0 test misses (its first page runs to line 40). Here the page still
        // breaks at the next detail line, and the next break stays on the multiple of 20 (line 40).
        assertEquals(List.of(0, 23, 42), pageStarts);
        assertEquals(List.of(0, 42), pageStarts(new TransactionReportJob(
                List.of(new CardCrossReference(CARD_A, 1, 101), new CardCrossReference(CARD_B, 2, 202)),
                List.of(new TransactionType("01", "Purchase")),
                List.of(new TransactionCategory("01", 1, "Regular Sales Draft")),
                SampleData.COBOL_PAGE_BREAK).run(transactions, JANUARY)));
        assertTrue(report.get(22).startsWith("-") && report.get(21).startsWith("Page Total"));
        assertEquals("Page Total " + ".".repeat(86) + "+         15.00", report.get(21).stripTrailing());
    }

    @Test
    void abendsOnReferenceDataThatIsNotOnFile() {
        assertAbend("INVALID CARD NUMBER : 9999000099990000",
                tx("T1", "9999000099990000", "01", 1, "1.00", "2026-01-02"));
        assertAbend("INVALID TRANSACTION TYPE : 09", tx("T1", CARD_A, "09", 1, "1.00", "2026-01-02"));
        assertAbend("INVALID TRAN CATG KEY : 020006", tx("T1", CARD_A, "02", 6, "1.00", "2026-01-02"));
    }

    @Test
    void ignoresBadReferencesOutsideTheDateRange() {
        assertEquals(List.of(), job.run(List.of(tx("T1", CARD_A, "09", 1, "1.00", "2026-02-02")), JANUARY));
    }

    private void assertAbend(String message, Transaction transaction) {
        TransactionReportException e = assertThrows(TransactionReportException.class,
                () -> job.run(List.of(transaction), JANUARY));
        assertEquals(message, e.getMessage());
    }

    @Test
    void sampleDataProducesTheSameTotalsAsTheDetails() throws IOException {
        SampleData.Scenario sample = SampleData.Scenario.load("sample");
        List<String> report = SampleData.job(PageBreakRule.EVERY_20_LINES).run(sample.transactions(), sample.range());
        BigDecimal details = sample.transactions().stream().map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(CobolComparisonTest.total(report, "Grand Total").get(0), details);
        assertEquals(sumOf(CobolComparisonTest.total(report, "Page Total")), details);
        assertEquals(sumOf(CobolComparisonTest.total(report, "Account Total")), details);
    }

    private static List<Integer> pageStarts(List<String> report) {
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < report.size(); i++) {
            if (report.get(i).startsWith("DALYREPT")) {
                starts.add(i);
            }
        }
        return starts;
    }

    private static BigDecimal sumOf(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<String> trimmed(List<String> lines) {
        return lines.stream().map(String::stripTrailing).toList();
    }

    private static Transaction tx(String id, String card, String type, int category, String amount, String date) {
        return new Transaction(String.format("%-16s", id), type, category, "POS TERM", new BigDecimal(amount), card,
                date + "-10.00.00.000000");
    }
}
