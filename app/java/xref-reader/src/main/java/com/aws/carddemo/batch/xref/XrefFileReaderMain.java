package com.aws.carddemo.batch.xref;

import com.aws.carddemo.batch.xref.io.XrefFile;
import com.aws.carddemo.batch.xref.io.XrefFileException;
import com.aws.carddemo.batch.xref.io.XrefRecords;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class XrefFileReaderMain {
    private static final int ABEND_RETURN_CODE = 12;

    private XrefFileReaderMain() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out::println));
    }

    static int run(String[] args, Consumer<String> sysout) {
        if (args.length != 1) {
            System.err.println("usage: java -jar xref-reader.jar XREFFILE");
            return 8;
        }

        sysout.accept("START OF EXECUTION OF PROGRAM CBACT03C");
        try {
            for (var xref : XrefFile.load(Path.of(args[0]))) {
                sysout.accept(XrefRecords.format(xref));
            }
        } catch (XrefFileException e) {
            e.sysoutLines().forEach(sysout);
            return ABEND_RETURN_CODE;
        }
        sysout.accept("END OF EXECUTION OF PROGRAM CBACT03C");
        return 0;
    }
}
