package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.io.EditedAmount;
import com.aws.carddemo.batch.statement.model.Statement;
import com.aws.carddemo.batch.statement.model.Transaction;

import java.util.ArrayList;
import java.util.List;

import static com.aws.carddemo.batch.statement.io.CardDemoRecords.pad;

/**
 * The HTML statement (CBSTM03A HTML-LINES), one 100-byte line per record. Like CBSTM03A, every statement is a
 * complete HTML document and the documents are written one after another. Customer and transaction text is
 * HTML-escaped; a value whose escaped line would not fit in 100 bytes loses characters from its end.
 */
public final class StatementHtml {

    public static final int WIDTH = 100;

    private static final String TR = "<tr>";
    private static final String TR_END = "</tr>";
    private static final String TD_END = "</td>";
    private static final String TITLE_CELL = "<td colspan=\"3\" style=\"padding:0px 5px;background-color:#1d1d96b3;\">";
    private static final String BANK_CELL = "<td colspan=\"3\" style=\"padding:0px 5px;background-color:#FFAF33;\">";
    private static final String DETAIL_CELL = "<td colspan=\"3\" style=\"padding:0px 5px;background-color:#f2f2f2;\">";
    private static final String SECTION_CELL = "<td colspan=\"3\" style=\"padding:0px 5px;"
            + "background-color:#33FFD1; text-align:center;\">";
    private static final String BIG = "<p style=\"font-size:16px\">";

    private StatementHtml() {
    }

    public static List<String> lines(Statement s) {
        List<String> lines = new ArrayList<>();
        // 5100-WRITE-HTML-HEADER
        lines.addAll(List.of("<!DOCTYPE html>", "<html lang=\"en\">", "<head>", "<meta charset=\"utf-8\">",
                "<title>HTML Table Layout</title>", "</head>", "<body style=\"margin:0px;\">",
                "<table  align=\"center\" frame=\"box\" style=\"width:70%; font:12px Segoe UI,sans-serif;\">",
                TR, TITLE_CELL,
                "<h3>Statement for Account Number: " + StatementText.accountId(s) + "</h3>",
                TD_END, TR_END, TR, BANK_CELL,
                BIG + "Bank of XYZ</p>", "<p>410 Terry Ave N</p>", "<p>Seattle WA 99999</p>",
                TD_END, TR_END, TR, DETAIL_CELL));
        // 5200-WRITE-HTML-NMADBS
        lines.add(paragraph(BIG, pad(StatementText.name(s.customer()), 50).stripTrailing(), "  </p>"));
        lines.add(paragraph("<p>", s.customer().addressLine1(), "  </p>"));
        lines.add(paragraph("<p>", s.customer().addressLine2(), "  </p>"));
        lines.add(paragraph("<p>", pad(StatementText.addressLine3(s.customer()), 80).stripTrailing(), "  </p>"));
        lines.addAll(List.of(TD_END, TR_END, TR, SECTION_CELL, BIG + "Basic Details</p>", TD_END, TR_END,
                TR, DETAIL_CELL,
                "<p>Account ID         : " + StatementText.accountId(s) + "</p>",
                "<p>Current Balance    : " + EditedAmount.balance(s.account().currentBalance()) + "</p>",
                paragraph("<p>FICO Score         : ", pad(s.customer().ficoScore(), 20), "</p>"),
                TD_END, TR_END, TR, SECTION_CELL, BIG + "Transaction Summary</p>", TD_END, TR_END,
                TR, column(25, "#33FF5E", "left"), BIG + "Tran ID</p>", TD_END,
                column(55, "#33FF5E", "left"), BIG + "Tran Details</p>", TD_END,
                column(20, "#33FF5E", "right"), BIG + "Amount</p>", TD_END, TR_END));
        // 6000-WRITE-TRANS
        for (Transaction t : s.transactions()) {
            lines.addAll(List.of(TR, column(25, "#f2f2f2", "left"), paragraph("<p>", pad(t.id(), 16), "</p>"), TD_END,
                    column(55, "#f2f2f2", "left"), paragraph("<p>", pad(t.description(), 49), "</p>"), TD_END,
                    column(20, "#f2f2f2", "right"), "<p>" + EditedAmount.amount(t.amount()) + "</p>", TD_END,
                    TR_END));
        }
        // end of 4000-TRNXFILE-GET
        lines.addAll(List.of(TR, TITLE_CELL, "<h3>End of Statement</h3>", TD_END, TR_END, "</table>", "</body>",
                "</html>"));
        return lines.stream().map(line -> pad(line, WIDTH)).toList();
    }

    private static String column(int widthPercent, String color, String align) {
        return "<td style=\"width:" + widthPercent + "%; padding:0px 5px; background-color:" + color
                + "; text-align:" + align + ";\">";
    }

    /** {@code prefix + escape(text) + suffix}, dropping characters from the end of text until it fits. */
    static String paragraph(String prefix, String text, String suffix) {
        String value = text;
        String line = prefix + escape(value) + suffix;
        while (line.length() > WIDTH) {
            value = value.substring(0, value.length() - 1);
            line = prefix + escape(value) + suffix;
        }
        return line;
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
