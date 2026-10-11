package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.CardCrossReference;
import com.aws.carddemo.batch.posting.model.RejectReason;
import com.aws.carddemo.batch.posting.model.TransactionCategoryBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.aws.carddemo.batch.posting.Fixtures.account;
import static com.aws.carddemo.batch.posting.Fixtures.transaction;
import static com.aws.carddemo.batch.posting.Fixtures.xref;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionPostingJobTest {

    private final List<String> sysout = new ArrayList<>();

    private TransactionPostingJob job(List<TransactionCategoryBalance> balances) {
        return new TransactionPostingJob(List.of(xref()), List.of(account("1000.00", "0.00", "0.00", "2025-05-20")),
                balances, new TransactionValidator(), SampleData.FIXED_CLOCK, sysout::add);
    }

    @Test
    void postsPurchasesAndRefundsToTheAccount() {
        TransactionPostingJob.Result result = job(List.of()).run(List.of(
                transaction("T1", "250.00", "2024-01-01"),
                transaction("T2", "-40.00", "2024-01-02")));

        Account account = result.accounts().get(0);
        assertEquals(new BigDecimal("310.00"), account.currentBalance());
        assertEquals(new BigDecimal("250.00"), account.currentCycleCredit());
        assertEquals(new BigDecimal("-40.00"), account.currentCycleDebit());
        assertEquals(List.of("T1", "T2"), result.postedTransactions().stream().map(t -> t.id()).toList());
        assertEquals(SampleData.FIXED_TIMESTAMP, result.postedTransactions().get(0).processedTimestamp());
        assertEquals(0, result.returnCode());
    }

    @Test
    void createsMissingCategoryBalancesAndUpdatesExistingOnes() {
        TransactionCategoryBalance existing = new TransactionCategoryBalance(1, "01", 1, new BigDecimal("10.00"));
        TransactionPostingJob.Result result = job(List.of(existing)).run(List.of(
                transaction("T1", "5.00", "2024-01-01"),
                transaction("T2", Fixtures.CARD, "02", 3, "7.00", "2024-01-01"),
                transaction("T3", Fixtures.CARD, "02", 3, "8.00", "2024-01-01")));

        assertEquals(List.of(
                new TransactionCategoryBalance(1, "01", 1, new BigDecimal("15.00")),
                new TransactionCategoryBalance(1, "02", 3, new BigDecimal("15.00"))), result.categoryBalances());
        assertEquals(List.of("TCATBAL record not found for key : 00000000001020003.. Creating."), sysout);
    }

    @Test
    void rejectsUnknownCardsAndMissingAccounts() {
        TransactionPostingJob job = new TransactionPostingJob(
                List.of(xref(), new CardCrossReference("4000000000000002", 2, 99)),
                List.of(account("1000.00", "0.00", "0.00", "2025-05-20")), List.of(),
                new TransactionValidator(), SampleData.FIXED_CLOCK, sysout::add);
        TransactionPostingJob.Result result = job.run(List.of(
                transaction("T1", "0000000000000000", "01", 1, "1.00", "2024-01-01"),
                transaction("T2", "4000000000000002", "01", 1, "1.00", "2024-01-01")));

        assertEquals(List.of(RejectReason.INVALID_CARD, RejectReason.ACCOUNT_NOT_FOUND),
                result.rejectedTransactions().stream().map(r -> r.reason()).toList());
        assertEquals(List.of(), result.postedTransactions());
        assertEquals(4, result.returnCode());
        assertEquals(2, result.processedCount());
    }

    @Test
    void duplicateTransactionIdAbends() {
        TransactionPostingException e = assertThrows(TransactionPostingException.class, () -> job(List.of()).run(List.of(
                transaction("T1", "1.00", "2024-01-01"),
                transaction("T1", "2.00", "2024-01-01"))));
        assertEquals("ERROR WRITING TO TRANSACTION FILE: duplicate transaction id T1", e.getMessage());
    }
}
