package com.aws.carddemo.batch.card.io;

import java.util.List;

public final class CardFileException extends Exception {
    private final List<String> sysoutLines;

    public CardFileException(List<String> sysoutLines, Throwable cause) {
        super(String.join(System.lineSeparator(), sysoutLines), cause);
        this.sysoutLines = List.copyOf(sysoutLines);
    }

    public List<String> sysoutLines() {
        return sysoutLines;
    }
}
