package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.RejectReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.aws.carddemo.batch.posting.Fixtures.account;
import static com.aws.carddemo.batch.posting.Fixtures.transaction;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionValidatorTest {

    private final TransactionValidator validator = new TransactionValidator();

    @Test
    void acceptsUpToAndIncludingTheCreditLimit() {
        Account account = account("1000.00", "0.00", "0.00", "2025-05-20");
        assertEquals(Optional.empty(), validator.validate(account, transaction("T1", "1000.00", "2024-01-01")));
        assertEquals(Optional.of(RejectReason.OVER_LIMIT),
                validator.validate(account, transaction("T1", "1000.01", "2024-01-01")));
    }

    @Test
    void refundsReduceTheCycleTotal() {
        // 900.00 bought, 300.00 refunded: 600.00 net, so 400.00 more fits a 1000.00 limit.
        Account account = account("1000.00", "900.00", "-300.00", "2025-05-20");
        assertEquals(new BigDecimal("600.00"), account.currentCycleNet());
        assertEquals(Optional.empty(), validator.validate(account, transaction("T1", "400.00", "2024-01-01")));
        assertEquals(Optional.of(RejectReason.OVER_LIMIT),
                validator.validate(account, transaction("T1", "400.01", "2024-01-01")));
    }

    @Test
    void theOriginalCobolCheckCountedRefundsAsSpending() {
        Account account = account("1000.00", "900.00", "-300.00", "2025-05-20");
        TransactionValidator cobol = new TransactionValidator(SampleData.COBOL_CREDIT_LIMIT_CHECK);
        // 900.00 - (-300.00) + 1.00 = 1201.00 > 1000.00
        assertEquals(Optional.of(RejectReason.OVER_LIMIT), cobol.validate(account, transaction("T1", "1.00", "2024-01-01")));
    }

    @Test
    void rejectsTransactionsAfterTheExpirationDate() {
        Account account = account("1000.00", "0.00", "0.00", "2025-05-20");
        assertEquals(Optional.empty(), validator.validate(account, transaction("T1", "1.00", "2025-05-20")));
        assertEquals(Optional.of(RejectReason.AFTER_EXPIRATION),
                validator.validate(account, transaction("T1", "1.00", "2025-05-21")));
    }

    @Test
    void expirationIsReportedWhenBothChecksFail() {
        Account account = account("1000.00", "0.00", "0.00", "2025-05-20");
        assertEquals(Optional.of(RejectReason.AFTER_EXPIRATION),
                validator.validate(account, transaction("T1", "5000.00", "2025-06-01")));
    }
}
