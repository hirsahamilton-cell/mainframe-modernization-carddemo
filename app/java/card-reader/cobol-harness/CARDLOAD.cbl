       IDENTIFICATION DIVISION.
       PROGRAM-ID. CARDLOAD.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT INPUT-FILE ASSIGN TO "INFILE"
               ORGANIZATION IS LINE SEQUENTIAL
               FILE STATUS IS INPUT-STATUS.
           SELECT CARDFILE-FILE ASSIGN TO "CARDFILE"
               ORGANIZATION IS INDEXED
               ACCESS MODE IS RANDOM
               RECORD KEY IS CARD-NUMBER
               FILE STATUS IS CARDFILE-STATUS.
       DATA DIVISION.
       FILE SECTION.
       FD INPUT-FILE.
       01 INPUT-RECORD PIC X(150).
       FD CARDFILE-FILE.
       01 CARDFILE-RECORD.
          05 CARD-NUMBER PIC X(16).
          05 CARD-DATA PIC X(134).
       WORKING-STORAGE SECTION.
       01 INPUT-STATUS PIC XX.
       01 CARDFILE-STATUS PIC XX.
       PROCEDURE DIVISION.
           OPEN INPUT INPUT-FILE
           IF INPUT-STATUS NOT = "00"
               DISPLAY "ERROR OPENING INFILE, FILE STATUS IS: "
                   INPUT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT CARDFILE-FILE
           IF CARDFILE-STATUS NOT = "00"
               DISPLAY "ERROR OPENING CARDFILE, FILE STATUS IS: "
                   CARDFILE-STATUS
               MOVE 12 TO RETURN-CODE
               CLOSE INPUT-FILE
               STOP RUN
           END-IF
           PERFORM UNTIL INPUT-STATUS = "10"
               READ INPUT-FILE
                   AT END CONTINUE
                   NOT AT END
                       MOVE INPUT-RECORD TO CARDFILE-RECORD
                       WRITE CARDFILE-RECORD
                       IF CARDFILE-STATUS NOT = "00"
                           DISPLAY "ERROR WRITING CARDFILE, "
                               "FILE STATUS IS: " CARDFILE-STATUS
                           MOVE 12 TO RETURN-CODE
                           CLOSE INPUT-FILE
                           CLOSE CARDFILE-FILE
                           STOP RUN
                       END-IF
               END-READ
           END-PERFORM
           CLOSE INPUT-FILE
           CLOSE CARDFILE-FILE
           MOVE 0 TO RETURN-CODE
           STOP RUN.
