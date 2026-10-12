package com.aws.carddemo.batch.report;

/** A condition on which CBTRN03C abends (CEE3ABD): the report is not written. */
public class TransactionReportException extends RuntimeException {

    public TransactionReportException(String message) {
        super(message);
    }
}
