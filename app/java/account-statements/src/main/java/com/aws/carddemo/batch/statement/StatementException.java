package com.aws.carddemo.batch.statement;

import java.util.List;

/** A condition on which CBSTM03A abends (CEE3ABD): no statement file is written. */
public class StatementException extends RuntimeException {

    private final List<String> sysoutLines;

    public StatementException(String... sysoutLines) {
        super(String.join(" / ", sysoutLines));
        this.sysoutLines = List.of(sysoutLines);
    }

    /** The lines CBSTM03A DISPLAYs before ABENDING PROGRAM. */
    public List<String> sysoutLines() {
        return sysoutLines;
    }
}
