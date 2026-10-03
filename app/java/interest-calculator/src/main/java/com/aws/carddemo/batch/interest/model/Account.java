package com.aws.carddemo.batch.interest.model;

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

    public boolean isOverCreditLimit() {
        return currentBalance.compareTo(creditLimit) > 0;
    }

    /** Mirrors 1050-UPDATE-ACCOUNT: post charges to the balance and reset the cycle totals. */
    public Account withCyclePosted(BigDecimal charges) {
        return new Account(id, activeStatus, currentBalance.add(charges), creditLimit, cashCreditLimit,
                openDate, expirationDate, reissueDate, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                addressZip, groupId);
    }
}
