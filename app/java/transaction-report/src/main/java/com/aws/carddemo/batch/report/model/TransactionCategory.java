package com.aws.carddemo.batch.report.model;

/** Transaction category record (copybook CVTRA04Y, RECLN 60), keyed by type code + category code. */
public record TransactionCategory(String typeCode, int categoryCode, String description) {

    public String key() {
        return key(typeCode, categoryCode);
    }

    public static String key(String typeCode, int categoryCode) {
        return typeCode + String.format("%04d", categoryCode);
    }
}
