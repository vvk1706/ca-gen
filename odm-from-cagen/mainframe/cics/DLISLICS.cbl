      *================================================================*
      * CICS Service Adapter: DLIS-LICENSE-ADAPTER
      * Program ID: DLISLICS
      *
      * Purpose:
      *   Executes the transactional DB2 writes for license issuance
      *   AFTER ODM rule evaluation has confirmed all business rules pass.
      *
      * Invocation:
      *   Called by the Java ODM REST adapter via EXEC CICS LINK
      *   using the EXCI (External CICS Interface) bridge.
      *
      * COMMAREA layout maps to LicenseIssuanceCommArea.java
      *
      * Flow:
      *   1. Receive COMMAREA (license number, expiry, vehicle class, etc.)
      *   2. Validate COMMAREA version marker
      *   3. INSERT into ISSUED_LICENSE table (DB2)
      *   4. UPDATE LICENSE_APPLICATION APPLICATION-STATUS = 'IS' (DB2)
      *   5. Return COMMAREA with new LICENSE-ID and return code
      *
      * Converted from: AB-ISSUE-LICENSE.ACB (lines 125–148)
      * Tables: DLIS.ISSUED_LICENSE, DLIS.LICENSE_APPLICATION
      *================================================================*
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DLISLICS.
       AUTHOR. ODM-MIGRATION-TOOL.
       DATE-WRITTEN. 2025.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

      *----------------------------------------------------------------*
      * DB2 Host Variables — ISSUED_LICENSE insert
      *----------------------------------------------------------------*
       01 WS-LICENSE-INSERT.
          05 HV-APPLICATION-ID       PIC 9(10).
          05 HV-CANDIDATE-ID         PIC 9(10).
          05 HV-LICENSE-NUMBER       PIC X(20).
          05 HV-LICENSE-TYPE         PIC X(1).
          05 HV-ISSUE-DATE           PIC X(10).
          05 HV-EXPIRY-DATE          PIC X(10).
          05 HV-LICENSE-STATUS       PIC X(1).
          05 HV-VEHICLE-CLASS        PIC X(2).
          05 HV-RESTRICTIONS         PIC X(200).
          05 HV-DEMERIT-BALANCE      PIC 9(3) COMP.
          05 HV-ISSUED-BY-AUTHORITY  PIC X(50).
          05 HV-ISSUED-BY-OFFICER    PIC X(50).

      *----------------------------------------------------------------*
      * DB2 Host Variables — APPLICATION update
      *----------------------------------------------------------------*
       01 WS-APP-UPDATE.
          05 HV-NEW-APP-STATUS       PIC X(2).
          05 HV-LAST-UPDATED-DATE    PIC X(10).
          05 HV-LAST-UPDATED-BY      PIC X(50).
          05 HV-APP-ID-KEY           PIC 9(10).

           EXEC SQL INCLUDE SQLCA END-EXEC.

       01 WS-RETURN-CODE              PIC 9(4) COMP.
       01 WS-RETURN-MSG               PIC X(100).
       01 WS-NEW-LICENSE-ID           PIC 9(10).

      *================================================================*
      * COMMAREA — must match LicenseIssuanceCommArea.java
      *================================================================*
       01 DFHCOMMAREA.
          05 CA-VERSION              PIC X(4).       *> '0001'
          05 CA-APPLICATION-ID       PIC 9(10).
          05 CA-CANDIDATE-ID         PIC 9(10).
          05 CA-LICENSE-NUMBER       PIC X(20).
          05 CA-LICENSE-TYPE         PIC X(1).
          05 CA-ISSUE-DATE           PIC X(10).
          05 CA-EXPIRY-DATE          PIC X(10).
          05 CA-VEHICLE-CLASS        PIC X(2).
          05 CA-RESTRICTIONS         PIC X(200).
          05 CA-DEMERIT-BALANCE      PIC 9(3) COMP.
          05 CA-ISSUED-BY-AUTHORITY  PIC X(50).
          05 CA-ISSUED-BY-OFFICER    PIC X(50).
          *> -- Outputs --
          05 CA-LICENSE-ID           PIC 9(10).
          05 CA-RETURN-CODE          PIC 9(4) COMP.
          05 CA-RETURN-MSG           PIC X(100).

       LINKAGE SECTION.

       PROCEDURE DIVISION.

      *================================================================*
       0000-MAIN.
      *================================================================*
           MOVE 0 TO WS-RETURN-CODE
           MOVE SPACES TO WS-RETURN-MSG

           IF CA-VERSION NOT = '0001'
              MOVE 99 TO CA-RETURN-CODE
              MOVE 'INVALID COMMAREA VERSION' TO CA-RETURN-MSG
              EXEC CICS RETURN END-EXEC
           END-IF

           PERFORM 1000-INSERT-LICENSE
           IF WS-RETURN-CODE NOT = 0
              MOVE WS-RETURN-CODE TO CA-RETURN-CODE
              MOVE WS-RETURN-MSG  TO CA-RETURN-MSG
              EXEC CICS RETURN END-EXEC
           END-IF

           PERFORM 2000-UPDATE-APPLICATION

           MOVE WS-RETURN-CODE TO CA-RETURN-CODE
           MOVE WS-RETURN-MSG  TO CA-RETURN-MSG
           EXEC CICS RETURN END-EXEC.

      *================================================================*
       1000-INSERT-LICENSE.
      *================================================================*
           MOVE CA-APPLICATION-ID      TO HV-APPLICATION-ID
           MOVE CA-CANDIDATE-ID        TO HV-CANDIDATE-ID
           MOVE CA-LICENSE-NUMBER      TO HV-LICENSE-NUMBER
           MOVE CA-LICENSE-TYPE        TO HV-LICENSE-TYPE
           MOVE CA-ISSUE-DATE          TO HV-ISSUE-DATE
           MOVE CA-EXPIRY-DATE         TO HV-EXPIRY-DATE
           MOVE 'A'                    TO HV-LICENSE-STATUS
           MOVE CA-VEHICLE-CLASS       TO HV-VEHICLE-CLASS
           MOVE CA-RESTRICTIONS        TO HV-RESTRICTIONS
           MOVE 12                     TO HV-DEMERIT-BALANCE
           MOVE CA-ISSUED-BY-AUTHORITY TO HV-ISSUED-BY-AUTHORITY
           MOVE CA-ISSUED-BY-OFFICER   TO HV-ISSUED-BY-OFFICER

           EXEC SQL
               INSERT INTO DLIS.ISSUED_LICENSE
               ( APPLICATION_ID, CANDIDATE_ID, LICENSE_NUMBER,
                 LICENSE_TYPE, ISSUE_DATE, EXPIRY_DATE,
                 LICENSE_STATUS, VEHICLE_CLASS, RESTRICTIONS,
                 DEMERIT_BALANCE, ISSUED_BY_AUTHORITY, ISSUED_BY_OFFICER )
               VALUES
               ( :HV-APPLICATION-ID, :HV-CANDIDATE-ID, :HV-LICENSE-NUMBER,
                 :HV-LICENSE-TYPE, :HV-ISSUE-DATE, :HV-EXPIRY-DATE,
                 :HV-LICENSE-STATUS, :HV-VEHICLE-CLASS, :HV-RESTRICTIONS,
                 :HV-DEMERIT-BALANCE, :HV-ISSUED-BY-AUTHORITY,
                 :HV-ISSUED-BY-OFFICER )
           END-EXEC

           IF SQLCODE NOT = 0
               MOVE 99 TO WS-RETURN-CODE
               STRING 'DB2 INSERT LICENSE FAILED SQLCODE='
                      SQLCODE DELIMITED SIZE
                      INTO WS-RETURN-MSG
               EXIT PARAGRAPH
           END-IF

           EXEC SQL
               SELECT LICENSE_ID INTO :WS-NEW-LICENSE-ID
               FROM DLIS.ISSUED_LICENSE
               WHERE LICENSE_NUMBER = :HV-LICENSE-NUMBER
           END-EXEC

           MOVE WS-NEW-LICENSE-ID TO CA-LICENSE-ID
           MOVE 0 TO WS-RETURN-CODE.

      *================================================================*
       2000-UPDATE-APPLICATION.
      *================================================================*
           MOVE 'IS'                      TO HV-NEW-APP-STATUS
           MOVE FUNCTION CURRENT-DATE (1:10) TO HV-LAST-UPDATED-DATE
           MOVE CA-ISSUED-BY-OFFICER      TO HV-LAST-UPDATED-BY
           MOVE CA-APPLICATION-ID         TO HV-APP-ID-KEY

           EXEC SQL
               UPDATE DLIS.LICENSE_APPLICATION
               SET    APPLICATION_STATUS = :HV-NEW-APP-STATUS,
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
               MOVE 'DRIVER LICENSE ISSUED SUCCESSFULLY' TO WS-RETURN-MSG
           END-IF.

       END PROGRAM DLISLICS.
