package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.model.Account;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverLimitFeePolicyTest {

    private static Account account(String balance, String limit) {
        BigDecimal zero = new BigDecimal("0.00");
        return new Account(1, "Y", new BigDecimal(balance), new BigDecimal(limit), zero,
                "", "", "", zero, zero, "", "");
    }

    @Test
    void chargesFlatFeeOnlyAboveTheLimit() {
        assertEquals(Optional.of(new BigDecimal("29.00")), OverLimitFeePolicy.feeFor(account("1000.01", "1000.00")));
        assertEquals(Optional.empty(), OverLimitFeePolicy.feeFor(account("1000.00", "1000.00")));
        assertEquals(Optional.empty(), OverLimitFeePolicy.feeFor(account("999.99", "1000.00")));
        assertEquals(Optional.empty(), OverLimitFeePolicy.feeFor(account("-50.00", "1000.00")));
    }
}
