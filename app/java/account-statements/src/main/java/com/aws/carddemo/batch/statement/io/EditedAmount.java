package com.aws.carddemo.batch.statement.io;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The COBOL numeric-edited pictures used on the statement: {@code 9(9).99-} for the current balance and
 * {@code Z(9).99-} for transaction amounts and the total. The sign is a trailing {@code '-'} for negative values
 * and a space otherwise. Values of 1,000,000,000.00 or more are rejected instead of losing their leading digits
 * as a COBOL MOVE would.
 */
public final class EditedAmount {

    public static final int WIDTH = 13;
    private static final BigDecimal LIMIT = new BigDecimal("1000000000");

    private EditedAmount() {
    }

    /** {@code 9(9).99-}: every digit printed, e.g. {@code 000001945.87 }. */
    public static String balance(BigDecimal value) {
        BigDecimal cents = checked(value, "9(9).99-");
        String digits = String.format("%011d", cents.abs().unscaledValue());
        return digits.substring(0, 9) + "." + digits.substring(9) + sign(cents);
    }

    /** {@code Z(9).99-}: leading zeros of the whole part become spaces, e.g. {@code       183.88 }. */
    public static String amount(BigDecimal value) {
        BigDecimal cents = checked(value, "Z(9).99-");
        String digits = String.format("%011d", cents.abs().unscaledValue());
        String units = digits.substring(0, 9).replaceFirst("^0+", "");
        return " ".repeat(9 - units.length()) + units + "." + digits.substring(9) + sign(cents);
    }

    public static boolean fits(BigDecimal value) {
        return value.abs().compareTo(LIMIT) < 0;
    }

    private static BigDecimal checked(BigDecimal value, String picture) {
        BigDecimal cents = value.setScale(2, RoundingMode.UNNECESSARY);
        if (!fits(cents)) {
            throw new IllegalArgumentException(value + " does not fit in " + picture);
        }
        return cents;
    }

    private static String sign(BigDecimal cents) {
        return cents.signum() < 0 ? "-" : " ";
    }
}
