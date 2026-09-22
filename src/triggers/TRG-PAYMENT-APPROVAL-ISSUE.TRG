*================================================================*
* Trigger Definitions
* Subject : PAYMENT, APPROVAL 1, APPROVAL 2, LICENSE ISSUE screens
*================================================================*

*----------------------------------------------------------------*
* TRIGGER: TRG-PAYMENT-PROCESS
* Event   : PF1 on SCR-PAYMENT-ENTRY
*----------------------------------------------------------------*
TRIGGER: TRG-PAYMENT-PROCESS
  SCREEN: SCR-PAYMENT-ENTRY
  EVENT:  PF1

  ACTION-DIAGRAM:
    MOVE FLD-APP-ID    TO WS-APPLICATION-ID
    MOVE FLD-CAND-ID   TO WS-CANDIDATE-ID
    MOVE FLD-PAY-METH  TO WS-PAYMENT-METHOD
    MOVE FLD-PAY-REF   TO WS-PAYMENT-REFERENCE
    MOVE FLD-PROC-BY   TO WS-PROCESSED-BY

    USE AB-PROCESS-PAYMENT

    IF WS-RETURN-CODE = 0
      MOVE WS-PAYMENT-ID    TO FLD-PAY-ID
      MOVE WS-FEE-AMOUNT    TO FLD-FEE
      MOVE WS-RECEIPT-NUMBER TO FLD-RECEIPT
      MOVE 'S'              TO FLD-PAY-STAT
      MOVE 'GREEN'          TO FLD-MSG COLOR
    ELSE
      MOVE 'RED'            TO FLD-MSG COLOR
    END-IF
    MOVE WS-RETURN-MSG TO FLD-MSG

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-RECEIPT-PRINT
* Event   : PF5 on SCR-PAYMENT-ENTRY
*----------------------------------------------------------------*
TRIGGER: TRG-RECEIPT-PRINT
  SCREEN: SCR-PAYMENT-ENTRY
  EVENT:  PF5

  ACTION-DIAGRAM:
    IF FLD-PAY-ID IS EMPTY
      MOVE 'PROCESS PAYMENT FIRST BEFORE PRINTING RECEIPT' TO FLD-MSG
      ESCAPE
    END-IF
    * Call external print action block / report
    CALL AB-PRINT-RECEIPT
      IMPORT WS-PAYMENT-ID = FLD-PAY-ID
    END-CALL
    MOVE 'RECEIPT SENT TO PRINTER' TO FLD-MSG
    MOVE 'GREEN' TO FLD-MSG COLOR

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-APPROVAL1-SUBMIT
* Event   : PF1 on SCR-APPROVAL-AUTH1
*----------------------------------------------------------------*
TRIGGER: TRG-APPROVAL1-SUBMIT
  SCREEN: SCR-APPROVAL-AUTH1
  EVENT:  PF1

  ACTION-DIAGRAM:
    MOVE FLD-APP-ID      TO WS-APPLICATION-ID
    MOVE FLD-AUTH-CODE   TO WS-AUTHORITY-USER-CODE
    MOVE FLD-DECISION    TO WS-DECISION
    MOVE FLD-DEC-NOTES   TO WS-DECISION-NOTES

    USE AB-RECORD-APPROVAL-1

    IF WS-RETURN-CODE = 0
      MOVE 'GREEN' TO FLD-MSG COLOR
    ELSE
      MOVE 'RED'   TO FLD-MSG COLOR
    END-IF
    MOVE WS-RETURN-MSG TO FLD-MSG

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-AUTHORITY-VALIDATE
* Event   : PF4 on approval screens
*           Validates authority user code and populates name
*----------------------------------------------------------------*
TRIGGER: TRG-AUTHORITY-VALIDATE
  SCREEN: SHARED
  EVENT:  PF4

  ACTION-DIAGRAM:
    IF FLD-AUTH-CODE IS EMPTY
      MOVE 'ENTER AUTHORITY USER CODE' TO FLD-MSG
      ESCAPE
    END-IF

    READ VAUTHORITY-LOOKUP
      WHERE USER-CODE     = FLD-AUTH-CODE
      AND   ACTIVE-STATUS = 'A'
      IMPORT VAUTHORITY-LOOKUP
        MOVE USER-NAME       TO FLD-AUTH-NAME
        MOVE 'AUTHORITY USER VALIDATED' TO FLD-MSG
        MOVE 'GREEN' TO FLD-MSG COLOR
      END-IMPORT
      WHEN NONE
        MOVE 'AUTHORITY USER NOT FOUND OR INACTIVE' TO FLD-MSG
        MOVE 'RED' TO FLD-MSG COLOR
    END-READ

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-APPROVAL2-SUBMIT
* Event   : PF1 on SCR-APPROVAL-AUTH2
*----------------------------------------------------------------*
TRIGGER: TRG-APPROVAL2-SUBMIT
  SCREEN: SCR-APPROVAL-AUTH2
  EVENT:  PF1

  ACTION-DIAGRAM:
    MOVE FLD-APP-ID      TO WS-APPLICATION-ID
    MOVE FLD-AUTH-CODE   TO WS-AUTHORITY-USER-CODE
    MOVE FLD-DECISION    TO WS-DECISION
    MOVE FLD-DEC-NOTES   TO WS-DECISION-NOTES

    USE AB-RECORD-APPROVAL-2

    IF WS-RETURN-CODE = 0
      MOVE 'GREEN' TO FLD-MSG COLOR
    ELSE
      MOVE 'RED'   TO FLD-MSG COLOR
    END-IF
    MOVE WS-RETURN-MSG TO FLD-MSG

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-LICENSE-ISSUE
* Event   : PF1 on SCR-LICENSE-ISSUE
*----------------------------------------------------------------*
TRIGGER: TRG-LICENSE-ISSUE
  SCREEN: SCR-LICENSE-ISSUE
  EVENT:  PF1

  ACTION-DIAGRAM:
    MOVE FLD-APP-ID      TO WS-APPLICATION-ID
    MOVE FLD-VEH-CLASS   TO WS-VEHICLE-CLASS
    MOVE FLD-RESTRICT    TO WS-RESTRICTIONS
    MOVE FLD-OFFICER     TO WS-ISSUED-BY-OFFICER
    MOVE FLD-AUTHORITY   TO WS-ISSUED-BY-AUTHORITY

    USE AB-ISSUE-LICENSE

    IF WS-RETURN-CODE = 0
      MOVE WS-LICENSE-NUMBER TO FLD-LIC-NUM
      MOVE WS-EXPIRY-DATE    TO FLD-EXPIRY
      MOVE 'GREEN'           TO FLD-MSG COLOR
    ELSE
      MOVE 'RED'             TO FLD-MSG COLOR
    END-IF
    MOVE WS-RETURN-MSG TO FLD-MSG

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-LICENSE-PRINT
* Event   : PF6 on SCR-LICENSE-ISSUE
*----------------------------------------------------------------*
TRIGGER: TRG-LICENSE-PRINT
  SCREEN: SCR-LICENSE-ISSUE
  EVENT:  PF6

  ACTION-DIAGRAM:
    IF FLD-LIC-NUM IS EMPTY
      MOVE 'ISSUE LICENSE FIRST BEFORE PRINTING' TO FLD-MSG
      ESCAPE
    END-IF
    CALL AB-PRINT-LICENSE
      IMPORT WS-LICENSE-NUMBER = FLD-LIC-NUM
    END-CALL
    MOVE 'LICENSE SENT TO PRINTER' TO FLD-MSG
    MOVE 'GREEN' TO FLD-MSG COLOR

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-MENU-SELECT
* Event   : ENTER on SCR-MAIN-MENU
*----------------------------------------------------------------*
TRIGGER: TRG-MENU-SELECT
  SCREEN: SCR-MAIN-MENU
  EVENT:  ENTER

  ACTION-DIAGRAM:
    IF FLD-OPT IS EMPTY
      MOVE 'PLEASE SELECT AN OPTION (1-15)' TO FLD-MSG
      ESCAPE
    END-IF

    IF FLD-OPT NOT IN ('1','2','3','4','5','6','7','8','9','10',
                       '11','12','13','14','15')
      MOVE 'INVALID OPTION. PLEASE SELECT 1-15' TO FLD-MSG
      MOVE 'RED' TO FLD-MSG COLOR
      ESCAPE
    END-IF

    * Menu routing handled by MENU-ROUTING section of SCR-MAIN-MENU

END-TRIGGER

END-TRIGGERS: PAYMENT-APPROVAL-ISSUE-SCREENS
