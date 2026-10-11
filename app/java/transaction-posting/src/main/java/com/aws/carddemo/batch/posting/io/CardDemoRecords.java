package com.aws.carddemo.batch.posting.io;

import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.CardCrossReference;
import com.aws.carddemo.batch.posting.model.RejectedTransaction;
import com.aws.carddemo.batch.posting.model.Transaction;
import com.aws.carddemo.batch.posting.model.TransactionCategoryBalance;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Fixed-width parsers/formatters for the CardDemo ASCII record layouts used by CBTRN02C. */
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

    /** Formats every record, keeping trailing spaces so each line is exactly one fixed-width record. */
    public static <T> List<String> formatAll(List<T> records, Function<T, String> formatter) {
        return records.stream().map(formatter).toList();
    }

    /** Writes already-formatted files in place, so links and permissions on existing outputs are kept. */
    public static void writeAll(Map<Path, List<String>> files) throws IOException {
        for (Map.Entry<Path, List<String>> file : files.entrySet()) {
            Files.write(file.getKey(), file.getValue(), StandardCharsets.US_ASCII);
        }
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

    /** The 22-byte FILLER is zeros in the shipped file and in every record CBTRN02C writes back or creates. */
    public static String formatCategoryBalance(TransactionCategoryBalance b) {
        return b.key() + ZonedDecimal.encode(b.balance(), 9, 2) + "0".repeat(22);
    }

    /** CVACT03Y. */
    public static CardCrossReference parseCrossReference(String line) {
        Fields f = new Fields(line, 50);
        return new CardCrossReference(f.text(16), f.number(9), f.number(11));
    }

    /** CVTRA05Y / CVTRA06Y. */
    public static Transaction parseTransaction(String line) {
        Fields f = new Fields(line, 350);
        return new Transaction(f.text(16), f.text(2), (int) f.number(4), f.trimmed(10), f.trimmed(100),
                f.decimal(11, 2), f.number(9), f.trimmed(50), f.trimmed(50), f.trimmed(10),
                f.text(16), f.trimmed(26), f.trimmed(26));
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

    /** DALYREJS: the 350-byte daily transaction followed by the 80-byte validation trailer. */
    public static String formatRejectedTransaction(RejectedTransaction r) {
        return formatTransaction(r.transaction())
                + String.format("%04d", r.reason().code())
                + pad(r.reason().description(), 76);
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

        String trimmed(int width) {
            return text(width).stripTrailing();
        }

        long number(int width) {
            return Long.parseLong(text(width));
        }

        BigDecimal decimal(int width, int scale) {
            return ZonedDecimal.decode(text(width), scale);
        }
    }
}
