package com.aws.carddemo.batch.report.model;

/** Card cross-reference record (copybook CVACT03Y, RECLN 50). */
public record CardCrossReference(String cardNumber, long customerId, long accountId) {
}
