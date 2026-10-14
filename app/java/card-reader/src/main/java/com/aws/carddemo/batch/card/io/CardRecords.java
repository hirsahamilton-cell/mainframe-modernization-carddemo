package com.aws.carddemo.batch.card.io;

import com.aws.carddemo.batch.card.model.Card;

import java.util.Locale;

public final class CardRecords {
    public static final int RECORD_LENGTH = 150;

    private CardRecords() {
    }

    public static Card parse(String line) {
        if (line.length() > RECORD_LENGTH) {
            throw new IllegalArgumentException("record is longer than 150 characters");
        }
        String record = line + " ".repeat(RECORD_LENGTH - line.length());
        String accountId = record.substring(16, 27);
        if (!accountId.matches("[0-9]{11}")) {
            throw new IllegalArgumentException("account id is not numeric");
        }
        String cvv = record.substring(27, 30);
        if (!cvv.matches("[0-9]{3}")) {
            throw new IllegalArgumentException("CVV is not numeric");
        }
        return new Card(
                record.substring(0, 16),
                Long.parseLong(accountId),
                Integer.parseInt(cvv),
                rightTrim(record.substring(30, 80)),
                record.substring(80, 90),
                record.substring(90, 91),
                record.substring(91, RECORD_LENGTH));
    }

    public static String format(Card card) {
        return field(card.cardNumber(), 16, "card number")
                + numeric(card.accountId(), 11, "account id")
                + numeric(card.cvv(), 3, "CVV")
                + field(card.embossedName(), 50, "embossed name")
                + field(card.expirationDate(), 10, "expiration date")
                + field(card.activeStatus(), 1, "active status")
                + field(card.reserved(), 59, "reserved");
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

    private static String rightTrim(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(0, end);
    }
}
