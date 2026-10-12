package com.aws.carddemo.batch.report;

import com.aws.carddemo.batch.report.model.CardCrossReference;
import com.aws.carddemo.batch.report.model.DateRange;
import com.aws.carddemo.batch.report.model.Transaction;
import com.aws.carddemo.batch.report.model.TransactionCategory;
import com.aws.carddemo.batch.report.model.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The TRANREPT job: the SORT step (keep transactions processed in the date range, ordered by card number)
 * followed by CBTRN03C, which prints one detail line per transaction with account, page and grand totals.
 */
public final class TransactionReportJob {

    private final Map<String, CardCrossReference> xrefsByCard;
    private final Map<String, TransactionType> typesByCode;
    private final Map<String, TransactionCategory> categoriesByKey;
    private final PageBreakRule pageBreakRule;

    public TransactionReportJob(List<CardCrossReference> xrefs, List<TransactionType> types,
                                List<TransactionCategory> categories) {
        this(xrefs, types, categories, PageBreakRule.EVERY_20_LINES);
    }

    TransactionReportJob(List<CardCrossReference> xrefs, List<TransactionType> types,
                         List<TransactionCategory> categories, PageBreakRule pageBreakRule) {
        this.xrefsByCard = index(xrefs, CardCrossReference::cardNumber);
        this.typesByCode = index(types, TransactionType::code);
        this.categoriesByKey = index(categories, TransactionCategory::key);
        this.pageBreakRule = pageBreakRule;
    }

    /** STEP05R SORT: INCLUDE the date range, SORT FIELDS=(TRAN-CARD-NUM,A); equal cards keep their order. */
    public static List<Transaction> select(List<Transaction> transactions, DateRange range) {
        return transactions.stream()
                .filter(t -> range.contains(t.processedDate()))
                .sorted(Comparator.comparing(Transaction::cardNumber))
                .toList();
    }

    /** Selects the transactions in {@code range} and returns the report lines (empty if none are selected). */
    public List<String> run(List<Transaction> transactions, DateRange range) {
        return new Report(range).write(select(transactions, range));
    }

    private final class Report {
        private final DateRange range;
        private final List<String> lines = new ArrayList<>();
        private long lineCounterAtLastBreak;
        private BigDecimal pageTotal = BigDecimal.ZERO;
        private BigDecimal accountTotal = BigDecimal.ZERO;
        private BigDecimal grandTotal = BigDecimal.ZERO;

        Report(DateRange range) {
            this.range = range;
        }

        List<String> write(List<Transaction> selected) {
            if (selected.isEmpty()) {
                return lines;
            }
            String currentCard = null;
            long accountId = 0;
            for (Transaction t : selected) {
                if (!t.cardNumber().equals(currentCard)) {
                    if (currentCard != null) {
                        writeAccountTotal();
                    }
                    currentCard = t.cardNumber();
                    accountId = lookup(xrefsByCard, currentCard, "INVALID CARD NUMBER : ").accountId();
                }
                String typeDescription = lookup(typesByCode, t.typeCode(), "INVALID TRANSACTION TYPE : ")
                        .description();
                String categoryDescription = lookup(categoriesByKey,
                        TransactionCategory.key(t.typeCode(), t.categoryCode()), "INVALID TRAN CATG KEY : ")
                        .description();
                writeDetail(t, accountId, typeDescription, categoryDescription);
            }
            writeAccountTotal();
            writePageTotal();
            add(ReportLines.grandTotal(grandTotal));
            return lines;
        }

        private void writeDetail(Transaction t, long accountId, String typeDescription,
                                 String categoryDescription) {
            if (lines.isEmpty()) {
                writeHeaders();
            } else if (pageBreakRule.newPageDue(lines.size(), lineCounterAtLastBreak)) {
                lineCounterAtLastBreak = lines.size();
                writePageTotal();
                writeHeaders();
            }
            pageTotal = pageTotal.add(t.amount());
            accountTotal = accountTotal.add(t.amount());
            add(ReportLines.detail(t, accountId, typeDescription, categoryDescription));
        }

        private void writeHeaders() {
            add(ReportLines.reportName(range));
            add(ReportLines.BLANK);
            add(ReportLines.COLUMN_HEADINGS);
            add(ReportLines.DASHES);
        }

        private void writePageTotal() {
            add(ReportLines.pageTotal(pageTotal));
            grandTotal = grandTotal.add(pageTotal);
            pageTotal = BigDecimal.ZERO;
            add(ReportLines.DASHES);
        }

        private void writeAccountTotal() {
            add(ReportLines.accountTotal(accountTotal));
            accountTotal = BigDecimal.ZERO;
            add(ReportLines.DASHES);
        }

        private void add(String line) {
            lines.add(line);
        }
    }

    private static <T> T lookup(Map<String, T> index, String key, String message) {
        T value = index.get(key);
        if (value == null) {
            throw new TransactionReportException(message + key);
        }
        return value;
    }

    private static <T> Map<String, T> index(List<T> records, Function<T, String> key) {
        return records.stream().collect(Collectors.toMap(key, Function.identity(), (first, second) -> first));
    }
}
