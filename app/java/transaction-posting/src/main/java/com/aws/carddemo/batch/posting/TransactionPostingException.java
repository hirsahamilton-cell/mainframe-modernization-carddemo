package com.aws.carddemo.batch.posting;

/** A condition on which CBTRN02C abends (9999-ABEND-PROGRAM). */
public class TransactionPostingException extends RuntimeException {

    public TransactionPostingException(String message) {
        super(message);
    }
}
