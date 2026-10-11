package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.io.CardDemoRecords;
import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.RejectReason;
import com.aws.carddemo.batch.posting.model.RejectedTransaction;
import com.aws.carddemo.batch.posting.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.aws.carddemo.batch.posting.SampleData.cobolBaseline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares the Java port with the original CBTRN02C compiled by GnuCOBOL and run on the same inputs
 * (see cobol-harness/run-cobol-baseline.sh). Two scenarios: the shipped sample data and hand-made edge cases.
 */
class CobolComparisonTest {

    private static final int PROC_TS_START = 304;
    private static final int PROC_TS_END = 330;
    private static final String PROC_TS_PATTERN = "\\d{4}-\\d\\d-\\d\\d-\\d\\d\\.\\d\\d\\.\\d\\d\\.\\d\\d0000";

    /** With the original credit-limit check plugged in, every output matches the COBOL run line for line. */
    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void withCobolCreditLimitCheckEveryOutputMatches(String scenario) throws IOException {
        List<String> sysout = new ArrayList<>();
        TransactionPostingJob.Result java = SampleData.Inputs.load(scenario)
                .run(new TransactionValidator(SampleData.COBOL_CREDIT_LIMIT_CHECK), sysout);

        List<String> cobolSysout = cobolLines(scenario, "posttran-sysout.txt");
        String cobolReturnCode = cobolSysout.remove(cobolSysout.size() - 1);
        assertEquals(cobolSysout, sysout);
        assertEquals("RETURN-CODE " + java.returnCode(), cobolReturnCode);

        assertLinesMatch(cobolLines(scenario, "posttran-acctdata.txt"),
                format(java.accounts(), CardDemoRecords::formatAccount));
        assertLinesMatch(cobolLines(scenario, "posttran-tcatbal.txt"),
                format(java.categoryBalances(), CardDemoRecords::formatCategoryBalance));
        assertLinesMatch(cobolLines(scenario, "posttran-dalyrejs.txt"),
                format(java.rejectedTransactions(), CardDemoRecords::formatRejectedTransaction));

        List<String> cobolPosted = cobolLines(scenario, "posttran-transact.txt");
        List<String> javaPosted = format(java.postedTransactions(), CardDemoRecords::formatTransaction);
        assertEquals(cobolPosted.size(), javaPosted.size());
        for (int i = 0; i < cobolPosted.size(); i++) {
            String cobol = cobolPosted.get(i);
            String port = javaPosted.get(i);
            assertTrue(cobol.substring(PROC_TS_START, PROC_TS_END).matches(PROC_TS_PATTERN), cobol);
            assertEquals(SampleData.FIXED_TIMESTAMP, port.substring(PROC_TS_START, PROC_TS_END));
            assertEquals(withoutProcessedTimestamp(cobol), withoutProcessedTimestamp(port), "transaction " + i);
        }
    }

    /**
     * With the fixed check (refunds reduce the cycle total), only transactions after a refund on the same card can
     * change outcome, and only the accounts and category balances they touch can differ.
     */
    @ParameterizedTest
    @ValueSource(strings = {"sample", "edge"})
    void fixedCreditLimitCheckOnlyChangesTransactionsAfterARefund(String scenario) throws IOException {
        SampleData.Inputs inputs = SampleData.Inputs.load(scenario);
        TransactionPostingJob.Result java = inputs.run(new TransactionValidator(), new ArrayList<>());
        Map<String, RejectReason> cobolRejects = cobolLines(scenario, "posttran-dalyrejs.txt").stream()
                .collect(Collectors.toMap(l -> l.substring(0, 16),
                        l -> reasonOf(Integer.parseInt(l.substring(350, 354)))));
        Map<String, RejectReason> javaRejects = java.rejectedTransactions().stream()
                .collect(Collectors.toMap(r -> r.transaction().id(), RejectedTransaction::reason));
        Map<Long, Account> cobolAccounts = byId(cobolLines(scenario, "posttran-acctdata.txt"));
        Map<Long, Account> javaAccounts = java.accounts().stream()
                .collect(Collectors.toMap(Account::id, Function.identity()));
        Map<String, Long> accountByCard = inputs.xrefs().stream()
                .collect(Collectors.toMap(x -> x.cardNumber(), x -> x.accountId(), (a, b) -> a));

        List<String> report = new ArrayList<>();
        report.add(String.format("%-16s %-11s %10s  %-10s %-10s", "TRAN-ID", "ACCOUNT", "AMOUNT", "COBOL", "JAVA"));
        Set<String> refundedCards = new HashSet<>();
        Set<Long> changedAccounts = new HashSet<>();
        for (Transaction t : inputs.daily()) {
            String cobol = outcome(cobolRejects.get(t.id()));
            String port = outcome(javaRejects.get(t.id()));
            if (!cobol.equals(port)) {
                assertTrue(refundedCards.contains(t.cardNumber()), t.id() + " changed without an earlier refund");
                assertTrue(cobol.equals("rejected 102") || port.equals("rejected 102"), t.id());
                long account = accountByCard.get(t.cardNumber());
                changedAccounts.add(account);
                report.add(String.format("%-16s %011d %10s  %-10s %-10s", t.id(), account, t.amount(), cobol, port));
            }
            if (t.amount().signum() < 0 && !javaRejects.containsKey(t.id())) {
                refundedCards.add(t.cardNumber());
            }
        }
        report.add(String.format("%d of %d transactions changed outcome; posted: COBOL %d, Java %d; rejected: COBOL %d, Java %d",
                report.size() - 1, inputs.daily().size(), inputs.daily().size() - cobolRejects.size(),
                java.postedTransactions().size(), cobolRejects.size(), javaRejects.size()));
        report.add("");
        report.add(String.format("%-11s %14s %14s %14s %14s", "ACCOUNT", "COBOL BAL", "JAVA BAL", "COBOL CYC-CR",
                "JAVA CYC-CR"));
        for (Account cobol : cobolAccounts.values()) {
            Account port = javaAccounts.get(cobol.id());
            if (changedAccounts.contains(cobol.id())) {
                report.add(String.format("%011d %14s %14s %14s %14s", cobol.id(), cobol.currentBalance(),
                        port.currentBalance(), cobol.currentCycleCredit(), port.currentCycleCredit()));
            } else {
                assertEquals(cobol, port, "account " + cobol.id() + " should be unaffected");
            }
        }
        List<String> cobolBalances = cobolLines(scenario, "posttran-tcatbal.txt");
        List<String> javaBalances = format(java.categoryBalances(), CardDemoRecords::formatCategoryBalance);
        assertEquals(cobolBalances.size(), javaBalances.size());
        for (int i = 0; i < cobolBalances.size(); i++) {
            if (!changedAccounts.contains(Long.parseLong(cobolBalances.get(i).substring(0, 11)))) {
                assertEquals(cobolBalances.get(i), javaBalances.get(i));
            }
        }
        writeReport(scenario, report);
    }

    @Test
    void edgeCasesBehaveAsDocumented() throws IOException {
        TransactionPostingJob.Result java = SampleData.Inputs.load("edge").run(new TransactionValidator(), new ArrayList<>());
        Map<String, Integer> rejects = java.rejectedTransactions().stream()
                .collect(Collectors.toMap(r -> r.transaction().id(), r -> r.reason().code(), (a, b) -> a, LinkedHashMap::new));
        assertEquals(Map.of(
                "EDGE000000000001", 100,
                "EDGE000000000002", 101,
                "EDGE000000000003", 103,
                "EDGE000000000005", 103,
                "EDGE000000000008", 102,
                "EDGE000000000012", 102), rejects);
        Set<String> posted = java.postedTransactions().stream().map(Transaction::id).collect(Collectors.toSet());
        assertEquals(Set.of("EDGE000000000004", "EDGE000000000006", "EDGE000000000007", "EDGE000000000009",
                "EDGE000000000010", "EDGE000000000011"), posted);
        assertEquals(4, java.returnCode());
    }

    private static String outcome(RejectReason reason) {
        return reason == null ? "posted" : "rejected " + reason.code();
    }

    private static RejectReason reasonOf(int code) {
        for (RejectReason r : RejectReason.values()) {
            if (r.code() == code) {
                return r;
            }
        }
        throw new IllegalArgumentException("unknown reject reason " + code);
    }

    private static Map<Long, Account> byId(List<String> lines) {
        return lines.stream().map(CardDemoRecords::parseAccount)
                .collect(Collectors.toMap(Account::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static String withoutProcessedTimestamp(String line) {
        return line.substring(0, PROC_TS_START) + line.substring(PROC_TS_END);
    }

    private static <T> List<String> format(List<T> records, Function<T, String> formatter) {
        return records.stream().map(formatter).map(String::stripTrailing).toList();
    }

    private static List<String> cobolLines(String scenario, String file) throws IOException {
        return Files.readAllLines(cobolBaseline(scenario, file), StandardCharsets.US_ASCII).stream()
                .map(String::stripTrailing).filter(l -> !l.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static void assertLinesMatch(List<String> cobol, List<String> java) {
        assertEquals(cobol.size(), java.size());
        for (int i = 0; i < cobol.size(); i++) {
            assertEquals(cobol.get(i), java.get(i), "record " + i);
        }
    }

    private static void writeReport(String scenario, List<String> lines) throws IOException {
        Path out = Path.of("target", "cobol-vs-java-" + scenario + ".txt");
        Files.createDirectories(out.getParent());
        Files.write(out, lines);
        System.out.println("---- COBOL vs Java (fixed credit-limit check): " + scenario + " ----");
        lines.forEach(System.out::println);
    }
}
