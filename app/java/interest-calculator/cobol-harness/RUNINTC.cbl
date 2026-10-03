      ******************************************************************
      * Test harness only: stands in for the JCL EXEC PARM= by building
      * the EXTERNAL-PARMS block and calling CBACT04C.
      * Usage: RUNINTC <PARM-DATE>   (e.g. RUNINTC 2022071800)
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. RUNINTC.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  EXTERNAL-PARMS.
           05  PARM-LENGTH         PIC S9(04) COMP VALUE 10.
           05  PARM-DATE           PIC X(10).
       PROCEDURE DIVISION.
           ACCEPT PARM-DATE FROM COMMAND-LINE
           CALL 'CBACT04C' USING EXTERNAL-PARMS
           STOP RUN.
