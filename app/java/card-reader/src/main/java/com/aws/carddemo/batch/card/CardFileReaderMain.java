package com.aws.carddemo.batch.card;

import com.aws.carddemo.batch.card.io.CardFile;
import com.aws.carddemo.batch.card.io.CardFileException;
import com.aws.carddemo.batch.card.io.CardRecords;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class CardFileReaderMain {
    private static final int ABEND_RETURN_CODE = 12;

    private CardFileReaderMain() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out::println));
    }

    static int run(String[] args, Consumer<String> sysout) {
        if (args.length != 1) {
            System.err.println("usage: java -jar card-reader.jar CARDFILE");
            return 8;
        }

        sysout.accept("START OF EXECUTION OF PROGRAM CBACT02C");
        try {
            for (var card : CardFile.load(Path.of(args[0]))) {
                sysout.accept(CardRecords.format(card));
            }
        } catch (CardFileException e) {
            e.sysoutLines().forEach(sysout);
            return ABEND_RETURN_CODE;
        }
        sysout.accept("END OF EXECUTION OF PROGRAM CBACT02C");
        return 0;
    }
}
