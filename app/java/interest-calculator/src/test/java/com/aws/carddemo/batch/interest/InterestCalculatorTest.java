package com.aws.carddemo.batch.interest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterestCalculatorTest {

    /** What CBACT04C computes: no ROUNDED clause, so the excess digits are dropped (truncated toward zero). */
    static BigDecimal cobolTruncated(BigDecimal balance, BigDecimal rate) {
        return balance.multiply(rate).divide(BigDecimal.valueOf(1200), 2, RoundingMode.DOWN);
    }

    @ParameterizedTest(name = "{0} @ {1}% -> {2}")
    @CsvSource({
            // balance, APR,  rounded, exact value (for reference)
            "1164.87, 15.00, 14.56",   // 14.560875  -> same either way
            "2339.97, 15.00, 29.25",   // 29.249625  -> truncation gives 29.24
            "100.00,  25.00, 2.08",    // 2.0833...  -> same either way
            "1000.00, 25.00, 20.83",   // 20.8333... -> same either way
            "10.00,   15.00, 0.13",    // 0.125      -> half rounds up; truncation gives 0.12
            "-70.77,  15.00, -0.88",   // -0.884625  -> half away from zero; truncation gives -0.88
            "-10.00,  15.00, -0.13",   // -0.125     -> truncation gives -0.12
            "0.00,    15.00, 0.00",
    })
    void roundsMonthlyInterestHalfUpToCents(String balance, String rate, String expected) {
        assertEquals(new BigDecimal(expected),
                InterestCalculator.monthlyInterest(new BigDecimal(balance), new BigDecimal(rate)));
    }

    @Test
    void truncationBugUnderchargesWhenFractionalCentIsHalfOrMore() {
        // Account 00000000002, category 01/0001 after POSTTRAN on the sample data: 2339.97 at the DEFAULT 15% APR.
        BigDecimal balance = new BigDecimal("2339.97");
        BigDecimal rate = new BigDecimal("15.00");
        BigDecimal exact = balance.multiply(rate).divide(BigDecimal.valueOf(1200));

        BigDecimal legacy = cobolTruncated(balance, rate);
        BigDecimal fixed = InterestCalculator.monthlyInterest(balance, rate);

        System.out.printf("2339.97 x 15.00 / 1200 = %s exact | COBOL (truncated) %s | Java (rounded) %s%n",
                exact.toPlainString(), legacy, fixed);
        assertEquals(new BigDecimal("29.249625"), exact);
        assertEquals(new BigDecimal("29.24"), legacy);
        assertEquals(new BigDecimal("29.25"), fixed);
    }
}
