package com.aws.carddemo.batch.report;

import com.aws.carddemo.batch.report.io.EditedAmount;
import com.aws.carddemo.batch.report.model.DateRange;
import com.aws.carddemo.batch.report.model.Transaction;

import java.math.BigDecimal;

import static com.aws.carddemo.batch.report.io.CardDemoRecords.pad;

/** The 133-byte TRANREPT lines (copybook CVTRA07Y). */
final class ReportLines {

    static final int WIDTH = 133;
    static final String DASHES = "-".repeat(WIDTH);
    static final String BLANK = " ".repeat(WIDTH);
    static final String COLUMN_HEADINGS = line(pad("Transaction ID", 17) + pad("Account ID", 12)
            + pad("Transaction Type", 19) + pad("Tran Category", 35) + pad("Tran Source", 14) + " "
            + pad("        Amount", 16));

    private ReportLines() {
    }

    static String reportName(DateRange range) {
        return line(pad("DALYREPT", 38) + pad("Daily Transaction Report", 41) + "Date Range: "
                + pad(range.start(), 10) + " to " + pad(range.end(), 10));
    }

    static String detail(Transaction t, long accountId, String typeDescription, String categoryDescription) {
        return line(pad(t.id(), 16) + " "
                + String.format("%011d", accountId) + " "
                + pad(t.typeCode(), 2) + "-" + pad(typeDescription, 15) + " "
                + String.format("%04d", t.categoryCode()) + "-" + pad(categoryDescription, 29) + " "
                + pad(t.source(), 10) + "    "
                + EditedAmount.detail(t.amount()) + "  ");
    }

    static String pageTotal(BigDecimal total) {
        return line(pad("Page Total", 11) + ".".repeat(86) + EditedAmount.total(total));
    }

    static String accountTotal(BigDecimal total) {
        return line(pad("Account Total", 13) + ".".repeat(84) + EditedAmount.total(total));
    }

    static String grandTotal(BigDecimal total) {
        return line(pad("Grand Total", 11) + ".".repeat(86) + EditedAmount.total(total));
    }

    private static String line(String text) {
        return pad(text, WIDTH);
    }
}
