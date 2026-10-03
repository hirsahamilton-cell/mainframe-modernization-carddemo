package com.aws.carddemo.batch.interest.model;

import java.math.BigDecimal;

/** Transaction category balance record (copybook CVTRA01Y, RECLN 50). */
public record TransactionCategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal balance) {
}
