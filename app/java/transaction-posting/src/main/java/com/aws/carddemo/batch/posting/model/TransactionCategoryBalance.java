package com.aws.carddemo.batch.posting.model;

import java.math.BigDecimal;

/** Transaction category balance record (copybook CVTRA01Y, RECLN 50). */
public record TransactionCategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal balance) {

    public static TransactionCategoryBalance empty(long accountId, String typeCode, int categoryCode) {
        return new TransactionCategoryBalance(accountId, typeCode, categoryCode, BigDecimal.ZERO.setScale(2));
    }

    /** The 17-byte record key TRANCAT-ACCT-ID + TRANCAT-TYPE-CD + TRANCAT-CD, as displayed by COBOL. */
    public String key() {
        return key(accountId, typeCode, categoryCode);
    }

    public static String key(long accountId, String typeCode, int categoryCode) {
        return String.format("%011d%-2s%04d", accountId, typeCode, categoryCode);
    }

    public TransactionCategoryBalance plus(BigDecimal amount) {
        return new TransactionCategoryBalance(accountId, typeCode, categoryCode, balance.add(amount));
    }
}
