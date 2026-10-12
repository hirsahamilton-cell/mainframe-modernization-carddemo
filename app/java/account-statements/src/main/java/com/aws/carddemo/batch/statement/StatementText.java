package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.io.EditedAmount;
import com.aws.carddemo.batch.statement.model.Customer;
import com.aws.carddemo.batch.statement.model.Statement;
import com.aws.carddemo.batch.statement.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.aws.carddemo.batch.statement.io.CardDemoRecords.pad;

/** The 80-byte text statement (CBSTM03A STATEMENT-LINES, ST-LINE0 to ST-LINE15). */
public final class StatementText {

    public static final int WIDTH = 80;
    static final String DASHES = "-".repeat(WIDTH);

    private StatementText() {
    }

    public static List<String> lines(Statement s) {
        List<String> lines = new ArrayList<>();
        lines.add("*".repeat(31) + "START OF STATEMENT" + "*".repeat(31));
        lines.add(pad(name(s.customer()), 75));
        lines.add(pad(s.customer().addressLine1(), 50));
        lines.add(pad(s.customer().addressLine2(), 50));
        lines.add(pad(addressLine3(s.customer()), 80));
        lines.add(DASHES);
        lines.add(" ".repeat(33) + "Basic Details");
        lines.add(DASHES);
        lines.add("Account ID         :" + accountId(s));
        lines.add("Current Balance    :" + EditedAmount.balance(s.account().currentBalance()));
        lines.add("FICO Score         :" + s.customer().ficoScore());
        lines.add(DASHES);
        lines.add(" ".repeat(30) + "TRANSACTION SUMMARY");
        lines.add(DASHES);
        lines.add("Tran ID         " + pad("Tran Details", 51) + "  Tran Amount");
        lines.add(DASHES);
        for (Transaction t : s.transactions()) {
            lines.add(pad(t.id(), 16) + " " + pad(t.description(), 49) + "$" + EditedAmount.amount(t.amount()));
        }
        lines.add(DASHES);
        lines.add(pad("Total EXP:", 66) + "$" + EditedAmount.amount(s.total()));
        lines.add("*".repeat(32) + "END OF STATEMENT" + "*".repeat(32));
        return lines.stream().map(line -> pad(line, WIDTH)).toList();
    }

    /** ST-NAME: first, middle and last name separated by single spaces; blank parts are left out. */
    static String name(Customer c) {
        return joinNonBlank(c.firstName(), c.middleName(), c.lastName());
    }

    /** ST-ADD3: city, state, country and ZIP code separated by single spaces; blank parts are left out. */
    static String addressLine3(Customer c) {
        return joinNonBlank(c.addressLine3(), c.stateCode(), c.countryCode(), c.zip());
    }

    /** ST-ACCT-ID: the 11-digit account number in a 20-character field. */
    static String accountId(Statement s) {
        return pad(String.format("%011d", s.account().id()), 20);
    }

    private static String joinNonBlank(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).map(String::strip).filter(p -> !p.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
