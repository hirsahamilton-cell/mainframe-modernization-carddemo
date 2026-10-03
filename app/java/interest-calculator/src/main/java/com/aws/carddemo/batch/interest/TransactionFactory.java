package com.aws.carddemo.batch.interest;

import com.aws.carddemo.batch.interest.model.Transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds system-generated transactions in the shape of 1300-B-WRITE-TX: the id is the job's
 * PARM date followed by a 6-digit sequence, source is {@code System}, merchant fields are blank,
 * and both timestamps carry the DB2-format processing time.
 */
public final class TransactionFactory {

    public static final String INTEREST_TYPE_CODE = "01";
    public static final int INTEREST_CATEGORY_CODE = 5;
    public static final String FEE_TYPE_CODE = "01";
    public static final int OVER_LIMIT_FEE_CATEGORY_CODE = 6;
    static final String SOURCE = "System";

    private static final DateTimeFormatter DB2_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS'0000'");

    private final String parmDate;
    private final Clock clock;
    private int sequence;

    public TransactionFactory(String parmDate, Clock clock) {
        if (parmDate.length() != 10) {
            throw new IllegalArgumentException("PARM date must be 10 characters, got '" + parmDate + "'");
        }
        this.parmDate = parmDate;
        this.clock = clock;
    }

    public Transaction interest(long accountId, String cardNumber, BigDecimal amount) {
        return build(INTEREST_TYPE_CODE, INTEREST_CATEGORY_CODE,
                "Int. for a/c " + formatAccountId(accountId), amount, cardNumber);
    }

    public Transaction overLimitFee(long accountId, String cardNumber, BigDecimal amount) {
        return build(FEE_TYPE_CODE, OVER_LIMIT_FEE_CATEGORY_CODE,
                "Over-limit fee for a/c " + formatAccountId(accountId), amount, cardNumber);
    }

    private Transaction build(String typeCode, int categoryCode, String description, BigDecimal amount,
                              String cardNumber) {
        sequence++;
        if (sequence > 999_999) {
            throw new IllegalStateException("Transaction id sequence exhausted for PARM date " + parmDate);
        }
        String id = parmDate + String.format("%06d", sequence);
        String timestamp = LocalDateTime.now(clock).format(DB2_TIMESTAMP);
        return new Transaction(id, typeCode, categoryCode, SOURCE, description, amount,
                0L, "", "", "", cardNumber, timestamp, timestamp);
    }

    private static String formatAccountId(long accountId) {
        return String.format("%011d", accountId);
    }
}
