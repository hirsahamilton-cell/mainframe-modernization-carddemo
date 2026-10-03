package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.model.Account;
import com.aws.carddemo.batch.interest.model.CardCrossReference;
import com.aws.carddemo.batch.interest.model.Transaction;
import com.aws.carddemo.batch.interest.model.TransactionCategoryBalance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Java port of CBACT04C. For each account present in the (account-ordered) transaction category
 * balance file it:
 * <ol>
 *   <li>looks up each category's annual rate (falling back to the DEFAULT disclosure group) and posts
 *       a monthly interest transaction for every non-zero rate;</li>
 *   <li>posts a single over-limit fee transaction if the account is over its credit limit;</li>
 *   <li>adds the interest and fee to the account balance and resets the cycle credit/debit totals.</li>
 * </ol>
 */
public final class InterestCalculationJob {

    private final InterestRateTable rateTable;
    private final LongFunction<Account> accountLookup;
    private final LongFunction<CardCrossReference> xrefByAccount;
    private final TransactionFactory transactionFactory;

    public InterestCalculationJob(InterestRateTable rateTable,
                                  LongFunction<Account> accountLookup,
                                  LongFunction<CardCrossReference> xrefByAccount,
                                  TransactionFactory transactionFactory) {
        this.rateTable = rateTable;
        this.accountLookup = accountLookup;
        this.xrefByAccount = xrefByAccount;
        this.transactionFactory = transactionFactory;
    }

    public record Result(List<Transaction> transactions, List<Account> updatedAccounts) {
    }

    public Result run(Iterable<TransactionCategoryBalance> balancesInAccountOrder) {
        Map<Long, List<TransactionCategoryBalance>> byAccount = new LinkedHashMap<>();
        Long previousAccount = null;
        for (TransactionCategoryBalance balance : balancesInAccountOrder) {
            long accountId = balance.accountId();
            if (previousAccount != null && accountId != previousAccount && byAccount.containsKey(accountId)) {
                throw new IllegalArgumentException("Category balances are not grouped by account at " + accountId);
            }
            byAccount.computeIfAbsent(accountId, id -> new ArrayList<>()).add(balance);
            previousAccount = accountId;
        }

        List<Transaction> transactions = new ArrayList<>();
        List<Account> updatedAccounts = new ArrayList<>();
        byAccount.forEach((accountId, balances) ->
                updatedAccounts.add(processAccount(accountId, balances, transactions)));
        return new Result(List.copyOf(transactions), List.copyOf(updatedAccounts));
    }

    private Account processAccount(long accountId, List<TransactionCategoryBalance> balances,
                                   List<Transaction> out) {
        Account account = require(accountLookup.apply(accountId), "Account not found: " + accountId);
        CardCrossReference xref = require(xrefByAccount.apply(accountId), "Card xref not found for account: " + accountId);

        BigDecimal charges = BigDecimal.ZERO.setScale(2);
        for (TransactionCategoryBalance balance : balances) {
            BigDecimal rate = rateTable.annualRateFor(account.groupId(), balance.typeCode(), balance.categoryCode());
            if (rate.signum() == 0) {
                continue;
            }
            BigDecimal interest = InterestCalculator.monthlyInterest(balance.balance(), rate);
            out.add(transactionFactory.interest(accountId, xref.cardNumber(), interest));
            charges = charges.add(interest);
        }

        Optional<BigDecimal> fee = OverLimitFeePolicy.feeFor(account);
        if (fee.isPresent()) {
            out.add(transactionFactory.overLimitFee(accountId, xref.cardNumber(), fee.get()));
            charges = charges.add(fee.get());
        }

        return account.withCyclePosted(charges);
    }

    private static <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
