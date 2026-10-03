package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.io.CardDemoRecords;
import com.aws.carddemo.batch.interest.model.Account;
import com.aws.carddemo.batch.interest.model.CardCrossReference;
import com.aws.carddemo.batch.interest.model.DisclosureGroup;
import com.aws.carddemo.batch.interest.model.Transaction;
import com.aws.carddemo.batch.interest.model.TransactionCategoryBalance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.aws.carddemo.batch.interest.SampleData.ascii;
import static com.aws.carddemo.batch.interest.SampleData.cobolBaseline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares the Java port with the original CBACT04C compiled by GnuCOBOL and run on the same inputs
 * (see cobol-harness/run-cobol-baseline.sh). Inputs are the sample data after POSTTRAN, plus the
 * credit-limit scenario: account 10 over its limit, account 20 exactly at it.
 */
class CobolComparisonTest {

    private static final long OVER_LIMIT_ACCOUNT = 10;
    private static final long AT_LIMIT_ACCOUNT = 20;
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    private static List<TransactionCategoryBalance> balances;
    private static Map<Long, Account> inputAccounts;
    private static InterestRateTable rates;
    private static List<Transaction> cobolTransactions;
    private static Map<Long, Account> cobolAccounts;
    private static InterestCalculationJob.Result java;

    @BeforeAll
    static void runBoth() throws IOException {
        balances = CardDemoRecords.readAll(cobolBaseline("intcalc-input-tcatbal.txt"), CardDemoRecords::parseCategoryBalance);
        List<Account> accounts = CardDemoRecords.readAll(cobolBaseline("intcalc-input-acctdata.txt"), CardDemoRecords::parseAccount);
        List<CardCrossReference> xrefs = CardDemoRecords.readAll(ascii("cardxref.txt"), CardDemoRecords::parseCrossReference);
        List<DisclosureGroup> groups = CardDemoRecords.readAll(ascii("discgrp.txt"), CardDemoRecords::parseDisclosureGroup);
        inputAccounts = byId(accounts);
        rates = new InterestRateTable(groups);
        cobolTransactions = CardDemoRecords.readAll(cobolBaseline("intcalc-transact.txt"), CardDemoRecords::parseTransaction);
        cobolAccounts = byId(CardDemoRecords.readAll(cobolBaseline("intcalc-acctdata.txt"), CardDemoRecords::parseAccount));

        java = InterestCalculatorMain.run(SampleData.PARM_DATE, SampleData.FIXED_CLOCK, balances, xrefs, accounts, groups);
    }

    private static Map<Long, Account> byId(List<Account> accounts) {
        return accounts.stream().collect(Collectors.toMap(Account::id, Function.identity()));
    }

    private static long accountOf(Transaction t) {
        return Long.parseLong(t.description().substring(t.description().length() - 11));
    }

    private static List<Transaction> interestOnly(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.categoryCode() == TransactionFactory.INTEREST_CATEGORY_CODE).toList();
    }

    @Test
    void interestMatchesCobolExceptWhereCobolTruncated() throws IOException {
        List<Transaction> javaInterest = interestOnly(java.transactions());
        List<TransactionCategoryBalance> charged = balances.stream()
                .filter(b -> rates.annualRateFor(inputAccounts.get(b.accountId()).groupId(), b.typeCode(), b.categoryCode()).signum() != 0)
                .toList();
        assertEquals(charged.size(), cobolTransactions.size());
        assertEquals(charged.size(), javaInterest.size());

        List<String> report = new ArrayList<>();
        report.add(String.format("%-11s %-7s %12s %6s %14s %10s %10s  %s",
                "ACCOUNT", "TYPE/CAT", "CAT-BAL", "APR", "EXACT", "COBOL", "JAVA", ""));
        int differences = 0;
        for (int i = 0; i < charged.size(); i++) {
            TransactionCategoryBalance b = charged.get(i);
            Transaction cobol = cobolTransactions.get(i);
            Transaction port = javaInterest.get(i);
            BigDecimal rate = rates.annualRateFor(inputAccounts.get(b.accountId()).groupId(), b.typeCode(), b.categoryCode());
            BigDecimal exact = b.balance().multiply(rate).divide(TWELVE_HUNDRED, 10, RoundingMode.UNNECESSARY).stripTrailingZeros();

            assertEquals(b.accountId(), accountOf(cobol));
            assertEquals(b.accountId(), accountOf(port));
            assertEquals(cobol.description(), port.description());
            assertEquals(cobol.cardNumber(), port.cardNumber());
            assertEquals(cobol.typeCode() + cobol.categoryCode(), port.typeCode() + port.categoryCode());
            assertEquals(exact.setScale(2, RoundingMode.DOWN), cobol.amount(), "COBOL truncates");
            assertEquals(exact.setScale(2, RoundingMode.HALF_UP), port.amount(), "Java rounds half-up");

            boolean differs = cobol.amount().compareTo(port.amount()) != 0;
            if (differs) {
                differences++;
            }
            report.add(String.format("%011d %s/%04d %12s %6s %14s %10s %10s  %s",
                    b.accountId(), b.typeCode(), b.categoryCode(), b.balance(), rate, exact.toPlainString(),
                    cobol.amount(), port.amount(), differs ? "<-- truncated by COBOL" : ""));
        }
        BigDecimal cobolTotal = cobolTransactions.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal javaTotal = javaInterest.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        report.add(String.format("%d interest transactions, %d differ; total COBOL %s, Java %s",
                charged.size(), differences, cobolTotal, javaTotal));
        writeReport("interest", report);

        assertTrue(differences > 0, "sample data should contain at least one truncation case");
        // Account 2, 01/0001: 2339.97 x 15% / 12 = 29.249625 -> COBOL 29.24, Java 29.25
        Transaction cobolAcct2 = cobolTransactions.stream().filter(t -> accountOf(t) == 2).findFirst().orElseThrow();
        Transaction javaAcct2 = javaInterest.stream().filter(t -> accountOf(t) == 2).findFirst().orElseThrow();
        assertEquals(new BigDecimal("29.24"), cobolAcct2.amount());
        assertEquals(new BigDecimal("29.25"), javaAcct2.amount());
    }

    @Test
    void overLimitFeeChargedOnlyAboveLimit() throws IOException {
        Account over = inputAccounts.get(OVER_LIMIT_ACCOUNT);
        Account atLimit = inputAccounts.get(AT_LIMIT_ACCOUNT);
        assertTrue(over.currentBalance().compareTo(over.creditLimit()) > 0);
        assertEquals(0, atLimit.currentBalance().compareTo(atLimit.creditLimit()));

        List<Transaction> javaFees = java.transactions().stream()
                .filter(t -> t.categoryCode() == TransactionFactory.OVER_LIMIT_FEE_CATEGORY_CODE).toList();
        assertEquals(1, javaFees.size(), "only account 10 is over its limit");
        Transaction fee = javaFees.get(0);
        assertEquals(OVER_LIMIT_ACCOUNT, accountOf(fee));
        assertEquals(new BigDecimal("29.00"), fee.amount());
        assertEquals("01", fee.typeCode());
        assertEquals("Over-limit fee for a/c 00000000010", fee.description());

        assertTrue(cobolTransactions.stream().allMatch(t -> t.categoryCode() == TransactionFactory.INTEREST_CATEGORY_CODE),
                "the COBOL 1400-COMPUTE-FEES stub posts no fees");

        writeReport("fees", List.of(
                String.format("account %011d: balance %s > limit %s -> COBOL fee: none, Java fee: %s",
                        OVER_LIMIT_ACCOUNT, over.currentBalance(), over.creditLimit(), fee.amount()),
                String.format("account %011d: balance %s = limit %s -> COBOL fee: none, Java fee: none",
                        AT_LIMIT_ACCOUNT, atLimit.currentBalance(), atLimit.creditLimit()),
                String.format("other 48 accounts under limit -> no fee from either")));
    }

    @Test
    void accountBalancesIncludeRoundedInterestAndFees() throws IOException {
        Map<Long, BigDecimal> javaCharges = java.transactions().stream().collect(Collectors.groupingBy(
                CobolComparisonTest::accountOf, Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
        Map<Long, BigDecimal> cobolCharges = cobolTransactions.stream().collect(Collectors.groupingBy(
                CobolComparisonTest::accountOf, Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
        long lastAccount = balances.get(balances.size() - 1).accountId();

        List<String> report = new ArrayList<>();
        assertEquals(inputAccounts.size(), java.updatedAccounts().size());
        for (Account updated : java.updatedAccounts()) {
            Account input = inputAccounts.get(updated.id());
            Account cobol = cobolAccounts.get(updated.id());
            assertEquals(input.currentBalance().add(javaCharges.getOrDefault(updated.id(), BigDecimal.ZERO)),
                    updated.currentBalance());
            assertEquals(new BigDecimal("0.00"), updated.currentCycleCredit());
            assertEquals(new BigDecimal("0.00"), updated.currentCycleDebit());

            if (updated.id() == lastAccount) {
                // CBACT04C never runs 1050-UPDATE-ACCOUNT for the final account (the EOF branch is unreachable).
                assertEquals(input.currentBalance(), cobol.currentBalance());
                assertEquals(input.currentCycleCredit(), cobol.currentCycleCredit());
                report.add(String.format("account %011d (last): COBOL balance left at %s (cycle credit %s not reset);"
                                + " Java posts %s -> %s",
                        updated.id(), cobol.currentBalance(), cobol.currentCycleCredit(),
                        javaCharges.get(updated.id()), updated.currentBalance()));
            } else {
                assertEquals(input.currentBalance().add(cobolCharges.get(updated.id())), cobol.currentBalance());
            }
        }
        writeReport("accounts", report);
    }

    private static void writeReport(String section, List<String> lines) throws IOException {
        Path out = Path.of("target", "cobol-vs-java-" + section + ".txt");
        Files.createDirectories(out.getParent());
        Files.write(out, lines);
        System.out.println("---- COBOL vs Java: " + section + " ----");
        lines.forEach(System.out::println);
    }
}
