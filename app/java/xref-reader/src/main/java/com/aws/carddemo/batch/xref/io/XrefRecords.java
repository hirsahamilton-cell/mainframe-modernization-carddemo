package com.aws.carddemo.batch.xref.io;

import com.aws.carddemo.batch.xref.model.CardCrossReference;

import java.util.Locale;

public final class XrefRecords {
    public static final int RECORD_LENGTH = 50;

    private XrefRecords() {
    }

    public static CardCrossReference parse(String line) {
        if (line.length() > RECORD_LENGTH) {
            throw new IllegalArgumentException("record is longer than 50 characters");
        }
        String record = line + " ".repeat(RECORD_LENGTH - line.length());
        return new CardCrossReference(
                record.substring(0, 16),
                parseDigits(record.substring(16, 25), "customer id"),
                parseDigits(record.substring(25, 36), "account id"),
                record.substring(36, RECORD_LENGTH));
    }

    public static String format(CardCrossReference xref) {
        return field(xref.cardNumber(), 16, "card number")
                + numeric(xref.customerId(), 9, "customer id")
                + numeric(xref.accountId(), 11, "account id")
                + field(xref.filler(), 14, "filler");
    }

    private static long parseDigits(String value, String name) {
        if (!value.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new IllegalArgumentException(name + " is not numeric");
        }
        return Long.parseLong(value);
    }

    private static String numeric(long value, int width, String name) {
        if (value < 0 || Long.toString(value).length() > width) {
            throw new IllegalArgumentException(name + " must fit in " + width + " digits");
        }
        return String.format(Locale.ROOT, "%0" + width + "d", value);
    }

    private static String field(String value, int width, String name) {
        if (value == null || value.length() > width) {
            throw new IllegalArgumentException(name + " must fit in " + width + " characters");
        }
        return value + " ".repeat(width - value.length());
    }
}
