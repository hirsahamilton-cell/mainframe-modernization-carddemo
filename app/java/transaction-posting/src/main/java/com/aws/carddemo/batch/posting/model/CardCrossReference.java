package com.aws.carddemo.batch.posting.model;

/** Card cross-reference record (copybook CVACT03Y, RECLN 50). */
public record CardCrossReference(String cardNumber, long customerId, long accountId) {
}
