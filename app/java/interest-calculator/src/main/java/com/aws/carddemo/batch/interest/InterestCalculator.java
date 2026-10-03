package com.aws.carddemo.batch.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monthly interest from a category balance and an annual percentage rate (1300-COMPUTE-INTEREST).
 *
 * <p>The COBOL original computed {@code (TRAN-CAT-BAL * DIS-INT-RATE) / 1200} without {@code ROUNDED},
 * which truncates fractional cents. Here the exact result is rounded to cents with
 * {@link RoundingMode#HALF_UP} (half away from zero), the same rule COBOL's {@code ROUNDED} applies.
 */
public final class InterestCalculator {

    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private InterestCalculator() {
    }

    public static BigDecimal monthlyInterest(BigDecimal categoryBalance, BigDecimal annualRatePercent) {
        return categoryBalance.multiply(annualRatePercent).divide(MONTHS_TIMES_PERCENT, 2, ROUNDING);
    }
}
