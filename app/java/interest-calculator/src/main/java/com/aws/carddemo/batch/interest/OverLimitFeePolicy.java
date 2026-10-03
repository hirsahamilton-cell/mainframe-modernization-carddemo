package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.model.Account;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Over-limit fee (1400-COMPUTE-FEES, an unimplemented stub in the COBOL original):
 * a flat fee when ACCT-CURR-BAL exceeds ACCT-CREDIT-LIMIT. Nothing is charged at or under the limit.
 */
public final class OverLimitFeePolicy {

    public static final BigDecimal OVER_LIMIT_FEE = new BigDecimal("29.00");

    private OverLimitFeePolicy() {
    }

    public static Optional<BigDecimal> feeFor(Account account) {
        return account.isOverCreditLimit() ? Optional.of(OVER_LIMIT_FEE) : Optional.empty();
    }
}
