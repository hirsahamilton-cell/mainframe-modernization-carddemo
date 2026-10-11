package com.aws.carddemo.batch.posting.model;

/** A DALYREJS record: the daily transaction as read plus the validation trailer. */
public record RejectedTransaction(Transaction transaction, RejectReason reason) {
}
