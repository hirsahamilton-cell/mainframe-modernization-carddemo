package com.aws.carddemo.batch.posting.model;

import java.math.BigDecimal;

/** Account master record (copybook CVACT01Y, RECLN 300). */
public record Account(
        long id,
        String activeStatus,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        String openDate,
        String expirationDate,
        String reissueDate,
        BigDecimal currentCycleCredit,
        BigDecimal currentCycleDebit,
        String addressZip,
        String groupId) {

    /**
     * Net amount charged this cycle. Purchases are accumulated in the cycle credit and refunds (negative
     * amounts) in the cycle debit, so the two are added, not subtracted.
     */
    public BigDecimal currentCycleNet() {
        return currentCycleCredit.add(currentCycleDebit);
    }

    /** Mirrors 2800-UPDATE-ACCOUNT-REC. */
    public Account withPosted(BigDecimal amount) {
        boolean credit = amount.signum() >= 0;
        return new Account(id, activeStatus, currentBalance.add(amount), creditLimit, cashCreditLimit,
                openDate, expirationDate, reissueDate,
                credit ? currentCycleCredit.add(amount) : currentCycleCredit,
                credit ? currentCycleDebit : currentCycleDebit.add(amount),
                addressZip, groupId);
    }
}
