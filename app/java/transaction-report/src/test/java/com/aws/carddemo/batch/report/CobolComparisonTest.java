package com.aws.carddemo.batch.report;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares the Java report with the report the original TRANREPT job (SORT + CBTRN03C) wrote under GnuCOBOL.
 * Two CBTRN03C defects are fixed on purpose and the comparison accounts for exactly those:
 * <ol>
 *   <li>the last card gets no account total and its last transaction is added to the final page and grand
 *       totals a second time (the end-of-file branch re-adds TRAN-AMT still held in the record area);</li>
 *   <li>a page break is skipped whenever an account total moves the line counter past a multiple of 20.</li>
 * </ol>
 */
class CobolComparisonTest {

    private static final int AMOUNT_COLUMN = 97;

    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void matchesCobolApartFromTheFinalTotalsFix(String scenario) throws IOException {
        SampleData.Scenario s = SampleData.Scenario.load(scenario);
        List<String> cobol = Files.readAllLines(SampleData.cobolBaseline(scenario, "tranrept-report.txt"),
                StandardCharsets.US_ASCII);
        List<String> java = trimmed(SampleData.job(SampleData.COBOL_PAGE_BREAK).run(s.transactions(), s.range()));

        BigDecimal lastAmount = amount(cobol.get(cobol.size() - 4));
        BigDecimal cobolLastPageTotal = amount(cobol.get(cobol.size() - 3));
        BigDecimal cobolGrandTotal = amount(cobol.get(cobol.size() - 1));
        List<String> expected = new ArrayList<>(cobol.subList(0, cobol.size() - 3));
        expected.add(java.get(java.size() - 5));
        expected.add("-".repeat(133));
        expected.add(ReportLines.pageTotal(cobolLastPageTotal.subtract(lastAmount)).stripTrailing());
        expected.add("-".repeat(133));
        expected.add(ReportLines.grandTotal(cobolGrandTotal.subtract(lastAmount)).stripTrailing());
        assertEquals(expected, java);

        BigDecimal details = s.transactions().stream().filter(t -> s.range().contains(t.processedDate()))
                .map(t -> t.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(details, total(java, "Grand Total").get(0));
        assertEquals(details.add(lastAmount), cobolGrandTotal);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void fixedPageBreaksOnlyMovePageLines(String scenario) throws IOException {
        SampleData.Scenario s = SampleData.Scenario.load(scenario);
        List<String> cobolRule = trimmed(SampleData.job(SampleData.COBOL_PAGE_BREAK).run(s.transactions(), s.range()));
        List<String> fixed = trimmed(SampleData.job(PageBreakRule.EVERY_20_LINES).run(s.transactions(), s.range()));
        List<String> cobolBaseline = Files.readAllLines(SampleData.cobolBaseline(scenario, "tranrept-report.txt"),
                StandardCharsets.US_ASCII);

        assertNotEquals(cobolRule, fixed, "the scenario should contain a skipped page break");
        assertEquals(withoutPageLines(cobolRule), withoutPageLines(fixed));
        assertEquals(total(cobolRule, "Grand Total"), total(fixed, "Grand Total"));
        assertTrue(longestPage(cobolBaseline) > 23, "COBOL skips a break: " + longestPage(cobolBaseline));
        assertTrue(longestPage(fixed) <= 23, "longest Java page: " + longestPage(fixed));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void writesTheSameProgressLinesAsCobol(String scenario) throws IOException {
        SampleData.Scenario s = SampleData.Scenario.load(scenario);
        List<String> cobolSysout = Files.readAllLines(SampleData.cobolBaseline(scenario, "tranrept-sysout.txt"),
                StandardCharsets.US_ASCII).stream()
                .filter(line -> line.startsWith("START OF") || line.startsWith("END OF")
                        || line.startsWith("Reporting from"))
                .toList();
        List<String> progress = new ArrayList<>();
        TransactionReportMain.run(SampleData.job(PageBreakRule.EVERY_20_LINES), s.transactions(),
                Optional.of(s.range()), progress::add);
        assertEquals(cobolSysout, progress);
    }

    static List<BigDecimal> total(List<String> report, String label) {
        return report.stream().filter(line -> line.startsWith(label)).map(CobolComparisonTest::amount).toList();
    }

    /** Reads back the edited amount in columns 98-112. */
    static BigDecimal amount(String line) {
        String field = String.format("%-133s", line).substring(AMOUNT_COLUMN, AMOUNT_COLUMN + 15);
        String digits = field.substring(1).replace(",", "").strip();
        if (digits.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal value = new BigDecimal(digits.startsWith(".") ? "0" + digits : digits);
        return field.charAt(0) == '-' ? value.negate() : value;
    }

    private static List<String> withoutPageLines(List<String> report) {
        List<String> kept = new ArrayList<>();
        for (int i = 0; i < report.size(); i++) {
            String line = report.get(i);
            boolean header = line.startsWith("DALYREPT") || line.startsWith("Transaction ID")
                    || (line.isEmpty() && i > 0 && report.get(i - 1).startsWith("DALYREPT"))
                    || (line.startsWith("---") && i > 0 && report.get(i - 1).startsWith("Transaction ID"));
            boolean pageTotal = line.startsWith("Page Total")
                    || (line.startsWith("---") && i > 0 && report.get(i - 1).startsWith("Page Total"));
            if (!header && !pageTotal) {
                kept.add(line);
            }
        }
        return kept;
    }

    /** Lines from a report-name header up to and including the page total and its dashes. */
    private static int longestPage(List<String> report) {
        int longest = 0;
        int start = -1;
        for (int i = 0; i < report.size(); i++) {
            if (report.get(i).startsWith("DALYREPT")) {
                start = i;
            } else if (report.get(i).startsWith("Page Total") && start >= 0) {
                longest = Math.max(longest, i + 2 - start);
            }
        }
        return longest;
    }

    private static List<String> trimmed(List<String> lines) {
        return lines.stream().map(String::stripTrailing).toList();
    }
}
