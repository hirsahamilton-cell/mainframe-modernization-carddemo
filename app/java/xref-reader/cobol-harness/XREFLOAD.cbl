       IDENTIFICATION DIVISION.
       PROGRAM-ID. XREFLOAD.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT INPUT-FILE ASSIGN TO "INFILE"
               ORGANIZATION IS LINE SEQUENTIAL
               FILE STATUS IS INPUT-STATUS.
           SELECT XREFFILE-FILE ASSIGN TO "XREFFILE"
               ORGANIZATION IS INDEXED
               ACCESS MODE IS RANDOM
               RECORD KEY IS XREF-CARD-NUM
               FILE STATUS IS XREFFILE-STATUS.
       DATA DIVISION.
       FILE SECTION.
       FD INPUT-FILE.
       01 INPUT-RECORD PIC X(50).
       FD XREFFILE-FILE.
       01 XREFFILE-RECORD.
          05 XREF-CARD-NUM PIC X(16).
          05 XREF-DATA PIC X(34).
       WORKING-STORAGE SECTION.
       01 INPUT-STATUS PIC XX.
       01 XREFFILE-STATUS PIC XX.
       PROCEDURE DIVISION.
           OPEN INPUT INPUT-FILE
           IF INPUT-STATUS NOT = "00"
               DISPLAY "ERROR OPENING INFILE, FILE STATUS IS: "
                   INPUT-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT XREFFILE-FILE
           IF XREFFILE-STATUS NOT = "00"
               DISPLAY "ERROR OPENING XREFFILE, FILE STATUS IS: "
                   XREFFILE-STATUS
               MOVE 12 TO RETURN-CODE
               CLOSE INPUT-FILE
               STOP RUN
           END-IF
           PERFORM UNTIL INPUT-STATUS = "10"
               READ INPUT-FILE
                   AT END CONTINUE
                   NOT AT END
                       MOVE INPUT-RECORD TO XREFFILE-RECORD
                       WRITE XREFFILE-RECORD
                       IF XREFFILE-STATUS NOT = "00"
                           DISPLAY "ERROR WRITING XREFFILE, "
                               "FILE STATUS IS: " XREFFILE-STATUS
                           MOVE 12 TO RETURN-CODE
                           CLOSE INPUT-FILE
                           CLOSE XREFFILE-FILE
                           STOP RUN
                       END-IF
               END-READ
           END-PERFORM
           CLOSE INPUT-FILE
           CLOSE XREFFILE-FILE
           MOVE 0 TO RETURN-CODE
           STOP RUN.
