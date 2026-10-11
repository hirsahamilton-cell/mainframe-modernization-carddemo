package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.io.CardDemoRecords;
import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.CardCrossReference;
import com.aws.carddemo.batch.posting.model.Transaction;
import com.aws.carddemo.batch.posting.model.TransactionCategoryBalance;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.function.Consumer;

/**
 * Command-line entry point equivalent to the POSTTRAN job step. Reads the ASCII (line-per-record) versions of
 * the input files and writes the posted transactions, the rejects and the updated account and category balance
 * files. Exits with 4 when transactions were rejected (like CBTRN02C) and 12 on an abend condition, in which
 * case no output file is written.
 *
 * <pre>
 * java -jar transaction-posting.jar DALYTRAN XREF ACCTDATA TCATBAL OUT-TRANSACT OUT-DALYREJS OUT-ACCTDATA OUT-TCATBAL
 * </pre>
 */
public final class TransactionPostingMain {

    static final int ABEND_RETURN_CODE = 12;

    private TransactionPostingMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 8) {
            System.err.println("usage: TransactionPostingMain DALYTRAN XREF ACCTDATA TCATBAL"
                    + " OUT-TRANSACT OUT-DALYREJS OUT-ACCTDATA OUT-TCATBAL");
            System.exit(8);
        }
        List<Transaction> daily = CardDemoRecords.readAll(Path.of(args[0]), CardDemoRecords::parseTransaction);
        List<CardCrossReference> xrefs = CardDemoRecords.readAll(Path.of(args[1]), CardDemoRecords::parseCrossReference);
        List<Account> accounts = CardDemoRecords.readAll(Path.of(args[2]), CardDemoRecords::parseAccount);
        List<TransactionCategoryBalance> balances =
                CardDemoRecords.readAll(Path.of(args[3]), CardDemoRecords::parseCategoryBalance);

        TransactionPostingJob.Result result;
        try {
            result = run(daily, xrefs, accounts, balances, new TransactionValidator(), Clock.systemDefaultZone(),
                    System.out::println);
        } catch (TransactionPostingException e) {
            System.out.println(e.getMessage());
            System.out.println("ABENDING PROGRAM");
            System.exit(ABEND_RETURN_CODE);
            return;
        }

        CardDemoRecords.writeAll(Path.of(args[4]), result.postedTransactions(), CardDemoRecords::formatTransaction);
        CardDemoRecords.writeAll(Path.of(args[5]), result.rejectedTransactions(),
                CardDemoRecords::formatRejectedTransaction);
        CardDemoRecords.writeAll(Path.of(args[6]), result.accounts(), CardDemoRecords::formatAccount);
        CardDemoRecords.writeAll(Path.of(args[7]), result.categoryBalances(), CardDemoRecords::formatCategoryBalance);
        if (result.returnCode() != 0) {
            System.exit(result.returnCode());
        }
    }

    /** Runs the job, writing the same SYSOUT lines as CBTRN02C to {@code sysout}. */
    static TransactionPostingJob.Result run(List<Transaction> daily, List<CardCrossReference> xrefs,
                                            List<Account> accounts, List<TransactionCategoryBalance> balances,
                                            TransactionValidator validator, Clock clock, Consumer<String> sysout) {
        sysout.accept("START OF EXECUTION OF PROGRAM CBTRN02C");
        TransactionPostingJob.Result result =
                new TransactionPostingJob(xrefs, accounts, balances, validator, clock, sysout).run(daily);
        sysout.accept(String.format("TRANSACTIONS PROCESSED :%09d", result.processedCount()));
        sysout.accept(String.format("TRANSACTIONS REJECTED  :%09d", result.rejectedTransactions().size()));
        sysout.accept("END OF EXECUTION OF PROGRAM CBTRN02C");
        return result;
    }
}
