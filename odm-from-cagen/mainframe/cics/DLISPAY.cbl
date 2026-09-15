      *================================================================*
      * CICS Service Adapter: DLIS-PAYMENT-ADAPTER
      * Program ID: DLISPAY
      *
      * Purpose:
      *   Executes the transactional DB2 writes for payment processing
      *   AFTER ODM rule evaluation has confirmed all business rules pass.
      *
      * Invocation:
      *   Called by the Java ODM REST adapter via EXEC CICS LINK
      *   using the EXCI (External CICS Interface) bridge.
      *
      * Communication Area (COMMAREA) layout maps to PaymentCommArea.java
      *
      * Flow:
      *   1. Receive COMMAREA from ODM REST layer
      *   2. Validate COMMAREA version marker
      *   3. INSERT into PAYMENT table (DB2)
      *   4. UPDATE LICENSE-APPLICATION PAYMENT-STATUS, APPLICATION-STATUS (DB2)
      *   5. Return COMMAREA with new PAYMENT-ID, RECEIPT-NUMBER, return code
      *
      * Converted from: AB-PROCESS-PAYMENT.ACB (lines 84–109)
      * Table: DLIS.PAYMENT, DLIS.LICENSE_APPLICATION
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DLISPAY.
       AUTHOR. ODM-MIGRATION-TOOL.
       DATE-WRITTEN. 2025.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

      *----------------------------------------------------------------*
      * DB2 Host Variables — PAYMENT insert
      *----------------------------------------------------------------*
       01 WS-PAYMENT-INSERT.
          05 HV-APPLICATION-ID       PIC 9(10).
          05 HV-CANDIDATE-ID         PIC 9(10).
          05 HV-PAYMENT-DATE         PIC X(10).
          05 HV-PAYMENT-AMOUNT       PIC 9(10)V99 COMP-3.
          05 HV-PAYMENT-METHOD       PIC X(2).
          05 HV-PAYMENT-REFERENCE    PIC X(30).
          05 HV-PAYMENT-STATUS       PIC X(1).
          05 HV-LICENSE-TYPE         PIC X(1).
          05 HV-FEE-TYPE             PIC X(2).
          05 HV-RECEIPT-NUMBER       PIC X(20).
          05 HV-PROCESSED-BY         PIC X(20).

      *----------------------------------------------------------------*
      * DB2 Host Variables — LICENSE_APPLICATION update
      *----------------------------------------------------------------*
       01 WS-APP-UPDATE.
          05 HV-NEW-PAYMENT-STATUS   PIC X(1).
          05 HV-NEW-PAYMENT-REF      PIC X(30).
          05 HV-NEW-APP-STATUS       PIC X(2).
          05 HV-LAST-UPDATED-DATE    PIC X(10).
          05 HV-LAST-UPDATED-BY      PIC X(20).
          05 HV-APP-ID-KEY           PIC 9(10).

      *----------------------------------------------------------------*
      * SQLCA
      *----------------------------------------------------------------*
           EXEC SQL INCLUDE SQLCA END-EXEC.

      *----------------------------------------------------------------*
      * Return code work areas
      *----------------------------------------------------------------*
       01 WS-RETURN-CODE              PIC 9(4) COMP.
       01 WS-RETURN-MSG               PIC X(100).
       01 WS-NEW-PAYMENT-ID           PIC 9(10).

      *================================================================*
      * COMMAREA — must match PaymentCommArea.java (ODM REST layer)
      *================================================================*
       01 DFHCOMMAREA.
          05 CA-VERSION              PIC X(4).       *> '0001'
          05 CA-APPLICATION-ID       PIC 9(10).
          05 CA-CANDIDATE-ID         PIC 9(10).
          05 CA-PAYMENT-METHOD       PIC X(2).
          05 CA-PAYMENT-REFERENCE    PIC X(30).
          05 CA-PROCESSED-BY         PIC X(20).
          05 CA-LICENSE-TYPE         PIC X(1).
          05 CA-FEE-AMOUNT           PIC 9(10)V99 COMP-3.
          *> -- Outputs --
          05 CA-PAYMENT-ID           PIC 9(10).
          05 CA-RECEIPT-NUMBER       PIC X(20).
          05 CA-RETURN-CODE          PIC 9(4) COMP.
          05 CA-RETURN-MSG           PIC X(100).

       LINKAGE SECTION.

       PROCEDURE DIVISION.

      *================================================================*
       0000-MAIN.
      *================================================================*
           MOVE 0  TO WS-RETURN-CODE
           MOVE SPACES TO WS-RETURN-MSG

      *    Validate COMMAREA version
           IF CA-VERSION NOT = '0001'
              MOVE 99 TO CA-RETURN-CODE
              MOVE 'INVALID COMMAREA VERSION' TO CA-RETURN-MSG
              EXEC CICS RETURN END-EXEC
           END-IF

           PERFORM 1000-INSERT-PAYMENT
           IF WS-RETURN-CODE NOT = 0
              MOVE WS-RETURN-CODE TO CA-RETURN-CODE
              MOVE WS-RETURN-MSG  TO CA-RETURN-MSG
              EXEC CICS RETURN END-EXEC
           END-IF

           PERFORM 2000-UPDATE-APPLICATION

           MOVE WS-RETURN-CODE    TO CA-RETURN-CODE
           MOVE WS-RETURN-MSG     TO CA-RETURN-MSG
           EXEC CICS RETURN END-EXEC.

      *================================================================*
       1000-INSERT-PAYMENT.
      *================================================================*
      *    Build receipt number: RCP + today + application-id
           MOVE CA-APPLICATION-ID  TO HV-APPLICATION-ID
           MOVE CA-CANDIDATE-ID    TO HV-CANDIDATE-ID
           MOVE FUNCTION CURRENT-DATE (1:10) TO HV-PAYMENT-DATE
           MOVE CA-FEE-AMOUNT      TO HV-PAYMENT-AMOUNT
           MOVE CA-PAYMENT-METHOD  TO HV-PAYMENT-METHOD
           MOVE CA-PAYMENT-REFERENCE TO HV-PAYMENT-REFERENCE
           MOVE 'S'                TO HV-PAYMENT-STATUS
           MOVE CA-LICENSE-TYPE    TO HV-LICENSE-TYPE
           MOVE 'IF'               TO HV-FEE-TYPE
           MOVE CA-PROCESSED-BY    TO HV-PROCESSED-BY

           STRING 'RCP'
                  FUNCTION CURRENT-DATE (1:8)
                  CA-APPLICATION-ID
                  DELIMITED SIZE
                  INTO HV-RECEIPT-NUMBER

           EXEC SQL
               INSERT INTO DLIS.PAYMENT
               ( APPLICATION_ID, CANDIDATE_ID, PAYMENT_DATE,
                 PAYMENT_AMOUNT, PAYMENT_METHOD, PAYMENT_REFERENCE,
                 PAYMENT_STATUS, LICENSE_TYPE, FEE_TYPE,
                 RECEIPT_NUMBER, PROCESSED_BY )
               VALUES
               ( :HV-APPLICATION-ID, :HV-CANDIDATE-ID, :HV-PAYMENT-DATE,
                 :HV-PAYMENT-AMOUNT, :HV-PAYMENT-METHOD, :HV-PAYMENT-REFERENCE,
                 :HV-PAYMENT-STATUS, :HV-LICENSE-TYPE, :HV-FEE-TYPE,
                 :HV-RECEIPT-NUMBER, :HV-PROCESSED-BY )
           END-EXEC

           IF SQLCODE NOT = 0
               MOVE 99 TO WS-RETURN-CODE
               STRING 'DB2 INSERT PAYMENT FAILED SQLCODE='
                      SQLCODE DELIMITED SIZE
                      INTO WS-RETURN-MSG
               EXIT PARAGRAPH
           END-IF

      *    Retrieve generated PAYMENT-ID (identity column)
           EXEC SQL
               SELECT PAYMENT_ID INTO :WS-NEW-PAYMENT-ID
               FROM DLIS.PAYMENT
               WHERE RECEIPT_NUMBER = :HV-RECEIPT-NUMBER
           END-EXEC

           MOVE WS-NEW-PAYMENT-ID  TO CA-PAYMENT-ID
           MOVE HV-RECEIPT-NUMBER  TO CA-RECEIPT-NUMBER
           MOVE 0 TO WS-RETURN-CODE.

      *================================================================*
       2000-UPDATE-APPLICATION.
      *================================================================*
           MOVE 'P'                       TO HV-NEW-PAYMENT-STATUS
           MOVE CA-PAYMENT-REFERENCE      TO HV-NEW-PAYMENT-REF
           MOVE 'PA'                      TO HV-NEW-APP-STATUS
           MOVE FUNCTION CURRENT-DATE (1:10) TO HV-LAST-UPDATED-DATE
           MOVE CA-PROCESSED-BY           TO HV-LAST-UPDATED-BY
           MOVE CA-APPLICATION-ID         TO HV-APP-ID-KEY

           EXEC SQL
               UPDATE DLIS.LICENSE_APPLICATION
               SET    PAYMENT_STATUS     = :HV-NEW-PAYMENT-STATUS,
                      PAYMENT_REFERENCE  = :HV-NEW-PAYMENT-REF,
                      APPLICATION_STATUS = :HV-NEW-APP-STATUS,
                      LAST_UPDATED_DATE  = :HV-LAST-UPDATED-DATE,
                      LAST_UPDATED_BY    = :HV-LAST-UPDATED-BY
               WHERE  APPLICATION_ID     = :HV-APP-ID-KEY
           END-EXEC

           IF SQLCODE NOT = 0
               MOVE 99 TO WS-RETURN-CODE
               STRING 'DB2 UPDATE APPLICATION FAILED SQLCODE='
                      SQLCODE DELIMITED SIZE
                      INTO WS-RETURN-MSG
           ELSE
               MOVE 0 TO WS-RETURN-CODE
               MOVE 'PAYMENT RECORDED SUCCESSFULLY' TO WS-RETURN-MSG
           END-IF.

       END PROGRAM DLISPAY.
