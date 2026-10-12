package com.aws.carddemo.batch.statement.io;

import com.aws.carddemo.batch.statement.model.Account;
import com.aws.carddemo.batch.statement.model.CardCrossReference;
import com.aws.carddemo.batch.statement.model.Customer;
import com.aws.carddemo.batch.statement.model.Transaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/** Fixed-width parsers for the CardDemo ASCII record layouts read by CREASTMT. */
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

    /** Writes already-formatted lines in place, so links and permissions on an existing output are kept. */
    public static void write(Path file, List<String> lines) throws IOException {
        Files.write(file, lines, StandardCharsets.US_ASCII);
    }

    /** CVTRA05Y. */
    public static Transaction parseTransaction(String line) {
        Fields f = new Fields(line, 350);
        String id = f.text(16);
        f.skip(2 + 4 + 10);
        String description = f.trimmed(100);
        BigDecimal amount = f.decimal(11, 2);
        f.skip(9 + 50 + 50 + 10);
        return new Transaction(id, description, amount, f.text(16));
    }

    /** CVACT03Y. */
    public static CardCrossReference parseCrossReference(String line) {
        Fields f = new Fields(line, 50);
        return new CardCrossReference(f.text(16), f.number(9), f.number(11));
    }

    /** CVACT01Y. */
    public static Account parseAccount(String line) {
        Fields f = new Fields(line, 300);
        long id = f.number(11);
        f.skip(1);
        return new Account(id, f.decimal(12, 2));
    }

    /** CUSTREC. */
    public static Customer parseCustomer(String line) {
        Fields f = new Fields(line, 500);
        long id = f.number(9);
        String first = f.trimmed(25);
        String middle = f.trimmed(25);
        String last = f.trimmed(25);
        String address1 = f.trimmed(50);
        String address2 = f.trimmed(50);
        String address3 = f.trimmed(50);
        String state = f.trimmed(2);
        String country = f.trimmed(3);
        String zip = f.trimmed(10);
        f.skip(15 + 15 + 9 + 20 + 10 + 10 + 1);
        return new Customer(id, first, middle, last, address1, address2, address3, state, country, zip,
                f.text(3));
    }

    /** Left-justify and space-fill or truncate, like a COBOL MOVE to PIC X(n). */
    public static String pad(String value, int width) {
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

        void skip(int width) {
            pos += width;
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
