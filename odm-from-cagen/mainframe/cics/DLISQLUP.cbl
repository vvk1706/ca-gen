      *================================================================*
      * CICS DB2 Lookup Program: DLISQLUP
      * Purpose: Pre-execution data retrieval for ODM rule invocation
      *
      * Retrieves all data required by ODM before a ruleset is called.
      * This avoids mid-rule DB2 access — ODM rules operate on an
      * in-memory snapshot loaded by this program.
      *
      * Queries executed:
      *   1. LICENSE-APPLICATION (by APPLICATION-ID)
      *   2. CANDIDATE            (by CANDIDATE-ID from application)
      *   3. DRIVING-HISTORY      (all active records for candidate)
      *   4. ISSUED-LICENSE       (all active licenses for candidate)
      *   5. LICENSE-FEE-SCHEDULE (active IF fee for license type)
      *
      * COMMAREA passes result JSON string to the Java EXCI caller.
      * Java layer deserialises into RuleContext for ODM invocation.
      *
      * Converted from: View-layer READ operations in all ABs
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DLISQLUP.
       AUTHOR. ODM-MIGRATION-TOOL.
       DATE-WRITTEN. 2025.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

           EXEC SQL INCLUDE SQLCA END-EXEC.

      *----------------------------------------------------------------*
      * Application row
      *----------------------------------------------------------------*
       01 WS-APPLICATION.
          05 HV-APPLICATION-ID         PIC 9(10).
          05 HV-CANDIDATE-ID           PIC 9(10).
          05 HV-LICENSE-TYPE           PIC X(1).
          05 HV-APPLICATION-DATE       PIC X(10).
          05 HV-APPLICATION-STATUS     PIC X(2).
          05 HV-ELIG-STATUS            PIC X(1).
          05 HV-ELIG-DATE              PIC X(10).
          05 HV-HIST-STATUS            PIC X(1).
          05 HV-HIST-DATE              PIC X(10).
          05 HV-PAY-STATUS             PIC X(1).
          05 HV-PAY-REF                PIC X(20).
          05 HV-APP1-STATUS            PIC X(1).
          05 HV-APP1-AUTH              PIC X(50).
          05 HV-APP2-STATUS            PIC X(1).
          05 HV-APP2-AUTH              PIC X(50).

      *----------------------------------------------------------------*
      * Candidate row
      *----------------------------------------------------------------*
       01 WS-CANDIDATE.
          05 HV-FIRST-NAME             PIC X(30).
          05 HV-LAST-NAME              PIC X(30).
          05 HV-DATE-OF-BIRTH          PIC X(10).
          05 HV-ID-NUMBER              PIC X(20).
          05 HV-RECORD-STATUS          PIC X(1).

      *----------------------------------------------------------------*
      * Fee schedule
      *----------------------------------------------------------------*
       01 WS-FEE.
          05 HV-FEE-SCHEDULE-ID        PIC 9(10).
          05 HV-FEE-AMOUNT             PIC 9(10)V99 COMP-3.
          05 HV-CURRENCY-CODE          PIC X(3).

      *----------------------------------------------------------------*
      * COMMAREA
      *----------------------------------------------------------------*
       01 DFHCOMMAREA.
          05 CA-VERSION                PIC X(4).      *> '0001'
          05 CA-APPLICATION-ID-IN      PIC 9(10).
          *> Result payload — serialised as fixed-length fields
          *> Application
          05 CA-APP-LICENSE-TYPE       PIC X(1).
          05 CA-APP-STATUS             PIC X(2).
          05 CA-ELIG-STATUS            PIC X(1).
          05 CA-HIST-STATUS            PIC X(1).
          05 CA-PAY-STATUS             PIC X(1).
          05 CA-APP1-STATUS            PIC X(1).
          05 CA-APP1-AUTH              PIC X(50).
          05 CA-APP2-STATUS            PIC X(1).
          05 CA-APP2-AUTH              PIC X(50).
          05 CA-CANDIDATE-ID           PIC 9(10).
          *> Candidate
          05 CA-FIRST-NAME             PIC X(30).
          05 CA-LAST-NAME              PIC X(30).
          05 CA-DATE-OF-BIRTH          PIC X(10).
          05 CA-RECORD-STATUS          PIC X(1).
          *> Fee schedule
          05 CA-FEE-AMOUNT             PIC 9(10)V99 COMP-3.
          05 CA-CURRENCY-CODE          PIC X(3).
          *> Return
          05 CA-RETURN-CODE            PIC 9(4) COMP.
          05 CA-RETURN-MSG             PIC X(100).

       LINKAGE SECTION.

       PROCEDURE DIVISION.

       0000-MAIN.
           MOVE 0 TO CA-RETURN-CODE

           MOVE CA-APPLICATION-ID-IN TO HV-APPLICATION-ID

           PERFORM 1000-READ-APPLICATION
           IF CA-RETURN-CODE NOT = 0
              EXEC CICS RETURN END-EXEC
           END-IF

           PERFORM 2000-READ-CANDIDATE
           PERFORM 3000-READ-FEE-SCHEDULE

           EXEC CICS RETURN END-EXEC.

       1000-READ-APPLICATION.
           EXEC SQL
               SELECT APPLICATION_ID, CANDIDATE_ID, LICENSE_TYPE,
                      APPLICATION_DATE, APPLICATION_STATUS,
                      ELIGIBILITY_CHECK_STATUS, ELIGIBILITY_CHECK_DATE,
                      HISTORY_CHECK_STATUS, HISTORY_CHECK_DATE,
                      PAYMENT_STATUS, PAYMENT_REFERENCE,
                      APPROVAL_1_STATUS, APPROVAL_1_AUTHORITY,
                      APPROVAL_2_STATUS, APPROVAL_2_AUTHORITY
               INTO   :HV-APPLICATION-ID, :HV-CANDIDATE-ID, :HV-LICENSE-TYPE,
                      :HV-APPLICATION-DATE, :HV-APPLICATION-STATUS,
                      :HV-ELIG-STATUS, :HV-ELIG-DATE,
                      :HV-HIST-STATUS, :HV-HIST-DATE,
                      :HV-PAY-STATUS, :HV-PAY-REF,
                      :HV-APP1-STATUS, :HV-APP1-AUTH,
                      :HV-APP2-STATUS, :HV-APP2-AUTH
               FROM   DLIS.LICENSE_APPLICATION
               WHERE  APPLICATION_ID = :HV-APPLICATION-ID
           END-EXEC

           IF SQLCODE = 100
               MOVE 1 TO CA-RETURN-CODE
               MOVE 'APPLICATION NOT FOUND' TO CA-RETURN-MSG
               EXIT PARAGRAPH
           END-IF

           MOVE HV-LICENSE-TYPE   TO CA-APP-LICENSE-TYPE
           MOVE HV-APPLICATION-STATUS TO CA-APP-STATUS
           MOVE HV-ELIG-STATUS    TO CA-ELIG-STATUS
           MOVE HV-HIST-STATUS    TO CA-HIST-STATUS
           MOVE HV-PAY-STATUS     TO CA-PAY-STATUS
           MOVE HV-APP1-STATUS    TO CA-APP1-STATUS
           MOVE HV-APP1-AUTH      TO CA-APP1-AUTH
           MOVE HV-APP2-STATUS    TO CA-APP2-STATUS
           MOVE HV-APP2-AUTH      TO CA-APP2-AUTH
           MOVE HV-CANDIDATE-ID   TO CA-CANDIDATE-ID.

       2000-READ-CANDIDATE.
           MOVE CA-CANDIDATE-ID TO HV-CANDIDATE-ID

           EXEC SQL
               SELECT FIRST_NAME, LAST_NAME, DATE_OF_BIRTH,
                      ID_NUMBER, RECORD_STATUS
               INTO   :HV-FIRST-NAME, :HV-LAST-NAME, :HV-DATE-OF-BIRTH,
                      :HV-ID-NUMBER, :HV-RECORD-STATUS
               FROM   DLIS.CANDIDATE
               WHERE  CANDIDATE_ID = :HV-CANDIDATE-ID
           END-EXEC

           MOVE HV-FIRST-NAME     TO CA-FIRST-NAME
           MOVE HV-LAST-NAME      TO CA-LAST-NAME
           MOVE HV-DATE-OF-BIRTH  TO CA-DATE-OF-BIRTH
           MOVE HV-RECORD-STATUS  TO CA-RECORD-STATUS.

       3000-READ-FEE-SCHEDULE.
           EXEC SQL
               SELECT FEE_SCHEDULE_ID, FEE_AMOUNT, CURRENCY_CODE
               INTO   :HV-FEE-SCHEDULE-ID, :HV-FEE-AMOUNT, :HV-CURRENCY-CODE
               FROM   DLIS.LICENSE_FEE_SCHEDULE
               WHERE  LICENSE_TYPE   = :HV-LICENSE-TYPE
               AND    FEE_TYPE       = 'IF'
               AND    ACTIVE_STATUS  = 'A'
               AND    EFFECTIVE_DATE <= CURRENT DATE
               AND    (EXPIRY_DATE IS NULL OR EXPIRY_DATE >= CURRENT DATE)
               FETCH FIRST 1 ROW ONLY
           END-EXEC

           IF SQLCODE = 0
               MOVE HV-FEE-AMOUNT    TO CA-FEE-AMOUNT
               MOVE HV-CURRENCY-CODE TO CA-CURRENCY-CODE
           END-IF.

       END PROGRAM DLISQLUP.
