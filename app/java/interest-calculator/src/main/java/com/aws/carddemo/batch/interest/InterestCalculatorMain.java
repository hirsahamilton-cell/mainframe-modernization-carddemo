package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.io.CardDemoRecords;
import com.aws.carddemo.batch.interest.model.Account;
import com.aws.carddemo.batch.interest.model.CardCrossReference;
import com.aws.carddemo.batch.interest.model.DisclosureGroup;
import com.aws.carddemo.batch.interest.model.TransactionCategoryBalance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Command-line entry point equivalent to the INTCALC job step. Reads the ASCII (line-per-record)
 * versions of the input files and writes the generated transactions and the updated account file.
 *
 * <pre>
 * java -jar interest-calculator.jar PARM-DATE TCATBAL XREF ACCTDATA DISCGRP OUT-TRANSACT OUT-ACCTDATA
 * </pre>
 */
public final class InterestCalculatorMain {

    private InterestCalculatorMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 7) {
            System.err.println("usage: InterestCalculatorMain PARM-DATE TCATBAL XREF ACCTDATA DISCGRP"
                    + " OUT-TRANSACT OUT-ACCTDATA");
            System.exit(8);
        }
        String parmDate = args[0];
        List<TransactionCategoryBalance> balances =
                CardDemoRecords.readAll(Path.of(args[1]), CardDemoRecords::parseCategoryBalance);
        List<CardCrossReference> xrefs = CardDemoRecords.readAll(Path.of(args[2]), CardDemoRecords::parseCrossReference);
        List<Account> accounts = CardDemoRecords.readAll(Path.of(args[3]), CardDemoRecords::parseAccount);
        List<DisclosureGroup> groups = CardDemoRecords.readAll(Path.of(args[4]), CardDemoRecords::parseDisclosureGroup);

        InterestCalculationJob.Result result = run(parmDate, Clock.systemDefaultZone(), balances, xrefs, accounts, groups);

        Files.write(Path.of(args[5]), result.transactions().stream().map(CardDemoRecords::formatTransaction).toList(),
                StandardCharsets.US_ASCII);

        Map<Long, Account> merged = accounts.stream()
                .collect(Collectors.toMap(Account::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        result.updatedAccounts().forEach(a -> merged.put(a.id(), a));
        Files.write(Path.of(args[6]), merged.values().stream().map(CardDemoRecords::formatAccount).toList(),
                StandardCharsets.US_ASCII);

        System.out.printf("CBACT04C (Java): %d category balances, %d transactions, %d accounts updated%n",
                balances.size(), result.transactions().size(), result.updatedAccounts().size());
    }

    static InterestCalculationJob.Result run(String parmDate, Clock clock,
                                             List<TransactionCategoryBalance> balances,
                                             List<CardCrossReference> xrefs,
                                             List<Account> accounts,
                                             List<DisclosureGroup> groups) {
        Map<Long, Account> accountsById = accounts.stream()
                .collect(Collectors.toMap(Account::id, Function.identity()));
        Map<Long, CardCrossReference> xrefByAccount = xrefs.stream()
                .collect(Collectors.toMap(CardCrossReference::accountId, Function.identity(), (first, dup) -> first));
        InterestCalculationJob job = new InterestCalculationJob(
                new InterestRateTable(groups), accountsById::get, xrefByAccount::get,
                new TransactionFactory(parmDate, clock));
        return job.run(balances);
    }
}
