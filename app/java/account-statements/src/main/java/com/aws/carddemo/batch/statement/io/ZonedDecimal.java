package com.aws.carddemo.batch.statement.io;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Decoder for COBOL signed zoned-decimal ({@code PIC S9(n)V9(m)} DISPLAY) fields as they appear in the
 * CardDemo ASCII sample files: the sign is overpunched on the last digit using the EBCDIC convention
 * ({@code '{'} / {@code 'A'..'I'} positive, {@code '}'} / {@code 'J'..'R'} negative).
 */
public final class ZonedDecimal {

    private static final String POSITIVE = "{ABCDEFGHI";
    private static final String NEGATIVE = "}JKLMNOPQR";

    private ZonedDecimal() {
    }

    public static BigDecimal decode(String field, int scale) {
        int last = field.length() - 1;
        char signChar = field.charAt(last);
        int digit;
        boolean negative = false;
        if (Character.isDigit(signChar)) {
            digit = signChar - '0';
        } else if (POSITIVE.indexOf(signChar) >= 0) {
            digit = POSITIVE.indexOf(signChar);
        } else if (NEGATIVE.indexOf(signChar) >= 0) {
            digit = NEGATIVE.indexOf(signChar);
            negative = true;
        } else {
            throw new IllegalArgumentException("Invalid zoned-decimal sign in '" + field + "'");
        }
        String digits = field.substring(0, last) + digit;
        if (!digits.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid zoned-decimal digits in '" + field + "'");
        }
        BigInteger unscaled = new BigInteger(digits);
        return new BigDecimal(negative ? unscaled.negate() : unscaled, scale);
    }
}
