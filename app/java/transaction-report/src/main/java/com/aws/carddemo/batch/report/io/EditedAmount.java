package com.aws.carddemo.batch.report.io;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * COBOL numeric-edited amounts used by the report (copybook CVTRA07Y): {@code -ZZZ,ZZZ,ZZZ.ZZ} for detail
 * lines and {@code +ZZZ,ZZZ,ZZZ.ZZ} for totals. Leading zeros and the commas before them become spaces, the
 * sign is a single fixed character in front, and a zero value prints as 15 spaces (every digit is a Z).
 */
public final class EditedAmount {

    public static final int WIDTH = 15;
    private static final BigDecimal LIMIT = new BigDecimal("1000000000");

    private EditedAmount() {
    }

    /** {@code -ZZZ,ZZZ,ZZZ.ZZ}: a minus sign for negative amounts, otherwise a space. */
    public static String detail(BigDecimal amount) {
        return format(amount, ' ');
    }

    /** {@code +ZZZ,ZZZ,ZZZ.ZZ}: a plus sign for positive amounts, a minus sign for negative ones. */
    public static String total(BigDecimal amount) {
        return format(amount, '+');
    }

    private static String format(BigDecimal amount, char positiveSign) {
        BigDecimal cents = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (cents.abs().compareTo(LIMIT) >= 0) {
            throw new IllegalArgumentException(amount + " does not fit in ZZZ,ZZZ,ZZZ.ZZ");
        }
        if (cents.signum() == 0) {
            return " ".repeat(WIDTH);
        }
        BigDecimal abs = cents.abs();
        long units = abs.toBigInteger().longValueExact();
        String integerPart = units == 0 ? "" : String.format("%,d", units);
        String fraction = abs.toPlainString().substring(abs.toPlainString().indexOf('.'));
        char sign = cents.signum() < 0 ? '-' : positiveSign;
        return sign + " ".repeat(11 - integerPart.length()) + integerPart + fraction;
    }
}
