package com.aws.carddemo.batch.statement;

import com.aws.carddemo.batch.statement.io.CardDemoRecords;
import com.aws.carddemo.batch.statement.model.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Command-line entry point equivalent to the CREASTMT job (SORT step + CBSTM03A / CBSTM03B). Reads the ASCII
 * (line-per-record) versions of the input files and writes the text statements (80-byte lines) and HTML
 * statements (100-byte lines). TRANSACT is the transaction master unloaded in any order. Exits with 12 on an
 * abend condition (a customer or account that is not on file, or an amount too large for the statement), in
 * which case neither output is written.
 *
 * <pre>
 * java -jar account-statements.jar TRANSACT XREF ACCTDATA CUSTDATA OUT-STMTFILE OUT-HTMLFILE
 * </pre>
 */
public final class StatementMain {

    static final int ABEND_RETURN_CODE = 12;

    private StatementMain() {
    }

    public static void main(String[] args) throws IOException {
        System.exit(run(args, System.out::println));
    }

    static int run(String[] args, Consumer<String> sysout) throws IOException {
        if (args.length != 6) {
            System.err.println("usage: StatementMain TRANSACT XREF ACCTDATA CUSTDATA OUT-STMTFILE OUT-HTMLFILE");
            return 8;
        }
        StatementJob.Output output;
        try {
            List<Transaction> transactions = CardDemoRecords.readAll(Path.of(args[0]),
                    CardDemoRecords::parseTransaction);
            StatementJob job = new StatementJob(
                    CardDemoRecords.readAll(Path.of(args[1]), CardDemoRecords::parseCrossReference),
                    CardDemoRecords.readAll(Path.of(args[3]), CardDemoRecords::parseCustomer),
                    CardDemoRecords.readAll(Path.of(args[2]), CardDemoRecords::parseAccount));
            output = job.run(transactions);
        } catch (StatementException e) {
            e.sysoutLines().forEach(sysout);
            sysout.accept("ABENDING PROGRAM");
            return ABEND_RETURN_CODE;
        }
        CardDemoRecords.write(Path.of(args[4]), output.statementLines());
        CardDemoRecords.write(Path.of(args[5]), output.htmlLines());
        return 0;
    }
}
