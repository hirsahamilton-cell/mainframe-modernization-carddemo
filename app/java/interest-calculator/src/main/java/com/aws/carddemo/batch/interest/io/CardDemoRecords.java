package com.aws.carddemo.batch.interest.io;

import com.aws.carddemo.batch.interest.model.Account;
import com.aws.carddemo.batch.interest.model.CardCrossReference;
import com.aws.carddemo.batch.interest.model.DisclosureGroup;
import com.aws.carddemo.batch.interest.model.Transaction;
import com.aws.carddemo.batch.interest.model.TransactionCategoryBalance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/** Fixed-width parsers/formatters for the CardDemo ASCII record layouts used by CBACT04C. */
public final class CardDemoRecords {

    private CardDemoRecords() {
    }

    public static <T> List<T> readAll(Path file, Function<String, T> parser) throws IOException {
        return Files.readAllLines(file, StandardCharsets.US_ASCII).stream()
                .map(line -> line.replace("\r", ""))
                .filter(line -> !line.isBlank())
                .map(parser)
                .toList();
    }

    /** CVACT01Y. */
    public static Account parseAccount(String line) {
        Fields f = new Fields(line, 300);
        return new Account(
                f.number(11), f.text(1), f.decimal(12, 2), f.decimal(12, 2), f.decimal(12, 2),
                f.text(10), f.text(10), f.text(10), f.decimal(12, 2), f.decimal(12, 2),
                f.text(10), f.text(10));
    }

    public static String formatAccount(Account a) {
        return String.format("%011d", a.id())
                + pad(a.activeStatus(), 1)
                + ZonedDecimal.encode(a.currentBalance(), 10, 2)
                + ZonedDecimal.encode(a.creditLimit(), 10, 2)
                + ZonedDecimal.encode(a.cashCreditLimit(), 10, 2)
                + pad(a.openDate(), 10) + pad(a.expirationDate(), 10) + pad(a.reissueDate(), 10)
                + ZonedDecimal.encode(a.currentCycleCredit(), 10, 2)
                + ZonedDecimal.encode(a.currentCycleDebit(), 10, 2)
                + pad(a.addressZip(), 10) + pad(a.groupId(), 10)
                + " ".repeat(178);
    }

    /** CVTRA01Y. */
    public static TransactionCategoryBalance parseCategoryBalance(String line) {
        Fields f = new Fields(line, 50);
        return new TransactionCategoryBalance(f.number(11), f.text(2), (int) f.number(4), f.decimal(11, 2));
    }

    /** CVTRA02Y. */
    public static DisclosureGroup parseDisclosureGroup(String line) {
        Fields f = new Fields(line, 50);
        return new DisclosureGroup(f.text(10), f.text(2), (int) f.number(4), f.decimal(6, 2));
    }

    /** CVACT03Y. */
    public static CardCrossReference parseCrossReference(String line) {
        Fields f = new Fields(line, 50);
        return new CardCrossReference(f.text(16), f.number(9), f.number(11));
    }

    /** CVTRA05Y. */
    public static Transaction parseTransaction(String line) {
        Fields f = new Fields(line, 350);
        return new Transaction(f.text(16), f.text(2), (int) f.number(4), f.text(10).strip(), f.text(100).strip(),
                f.decimal(11, 2), f.number(9), f.text(50).strip(), f.text(50).strip(), f.text(10).strip(),
                f.text(16), f.text(26), f.text(26));
    }

    public static String formatTransaction(Transaction t) {
        return pad(t.id(), 16)
                + pad(t.typeCode(), 2)
                + String.format("%04d", t.categoryCode())
                + pad(t.source(), 10)
                + pad(t.description(), 100)
                + ZonedDecimal.encode(t.amount(), 9, 2)
                + String.format("%09d", t.merchantId())
                + pad(t.merchantName(), 50) + pad(t.merchantCity(), 50) + pad(t.merchantZip(), 10)
                + pad(t.cardNumber(), 16) + pad(t.originTimestamp(), 26) + pad(t.processedTimestamp(), 26)
                + " ".repeat(20);
    }

    /** Left-justify and space-fill or truncate, like a COBOL MOVE to PIC X(n). */
    static String pad(String value, int width) {
        return value.length() >= width ? value.substring(0, width) : value + " ".repeat(width - value.length());
    }

    private static final class Fields {
        private final String line;
        private int pos;

        Fields(String line, int recordLength) {
            this.line = pad(line, recordLength);
        }

        String text(int width) {
            String s = line.substring(pos, pos + width);
            pos += width;
            return s;
        }

        long number(int width) {
            return Long.parseLong(text(width));
        }

        java.math.BigDecimal decimal(int width, int scale) {
            return ZonedDecimal.decode(text(width), scale);
        }
    }
}
