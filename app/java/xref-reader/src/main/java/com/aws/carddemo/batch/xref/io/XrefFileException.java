package com.aws.carddemo.batch.xref.io;

import java.util.List;

public final class XrefFileException extends Exception {
    private final List<String> sysoutLines;

    public XrefFileException(List<String> sysoutLines, Throwable cause) {
        super(String.join(System.lineSeparator(), sysoutLines), cause);
        this.sysoutLines = List.copyOf(sysoutLines);
    }

    public List<String> sysoutLines() {
        return sysoutLines;
    }
}
