      ******************************************************************
      * Test harness only: converts CardDemo ASCII sample files into
      * GnuCOBOL indexed (VSAM KSDS stand-in) files and back.
      * Usage: CDLOADER <MODE>   with INFILE / OUTFILE env vars set.
      *   LOADACCT LOADTCAT LOADXREF LOADXRFA LOADDISC LOADTTYP LOADTCTG
      *   LOADCUST LOADTRXF
      *   DUMPACCT DUMPTCAT DUMPTRAN
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
           SELECT TEXT-OUT-TRAN ASSIGN TO OUTFILE
                  ORGANIZATION IS LINE SEQUENTIAL
                  FILE STATUS IS WS-STAT-OUT.
           SELECT CUST-KSDS ASSIGN TO CUSTFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS CK-CUST-ID
                  FILE STATUS  IS WS-STAT-KSDS.
           SELECT TRXF-KSDS ASSIGN TO TRNXFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS FK-TRNX-KEY
                  FILE STATUS  IS WS-STAT-KSDS.
           SELECT TRAN-KSDS ASSIGN TO TRANFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS RK-TRAN-ID
                  FILE STATUS  IS WS-STAT-KSDS.
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
           SELECT TTYP-KSDS ASSIGN TO TRANTYPE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS YK-TYPE
                  FILE STATUS  IS WS-STAT-KSDS.
           SELECT TCTG-KSDS ASSIGN TO TRANCATG
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS GK-KEY
                  FILE STATUS  IS WS-STAT-KSDS.
       DATA DIVISION.
       FILE SECTION.
       FD  TEXT-IN.
       01  TEXT-IN-REC                  PIC X(500).
       FD  TEXT-OUT.
       01  TEXT-OUT-REC                 PIC X(300).
       FD  TEXT-OUT-TRAN.
       01  TEXT-OUT-TRAN-REC            PIC X(350).
       FD  CUST-KSDS.
       01  CUST-KSDS-REC.
           05 CK-CUST-ID                PIC X(09).
           05 FILLER                    PIC X(491).
       FD  TRXF-KSDS.
       01  TRXF-KSDS-REC.
           05 FK-TRNX-KEY               PIC X(32).
           05 FILLER                    PIC X(318).
       FD  TRAN-KSDS.
       01  TRAN-KSDS-REC.
           05 RK-TRAN-ID                PIC X(16).
           05 FILLER                    PIC X(334).
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
       FD  TTYP-KSDS.
       01  TTYP-KSDS-REC.
           05 YK-TYPE                   PIC X(02).
           05 FILLER                    PIC X(58).
       FD  TCTG-KSDS.
       01  TCTG-KSDS-REC.
           05 GK-KEY                    PIC X(06).
           05 FILLER                    PIC X(54).
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
             WHEN 'LOADTTYP'
               OPEN INPUT TEXT-IN OUTPUT TTYP-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE TTYP-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN TTYP-KSDS
             WHEN 'LOADTCTG'
               OPEN INPUT TEXT-IN OUTPUT TCTG-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE TCTG-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN TCTG-KSDS
             WHEN 'LOADCUST'
               OPEN INPUT TEXT-IN OUTPUT CUST-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE CUST-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN CUST-KSDS
             WHEN 'LOADTRXF'
               OPEN INPUT TEXT-IN OUTPUT TRXF-KSDS
               PERFORM READ-IN
               PERFORM UNTIL WS-EOF = 'Y'
                 WRITE TRXF-KSDS-REC FROM TEXT-IN-REC
                 PERFORM CHECK-KSDS
                 PERFORM READ-IN
               END-PERFORM
               CLOSE TEXT-IN TRXF-KSDS
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
             WHEN 'DUMPTRAN'
               OPEN INPUT TRAN-KSDS OUTPUT TEXT-OUT-TRAN
               PERFORM UNTIL WS-EOF = 'Y'
                 READ TRAN-KSDS
                   AT END MOVE 'Y' TO WS-EOF
                   NOT AT END
                     WRITE TEXT-OUT-TRAN-REC FROM TRAN-KSDS-REC
                     ADD 1 TO WS-COUNT
                 END-READ
               END-PERFORM
               CLOSE TRAN-KSDS TEXT-OUT-TRAN
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
