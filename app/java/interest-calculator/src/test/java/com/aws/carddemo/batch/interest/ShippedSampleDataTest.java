package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.io.CardDemoRecords;
import com.aws.carddemo.batch.interest.model.Account;
import com.aws.carddemo.batch.interest.model.CardCrossReference;
import com.aws.carddemo.batch.interest.model.DisclosureGroup;
import com.aws.carddemo.batch.interest.model.TransactionCategoryBalance;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static com.aws.carddemo.batch.interest.SampleData.ascii;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the job directly on the files shipped in app/data/ASCII, before any POSTTRAN run. */
class ShippedSampleDataTest {

    @Test
    void zeroCategoryBalancesAndNoOverLimitAccountsProduceNoCharges() throws IOException {
        List<TransactionCategoryBalance> balances =
                CardDemoRecords.readAll(ascii("tcatbal.txt"), CardDemoRecords::parseCategoryBalance);
        List<CardCrossReference> xrefs = CardDemoRecords.readAll(ascii("cardxref.txt"), CardDemoRecords::parseCrossReference);
        List<Account> accounts = CardDemoRecords.readAll(ascii("acctdata.txt"), CardDemoRecords::parseAccount);
        List<DisclosureGroup> groups = CardDemoRecords.readAll(ascii("discgrp.txt"), CardDemoRecords::parseDisclosureGroup);

        assertEquals(50, balances.size());
        assertTrue(balances.stream().allMatch(b -> b.balance().signum() == 0), "shipped tcatbal.txt is all zero");
        assertTrue(accounts.stream().noneMatch(Account::isOverCreditLimit), "no shipped account is over its limit");
        assertTrue(accounts.stream().allMatch(a -> a.groupId().isBlank()), "shipped accounts have a blank group id");

        InterestCalculationJob.Result result = InterestCalculatorMain.run(
                SampleData.PARM_DATE, SampleData.FIXED_CLOCK, balances, xrefs, accounts, groups);

        // 01/0001 resolves to DEFAULT's 15% rate, so one (zero-amount) interest transaction per account.
        assertEquals(50, result.transactions().size());
        assertTrue(result.transactions().stream().allMatch(t -> t.amount().signum() == 0));
        assertTrue(result.transactions().stream().allMatch(t -> t.categoryCode() == TransactionFactory.INTEREST_CATEGORY_CODE));
        assertEquals(50, result.updatedAccounts().size(), "every account, including the last, is updated");
        for (Account updated : result.updatedAccounts()) {
            Account original = accounts.stream().filter(a -> a.id() == updated.id()).findFirst().orElseThrow();
            assertEquals(original.currentBalance(), updated.currentBalance());
            assertEquals(new BigDecimal("0.00"), updated.currentCycleCredit());
        }
    }

    @Test
    void transactionRecordsMatchTheCopybookLayout() throws IOException {
        TransactionFactory factory = new TransactionFactory(SampleData.PARM_DATE, SampleData.FIXED_CLOCK);
        String interest = CardDemoRecords.formatTransaction(
                factory.interest(1, "0500024453765740", new BigDecimal("14.56")));
        String fee = CardDemoRecords.formatTransaction(
                factory.overLimitFee(1, "0500024453765740", new BigDecimal("29.00")));

        assertEquals(350, interest.length());
        assertEquals("2022071800000001010005System    Int. for a/c 00000000001", interest.substring(0, 56));
        assertEquals("0000000145F000000000", interest.substring(132, 152));
        assertEquals("05000244537657402022-07-18-01.02.03.450000", interest.substring(262, 304));
        assertEquals("2022071800000002010006System    Over-limit fee for a/c 00000000001", fee.substring(0, 66));
        assertEquals("0000000290{", fee.substring(132, 143));
    }
}
