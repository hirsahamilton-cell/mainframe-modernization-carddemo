package com.aws.carddemo.batch.posting;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Z-GET-DB2-FORMAT-TIMESTAMP: {@code YYYY-MM-DD-HH.MM.SS.hh0000} (hundredths of a second, then zeros). */
final class ProcessingTimestamp {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS'0000'");

    private ProcessingTimestamp() {
    }

    static String now(Clock clock) {
        return LocalDateTime.now(clock).format(FORMAT);
    }
}
