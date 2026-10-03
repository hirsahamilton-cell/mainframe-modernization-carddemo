      ******************************************************************
      * Test harness only: converts CardDemo ASCII sample files into
      * GnuCOBOL indexed (VSAM KSDS stand-in) files and back.
      * Usage: CDLOADER <MODE>   with INFILE / OUTFILE env vars set.
      *   LOADACCT LOADTCAT LOADXREF LOADXRFA LOADDISC
      *   DUMPACCT DUMPTCAT
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CDLOADER.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT TEXT-IN ASSIGN TO INFILE
                  ORGANIZATION IS LINE SEQUENTIAL
                  FILE STATUS IS WS-STAT-IN.
           SELECT TEXT-OUT ASSIGN TO OUTFILE
                  ORGANIZATION IS LINE SEQUENTIAL
                  FILE STATUS IS WS-STAT-OUT.
           SELECT ACCT-KSDS ASSIGN TO ACCTFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS AK-ACCT-ID
                  FILE STATUS  IS WS-STAT-KSDS.
           SELECT TCAT-KSDS ASSIGN TO TCATBALF
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS TK-KEY
                  FILE STATUS  IS WS-STAT-KSDS.
           SELECT XREF-KSDS ASSIGN TO XREFFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS XK-CARD-NUM
                  FILE STATUS  IS WS-STAT-KSDS.
           SELECT XRFA-KSDS ASSIGN TO XREFFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS XA-CARD-NUM
                  ALTERNATE RECORD KEY IS XA-ACCT-ID
                  FILE STATUS  IS WS-STAT-KSDS.
           SELECT DISC-KSDS ASSIGN TO DISCGRP
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS DK-KEY
                  FILE STATUS  IS WS-STAT-KSDS.
       DATA DIVISION.
       FILE SECTION.
       FD  TEXT-IN.
       01  TEXT-IN-REC                  PIC X(300).
       FD  TEXT-OUT.
       01  TEXT-OUT-REC                 PIC X(300).
       FD  ACCT-KSDS.
       01  ACCT-KSDS-REC.
           05 AK-ACCT-ID                PIC 9(11).
           05 FILLER                    PIC X(289).
       FD  TCAT-KSDS.
       01  TCAT-KSDS-REC.
           05 TK-KEY                    PIC X(17).
           05 FILLER                    PIC X(33).
       FD  XREF-KSDS.
       01  XREF-KSDS-REC.
           05 XK-CARD-NUM               PIC X(16).
           05 FILLER                    PIC X(34).
       FD  XRFA-KSDS.
       01  XRFA-KSDS-REC.
           05 XA-CARD-NUM               PIC X(16).
           05 FILLER                    PIC X(09).
           05 XA-ACCT-ID                PIC 9(11).
           05 FILLER                    PIC X(14).
       FD  DISC-KSDS.
       01  DISC-KSDS-REC.
           05 DK-KEY                    PIC X(16).
           05 FILLER                    PIC X(34).
       WORKING-STORAGE SECTION.
       01  WS-MODE                      PIC X(08).
       01  WS-STAT-IN                   PIC XX.
       01  WS-STAT-OUT                  PIC XX.
       01  WS-STAT-KSDS                 PIC XX.
       01  WS-EOF                       PIC X VALUE 'N'.
       01  WS-COUNT                     PIC 9(6) VALUE 0.
       PROCEDURE DIVISION.
           ACCEPT WS-MODE FROM COMMAND-LINE
           EVALUATE WS-MODE
             WHEN 'LOADACCT'
               OPEN INPUT TEXT-IN OUTPUT ACCT-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE ACCT-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN ACCT-KSDS
             WHEN 'LOADTCAT'
               OPEN INPUT TEXT-IN OUTPUT TCAT-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE TCAT-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN TCAT-KSDS
             WHEN 'LOADXREF'
               OPEN INPUT TEXT-IN OUTPUT XREF-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE XREF-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN XREF-KSDS
             WHEN 'LOADXRFA'
               OPEN INPUT TEXT-IN OUTPUT XRFA-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE XRFA-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN XRFA-KSDS
             WHEN 'LOADDISC'
               OPEN INPUT TEXT-IN OUTPUT DISC-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE DISC-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN DISC-KSDS
             WHEN 'DUMPACCT'
               OPEN INPUT ACCT-KSDS OUTPUT TEXT-OUT
               PERFORM UNTIL WS-EOF = 'Y'
                 READ ACCT-KSDS
                   AT END MOVE 'Y' TO WS-EOF
                   NOT AT END
                     WRITE TEXT-OUT-REC FROM ACCT-KSDS-REC
                     ADD 1 TO WS-COUNT
                 END-READ
               END-PERFORM
               CLOSE ACCT-KSDS TEXT-OUT
             WHEN 'DUMPTCAT'
               OPEN INPUT TCAT-KSDS OUTPUT TEXT-OUT
               PERFORM UNTIL WS-EOF = 'Y'
                 READ TCAT-KSDS
                   AT END MOVE 'Y' TO WS-EOF
                   NOT AT END
                     MOVE SPACES TO TEXT-OUT-REC
                     MOVE TCAT-KSDS-REC TO TEXT-OUT-REC
                     WRITE TEXT-OUT-REC
                     ADD 1 TO WS-COUNT
                 END-READ
               END-PERFORM
               CLOSE TCAT-KSDS TEXT-OUT
             WHEN OTHER
               DISPLAY 'CDLOADER: UNKNOWN MODE ' WS-MODE
               MOVE 8 TO RETURN-CODE
               STOP RUN
           END-EVALUATE
           DISPLAY 'CDLOADER ' WS-MODE ' RECORDS: ' WS-COUNT
           STOP RUN.

       READ-IN.
           READ TEXT-IN
             AT END MOVE 'Y' TO WS-EOF
             NOT AT END
               INSPECT TEXT-IN-REC REPLACING ALL X'0D' BY SPACE
               ADD 1 TO WS-COUNT
           END-READ.

       CHECK-KSDS.
           IF WS-STAT-KSDS NOT = '00'
              DISPLAY 'CDLOADER: WRITE FAILED, STATUS ' WS-STAT-KSDS
                      ' MODE ' WS-MODE ' RECORD ' WS-COUNT
              MOVE 12 TO RETURN-CODE
              STOP RUN
           END-IF.
