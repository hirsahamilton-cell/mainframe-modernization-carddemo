package com.aws.carddemo.batch.posting;

import com.aws.carddemo.batch.posting.model.Account;
import com.aws.carddemo.batch.posting.model.CardCrossReference;
import com.aws.carddemo.batch.posting.model.RejectReason;
import com.aws.carddemo.batch.posting.model.RejectedTransaction;
import com.aws.carddemo.batch.posting.model.Transaction;
import com.aws.carddemo.batch.posting.model.TransactionCategoryBalance;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * POSTTRAN (CBTRN02C): validates each daily transaction, then posts it to the transaction category balance,
 * the account and the transaction master, or writes it to the rejects file.
 */
public final class TransactionPostingJob {

    /** Outputs in the order the indexed files would be read back (key order); rejects in input order. */
    public record Result(
            List<Transaction> postedTransactions,
            List<RejectedTransaction> rejectedTransactions,
            List<Account> accounts,
            List<TransactionCategoryBalance> categoryBalances) {

        public int processedCount() {
            return postedTransactions.size() + rejectedTransactions.size();
        }

        /** CBTRN02C sets RETURN-CODE 4 when any transaction was rejected. */
        public int returnCode() {
            return rejectedTransactions.isEmpty() ? 0 : 4;
        }
    }

    private final Map<String, CardCrossReference> xrefByCard = new HashMap<>();
    private final Map<Long, Account> accounts = new TreeMap<>();
    private final Map<String, TransactionCategoryBalance> balances = new TreeMap<>();
    private final TransactionValidator validator;
    private final Clock clock;
    private final Consumer<String> sysout;

    public TransactionPostingJob(List<CardCrossReference> xrefs, List<Account> accounts,
                                 List<TransactionCategoryBalance> categoryBalances,
                                 TransactionValidator validator, Clock clock, Consumer<String> sysout) {
        xrefs.forEach(x -> xrefByCard.putIfAbsent(x.cardNumber(), x));
        accounts.forEach(a -> this.accounts.put(a.id(), a));
        categoryBalances.forEach(b -> balances.put(b.key(), b));
        this.validator = validator;
        this.clock = clock;
        this.sysout = sysout;
    }

    public Result run(List<Transaction> dailyTransactions) {
        Map<String, Transaction> posted = new TreeMap<>();
        List<RejectedTransaction> rejected = new ArrayList<>();
        for (Transaction daily : dailyTransactions) {
            CardCrossReference xref = xrefByCard.get(daily.cardNumber());
            Optional<RejectReason> reason = validate(daily, xref);
            if (reason.isPresent()) {
                rejected.add(new RejectedTransaction(daily, reason.get()));
            } else {
                post(daily, xref.accountId(), posted);
            }
        }
        return new Result(List.copyOf(posted.values()), List.copyOf(rejected),
                List.copyOf(accounts.values()), List.copyOf(balances.values()));
    }

    private Optional<RejectReason> validate(Transaction daily, CardCrossReference xref) {
        if (xref == null) {
            return Optional.of(RejectReason.INVALID_CARD);
        }
        Account account = accounts.get(xref.accountId());
        if (account == null) {
            return Optional.of(RejectReason.ACCOUNT_NOT_FOUND);
        }
        return validator.validate(account, daily);
    }

    private void post(Transaction daily, long accountId, Map<String, Transaction> posted) {
        Transaction transaction = daily.withProcessedTimestamp(ProcessingTimestamp.now(clock));
        if (posted.containsKey(transaction.id())) {
            throw new TransactionPostingException("ERROR WRITING TO TRANSACTION FILE: duplicate transaction id "
                    + transaction.id());
        }
        updateCategoryBalance(accountId, transaction);
        accounts.computeIfPresent(accountId, (id, account) -> account.withPosted(transaction.amount()));
        posted.put(transaction.id(), transaction);
    }

    private void updateCategoryBalance(long accountId, Transaction transaction) {
        String key = TransactionCategoryBalance.key(accountId, transaction.typeCode(), transaction.categoryCode());
        TransactionCategoryBalance balance = balances.get(key);
        if (balance == null) {
            sysout.accept("TCATBAL record not found for key : " + key + ".. Creating.");
            balance = TransactionCategoryBalance.empty(accountId, transaction.typeCode(), transaction.categoryCode());
        }
        balances.put(key, balance.plus(transaction.amount()));
    }
}
