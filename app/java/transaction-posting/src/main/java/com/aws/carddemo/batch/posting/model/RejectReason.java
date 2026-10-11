package com.aws.carddemo.batch.posting.model;

/** Validation failures written to the DALYREJS trailer (WS-VALIDATION-FAIL-REASON / -DESC). */
public enum RejectReason {
    INVALID_CARD(100, "INVALID CARD NUMBER FOUND"),
    ACCOUNT_NOT_FOUND(101, "ACCOUNT RECORD NOT FOUND"),
    OVER_LIMIT(102, "OVERLIMIT TRANSACTION"),
    AFTER_EXPIRATION(103, "TRANSACTION RECEIVED AFTER ACCT EXPIRATION");

    private final int code;
    private final String description;

    RejectReason(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int code() {
        return code;
    }

    public String description() {
        return description;
    }
}
