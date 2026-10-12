package com.aws.carddemo.batch.report;

import com.aws.carddemo.batch.report.io.CardDemoRecords;
import com.aws.carddemo.batch.report.model.DateRange;
import com.aws.carddemo.batch.report.model.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Command-line entry point equivalent to the TRANREPT job (SORT step + CBTRN03C). Reads the ASCII
 * (line-per-record) versions of the input files and writes the report, one 133-byte line per record.
 * TRANSACT is the transaction master unloaded in any order. Exits with 12 on an abend condition (a card,
 * transaction type or category that is not on file), in which case no report is written.
 *
 * <pre>
 * java -jar transaction-report.jar TRANSACT XREF TRANTYPE TRANCATG DATEPARM OUT-TRANREPT
 * </pre>
 */
public final class TransactionReportMain {

    static final int ABEND_RETURN_CODE = 12;

    private TransactionReportMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 6) {
            System.err.println("usage: TransactionReportMain TRANSACT XREF TRANTYPE TRANCATG DATEPARM OUT-TRANREPT");
            System.exit(8);
        }
        int returnCode = run(args, System.out::println);
        if (returnCode != 0) {
            System.exit(returnCode);
        }
    }

    static int run(String[] args, Consumer<String> sysout) throws IOException {
        TransactionReportJob job = new TransactionReportJob(
                CardDemoRecords.readAll(Path.of(args[1]), CardDemoRecords::parseCrossReference),
                CardDemoRecords.readAll(Path.of(args[2]), CardDemoRecords::parseTransactionType),
                CardDemoRecords.readAll(Path.of(args[3]), CardDemoRecords::parseTransactionCategory));
        Optional<DateRange> range = CardDemoRecords.readDateRange(Path.of(args[4]));
        List<String> report;
        try {
            report = run(job, CardDemoRecords.readAll(Path.of(args[0]), CardDemoRecords::parseTransaction),
                    range, sysout);
        } catch (TransactionReportException | IllegalArgumentException e) {
            sysout.accept(e.getMessage());
            sysout.accept("ABENDING PROGRAM");
            return ABEND_RETURN_CODE;
        }
        CardDemoRecords.write(Path.of(args[5]), report);
        return 0;
    }

    /** Runs the report, writing the same progress lines as CBTRN03C to {@code sysout}. */
    static List<String> run(TransactionReportJob job, List<Transaction> transactions,
                            Optional<DateRange> range, Consumer<String> sysout) {
        sysout.accept("START OF EXECUTION OF PROGRAM CBTRN03C");
        List<String> report = List.of();
        if (range.isPresent()) {
            sysout.accept("Reporting from " + range.get().start() + " to " + range.get().end());
            report = job.run(transactions, range.get());
        }
        sysout.accept("END OF EXECUTION OF PROGRAM CBTRN03C");
        return report;
    }
}
