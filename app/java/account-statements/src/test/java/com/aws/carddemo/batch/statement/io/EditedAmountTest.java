package com.aws.carddemo.batch.statement.io;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditedAmountTest {

    @Test
    void balanceKeepsLeadingZeros() {
        assertEquals("000001945.87 ", EditedAmount.balance(new BigDecimal("1945.87")));
        assertEquals("000000000.00 ", EditedAmount.balance(BigDecimal.ZERO));
        assertEquals("000000150.25-", EditedAmount.balance(new BigDecimal("-150.25")));
        assertEquals("999999999.99 ", EditedAmount.balance(new BigDecimal("999999999.99")));
    }

    @Test
    void amountSuppressesLeadingZeros() {
        assertEquals("      183.88 ", EditedAmount.amount(new BigDecimal("183.88")));
        assertEquals("         .00 ", EditedAmount.amount(BigDecimal.ZERO));
        assertEquals("         .05 ", EditedAmount.amount(new BigDecimal("0.05")));
        assertEquals("       25.50-", EditedAmount.amount(new BigDecimal("-25.5")));
        assertEquals("999999999.99 ", EditedAmount.amount(new BigDecimal("999999999.99")));
        assertEquals(EditedAmount.WIDTH, EditedAmount.amount(new BigDecimal("-1")).length());
    }

    @Test
    void rejectsValuesThatDoNotFit() {
        assertTrue(EditedAmount.fits(new BigDecimal("-999999999.99")));
        assertFalse(EditedAmount.fits(new BigDecimal("1000000000.00")));
        assertThrows(IllegalArgumentException.class, () -> EditedAmount.amount(new BigDecimal("1000000000")));
        assertThrows(ArithmeticException.class, () -> EditedAmount.balance(new BigDecimal("0.001")));
    }
}
