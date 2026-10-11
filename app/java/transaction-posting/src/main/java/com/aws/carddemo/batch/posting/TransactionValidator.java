package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.RejectReason;
import com.aws.carddemo.batch.posting.model.Transaction;

import java.math.BigDecimal;
import java.util.Optional;

/** Account-level checks from 1500-B-LOOKUP-ACCT (card and account lookups happen in the job). */
public final class TransactionValidator {

    /** Decides whether posting {@code amount} would take the account over its credit limit. */
    @FunctionalInterface
    public interface CreditLimitCheck {
        boolean exceedsLimit(Account account, BigDecimal amount);
    }

    /**
     * Net cycle charges plus the new amount must not exceed the credit limit. CBTRN02C computes
     * {@code ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT}, but the cycle debit holds refunds as
     * negative amounts, so subtracting it counted refunds as extra spending.
     */
    public static final CreditLimitCheck NET_CYCLE_CHARGES =
            (account, amount) -> account.currentCycleNet().add(amount).compareTo(account.creditLimit()) > 0;

    private final CreditLimitCheck creditLimitCheck;

    public TransactionValidator() {
        this(NET_CYCLE_CHARGES);
    }

    public TransactionValidator(CreditLimitCheck creditLimitCheck) {
        this.creditLimitCheck = creditLimitCheck;
    }

    /**
     * Returns the reject reason, if any. Like CBTRN02C, both checks run and the expiration check is reported
     * when both fail (the reject trailer holds a single reason).
     */
    public Optional<RejectReason> validate(Account account, Transaction transaction) {
        if (account.expirationDate().compareTo(transaction.originDate()) < 0) {
            return Optional.of(RejectReason.AFTER_EXPIRATION);
        }
        if (creditLimitCheck.exceedsLimit(account, transaction.amount())) {
            return Optional.of(RejectReason.OVER_LIMIT);
        }
        return Optional.empty();
    }
}
