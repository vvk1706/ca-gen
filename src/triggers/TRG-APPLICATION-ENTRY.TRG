*================================================================*
* Trigger Definitions
* Subject : APPLICATION ENTRY screen triggers
* Screen  : SCR-APPLICATION-ENTRY
*================================================================*

*----------------------------------------------------------------*
* TRIGGER: TRG-APPLICATION-SUBMIT
* Event   : PF1 pressed on SCR-APPLICATION-ENTRY
*----------------------------------------------------------------*
TRIGGER: TRG-APPLICATION-SUBMIT
  SCREEN: SCR-APPLICATION-ENTRY
  EVENT:  PF1

  ACTION-DIAGRAM:
    MOVE FLD-CAND-ID   TO WS-CANDIDATE-ID
    MOVE FLD-LIC-TYPE  TO WS-LICENSE-TYPE
    MOVE USER-ID       TO WS-CREATED-BY

    USE AB-CREATE-APPLICATION

    IF WS-RETURN-CODE = 0
      MOVE WS-APPLICATION-ID TO FLD-APP-ID
      MOVE 'GREEN'            TO FLD-MSG COLOR
    ELSE
      MOVE 'RED'              TO FLD-MSG COLOR
    END-IF
    MOVE WS-RETURN-MSG TO FLD-MSG

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-CANDIDATE-LOOKUP
* Event   : PF3 pressed on SCR-APPLICATION-ENTRY
*           Populates candidate name from ID
*----------------------------------------------------------------*
TRIGGER: TRG-CANDIDATE-LOOKUP
  SCREEN: SCR-APPLICATION-ENTRY
  EVENT:  PF3

  ACTION-DIAGRAM:
    IF FLD-CAND-ID IS EMPTY
      MOVE 'ENTER CANDIDATE ID' TO FLD-MSG
      ESCAPE
    END-IF

    READ VCANDIDATE-SUMMARY
      WHERE CANDIDATE-ID = FLD-CAND-ID
      IMPORT VCANDIDATE-SUMMARY
        CONCATENATE FIRST-NAME ' ' LAST-NAME INTO FLD-CAND-NAME
        MOVE 'CANDIDATE FOUND' TO FLD-MSG
        MOVE 'GREEN' TO FLD-MSG COLOR
      END-IMPORT
      WHEN NONE
        MOVE 'CANDIDATE NOT FOUND' TO FLD-MSG
        MOVE 'RED' TO FLD-MSG COLOR
    END-READ

END-TRIGGER

*----------------------------------------------------------------*
* TRIGGER: TRG-FEE-CALCULATE
* Event   : PF5 pressed on SCR-APPLICATION-ENTRY
*           Looks up and displays the applicable fee
*----------------------------------------------------------------*
TRIGGER: TRG-FEE-CALCULATE
  SCREEN: SCR-APPLICATION-ENTRY
  EVENT:  PF5

  ACTION-DIAGRAM:
    IF FLD-LIC-TYPE IS EMPTY
      MOVE 'SELECT LICENSE TYPE FIRST' TO FLD-MSG
      ESCAPE
    END-IF

    READ VFEE-LOOKUP
      WHERE LICENSE-TYPE  = FLD-LIC-TYPE
      AND   FEE-TYPE      = 'IF'
      AND   ACTIVE-STATUS = 'A'
      IMPORT VFEE-LOOKUP
        MOVE FEE-AMOUNT     TO FLD-FEE
        MOVE CURRENCY-CODE  TO FLD-CURR
        MOVE 'FEE RETRIEVED SUCCESSFULLY' TO FLD-MSG
        MOVE 'GREEN' TO FLD-MSG COLOR
      END-IMPORT
      WHEN NONE
        MOVE 'NO FEE SCHEDULE FOUND FOR THIS LICENSE TYPE' TO FLD-MSG
        MOVE 'RED' TO FLD-MSG COLOR
    END-READ

END-TRIGGER

END-TRIGGERS: SCR-APPLICATION-ENTRY
