# 01 — System Architecture
## Driver License Issuance System (DLIS)

---

## 1. Architectural Overview

DLIS is built on the **CA Gen Information Engineering** methodology. It is structured across three layers:

| Layer | CA Gen Artefact | Purpose |
|---|---|---|
| **Data Layer** | Entities, Views | Persistent data model — encyclopedia-managed |
| **Logic Layer** | Action Blocks, Business Functions | All procedural rules and validations |
| **Presentation Layer** | Screen Maps, Triggers | User interface and event handling |

The **CA Gen Generator** produces executable target code (COBOL/CICS or C/Java) from these model artefacts. The encyclopedia is the single source of truth — no logic exists outside it.

---

## 2. CA Gen Toolset Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│   Screen Maps (.SCR)    ←→   Triggers (.TRG)                │
│   SCR-MAIN-MENU                TRG-CANDIDATE-MAINT          │
│   SCR-CANDIDATE-MAINT          TRG-APPLICATION-ENTRY        │
│   SCR-APPLICATION-ENTRY        TRG-CHECK-AND-STATUS         │
│   SCR-ELIGIBILITY-CHECK        TRG-PAYMENT-APPROVAL-ISSUE   │
│   SCR-HISTORY-CHECK                                         │
│   SCR-PAYMENT-ENTRY                                         │
│   SCR-APPROVAL-AUTH1                                        │
│   SCR-APPROVAL-AUTH2                                        │
│   SCR-LICENSE-ISSUE                                         │
│   SCR-APPLICATION-STATUS                                    │
├─────────────────────────────────────────────────────────────┤
│                      LOGIC LAYER                            │
│   Business Functions (.BFN)   Action Blocks (.ACB)          │
│   BF-CANDIDATE-MANAGEMENT     AB-CREATE-CANDIDATE           │
│   BF-LICENSE-APPLICATION      AB-CREATE-APPLICATION         │
│   BF-ELIGIBILITY-CHECK        AB-CHECK-ELIGIBILITY          │
│   BF-HISTORY-CHECK            AB-CHECK-HISTORY              │
│   BF-PAYMENT-PROCESSING       AB-PROCESS-PAYMENT            │
│   BF-APPROVAL-AUTHORITY-1     AB-RECORD-APPROVAL-1          │
│   BF-APPROVAL-AUTHORITY-2     AB-RECORD-APPROVAL-2          │
│   BF-LICENSE-ISSUANCE         AB-ISSUE-LICENSE              │
│   BF-LICENSE-INQUIRY          AB-INQUIRE-APPLICATION-STATUS │
├─────────────────────────────────────────────────────────────┤
│                       DATA LAYER                            │
│   Entities (.ENT)             Views (.VEW)                  │
│   CANDIDATE                   24 scoped views across        │
│   LICENSE-APPLICATION         6 view files                  │
│   DRIVING-HISTORY                                           │
│   PAYMENT                                                   │
│   ISSUED-LICENSE                                            │
│   AUTHORITY-USER                                            │
│   LICENSE-FEE-SCHEDULE                                      │
└─────────────────────────────────────────────────────────────┘
                           ↕  CA Gen Generator
┌─────────────────────────────────────────────────────────────┐
│               GENERATED TARGET PLATFORM                     │
│     COBOL + CICS + DB2 (Mainframe)  /  Java (Web)           │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Entity-Relationship Diagram (ERD)

```mermaid
erDiagram
    CANDIDATE {
        NUMERIC(10)     CANDIDATE-ID PK
        CHARACTER(30)   FIRST-NAME
        CHARACTER(30)   LAST-NAME
        DATE            DATE-OF-BIRTH
        CHARACTER(20)   ID-NUMBER
        CHARACTER(50)   ADDRESS-LINE-1
        CHARACTER(30)   CITY
        CHARACTER(30)   STATE-PROVINCE
        CHARACTER(10)   POSTAL-CODE
        CHARACTER(30)   COUNTRY
        CHARACTER(15)   PHONE-NUMBER
        CHARACTER(60)   EMAIL-ADDRESS
        CHARACTER(1)    RECORD-STATUS
    }

    LICENSE-APPLICATION {
        NUMERIC(10)     APPLICATION-ID PK
        NUMERIC(10)     CANDIDATE-ID FK
        CHARACTER(1)    LICENSE-TYPE
        DATE            APPLICATION-DATE
        CHARACTER(2)    APPLICATION-STATUS
        CHARACTER(1)    ELIGIBILITY-CHECK-STATUS
        CHARACTER(1)    HISTORY-CHECK-STATUS
        CHARACTER(1)    PAYMENT-STATUS
        CHARACTER(1)    APPROVAL-1-STATUS
        CHARACTER(50)   APPROVAL-1-AUTHORITY
        CHARACTER(1)    APPROVAL-2-STATUS
        CHARACTER(50)   APPROVAL-2-AUTHORITY
    }

    DRIVING-HISTORY {
        NUMERIC(10)     HISTORY-ID PK
        NUMERIC(10)     CANDIDATE-ID FK
        DATE            INCIDENT-DATE
        CHARACTER(2)    INCIDENT-TYPE
        NUMERIC(3)      DEMERIT-POINTS
        NUMERIC(10-2)   FINE-AMOUNT
        CHARACTER(1)    FINE-PAID-STATUS
        DATE            SUSPENSION-START-DATE
        DATE            SUSPENSION-END-DATE
    }

    PAYMENT {
        NUMERIC(10)     PAYMENT-ID PK
        NUMERIC(10)     APPLICATION-ID FK
        NUMERIC(10)     CANDIDATE-ID FK
        DATE            PAYMENT-DATE
        NUMERIC(10-2)   PAYMENT-AMOUNT
        CHARACTER(2)    PAYMENT-METHOD
        CHARACTER(20)   RECEIPT-NUMBER
        CHARACTER(1)    PAYMENT-STATUS
    }

    ISSUED-LICENSE {
        NUMERIC(10)     LICENSE-ID PK
        NUMERIC(10)     APPLICATION-ID FK
        NUMERIC(10)     CANDIDATE-ID FK
        CHARACTER(20)   LICENSE-NUMBER
        CHARACTER(1)    LICENSE-TYPE
        DATE            ISSUE-DATE
        DATE            EXPIRY-DATE
        CHARACTER(1)    LICENSE-STATUS
        CHARACTER(2)    VEHICLE-CLASS
        NUMERIC(3)      DEMERIT-BALANCE
    }

    AUTHORITY-USER {
        NUMERIC(10)     AUTHORITY-USER-ID PK
        CHARACTER(20)   USER-CODE
        CHARACTER(60)   USER-NAME
        CHARACTER(50)   AUTHORITY-NAME
        CHARACTER(1)    AUTHORITY-LEVEL
        CHARACTER(1)    ACTIVE-STATUS
        CHARACTER(3)    LICENSE-TYPES-AUTHORISED
    }

    LICENSE-FEE-SCHEDULE {
        NUMERIC(10)     FEE-SCHEDULE-ID PK
        CHARACTER(1)    LICENSE-TYPE
        CHARACTER(2)    FEE-TYPE
        NUMERIC(10-2)   FEE-AMOUNT
        DATE            EFFECTIVE-DATE
        CHARACTER(3)    CURRENCY-CODE
        CHARACTER(1)    ACTIVE-STATUS
    }

    CANDIDATE ||--o{ LICENSE-APPLICATION : "submits"
    CANDIDATE ||--o{ DRIVING-HISTORY     : "has"
    CANDIDATE ||--o{ PAYMENT             : "makes"
    CANDIDATE ||--o{ ISSUED-LICENSE      : "holds"
    LICENSE-APPLICATION ||--o{ PAYMENT      : "receives"
    LICENSE-APPLICATION ||--o|  ISSUED-LICENSE : "results in"
```

---

## 4. Application Status State Machine

```mermaid
stateDiagram-v2
    [*] --> PE : Create Application
    PE --> EC : Eligibility Check PASS
    PE --> RE : Eligibility Check FAIL
    EC --> HC : History Check PASS
    EC --> RE : History Check FAIL
    HC --> PA : Payment Recorded
    PA --> A2 : First Authority APPROVE
    PA --> RE : First Authority REJECT
    A2 --> AP : Second Authority APPROVE
    A2 --> RE : Second Authority REJECT
    AP --> IS : License Issued
    RE --> [*]
    IS --> [*]

    PE : PE — Pending
    EC : EC — Eligibility Checked
    HC : HC — History Checked
    PA : PA — Payment Approved
    A2 : A2 — Awaiting 2nd Approval
    AP : AP — Fully Approved
    IS : IS — License Issued
    RE : RE — Rejected
```

---

## 5. Action Block Call Chain

```mermaid
flowchart TD
    SCR1[SCR-CANDIDATE-MAINT] --> TRG1[TRG-CANDIDATE-MAINT]
    TRG1 --> AB1[AB-CREATE-CANDIDATE]

    SCR2[SCR-APPLICATION-ENTRY] --> TRG2[TRG-APPLICATION-ENTRY]
    TRG2 --> AB2[AB-CREATE-APPLICATION]

    SCR3[SCR-ELIGIBILITY-CHECK] --> TRG3[TRG-ELIGIBILITY-RUN]
    TRG3 --> AB3[AB-CHECK-ELIGIBILITY]

    SCR4[SCR-HISTORY-CHECK] --> TRG4[TRG-HISTORY-RUN]
    TRG4 --> AB4[AB-CHECK-HISTORY]

    SCR5[SCR-PAYMENT-ENTRY] --> TRG5[TRG-PAYMENT-PROCESS]
    TRG5 --> AB5[AB-PROCESS-PAYMENT]

    SCR6[SCR-APPROVAL-AUTH1] --> TRG6[TRG-APPROVAL1-SUBMIT]
    TRG6 --> AB6[AB-RECORD-APPROVAL-1]

    SCR7[SCR-APPROVAL-AUTH2] --> TRG7[TRG-APPROVAL2-SUBMIT]
    TRG7 --> AB7[AB-RECORD-APPROVAL-2]

    SCR8[SCR-LICENSE-ISSUE] --> TRG8[TRG-LICENSE-ISSUE]
    TRG8 --> AB8[AB-ISSUE-LICENSE]

    SCR9[SCR-APPLICATION-STATUS] --> TRG9[TRG-STATUS-INQUIRE]
    TRG9 --> AB9[AB-INQUIRE-APPLICATION-STATUS]

    AB1 & AB2 & AB3 & AB4 & AB5 & AB6 & AB7 & AB8 & AB9 --> ENC[(Encyclopedia\nEntities & Views)]
```

---

## 6. View Layer Architecture

Each Action Block operates exclusively through **Views** — never directly on entities. This enforces:
- **Minimal privilege** — each view exposes only the attributes required for the operation
- **Decoupling** — action block logic does not depend on entity physical layout
- **Reusability** — views can be shared across multiple action blocks

| View Category | Example | Used By |
|---|---|---|
| ALL views | `VAPPLICATION-ALL` | Inquiry and full-read operations |
| CREATE views | `VAPPLICATION-CREATE` | Insert new record operations |
| UPDATE views | `VAPPLICATION-ELIGIBILITY-UPD` | Targeted field updates |
| LOOKUP views | `VCANDIDATE-ID-LOOKUP` | Key-based lookups |
| CHECK views | `VHISTORY-CHECK` | Business rule evaluation |
| RECEIPT views | `VPAYMENT-RECEIPT` | Output/print operations |

---

## 7. Security & Segregation of Duties

| Control | Implementation |
|---|---|
| Dual approval required | Two separate `AUTHORITY-USER` records with `AUTHORITY-LEVEL` 1 and 2 |
| Different approvers enforced | `AB-RECORD-APPROVAL-2` rejects if `APPROVAL-1-AUTHORITY = APPROVAL-2-AUTHORITY` |
| License type authorisation | `LICENSE-TYPES-AUTHORISED` field restricts which types each officer may approve |
| Sequential gate enforcement | Each AB checks preceding step status before proceeding |
