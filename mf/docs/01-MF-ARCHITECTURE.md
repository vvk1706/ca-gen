# 01 — System Architecture

## Overview

DLIS is a multi-tier CICS online system running on IBM z/OS.  
Each user interaction is a **pseudo-conversational** CICS transaction: the program sends a BMS map, issues `EXEC CICS RETURN TRANSID(…) COMMAREA(…)`, and terminates. CICS reinstates the program on the next terminal input, restoring state via the 350-byte COMMAREA.

---

## System layers

```
┌─────────────────────────────────────────────────────────────────┐
│  3270 TERMINAL (24×80)                                          │
│  User types into BMS-rendered screens, presses PF keys          │
└────────────────────────────┬────────────────────────────────────┘
                             │  BMS SEND/RECEIVE
┌────────────────────────────▼────────────────────────────────────┐
│  CICS/TS REGION                                                 │
│  ┌──────────┐  XCTL  ┌──────────┐  XCTL  ┌──────────┐         │
│  │ DLISMENU │───────▶│ DLISCAND │        │ DLISAPPL │  …       │
│  │  (DL01)  │        │  (DL02)  │        │  (DL03)  │         │
│  └──────────┘        └──────────┘        └──────────┘         │
│                                                                 │
│  All programs share DLIS-COMMAREA (350 bytes) via DFHCOMMAREA   │
│  DB2 access via EXEC SQL embedded in COBOL + DB2ENTRY DLISDB2E  │
└────────────────────────────┬────────────────────────────────────┘
                             │  EXEC SQL
┌────────────────────────────▼────────────────────────────────────┐
│  DB2 for z/OS  (database: DLISDB)                               │
│  Schema: DLIS                                                   │
│  Tables: CANDIDATE · LICENSE_APPLICATION · DRIVING_HISTORY      │
│          PAYMENT · ISSUED_LICENSE · AUTHORITY_USER              │
│          LICENSE_FEE_SCHEDULE                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## CICS program map

```
DL01 ──▶ DLISMENU
             │
             ├── opt 1/2 ──▶ DLISCAND  (DL02)  Candidate Maint / Inquiry
             ├── opt 3   ──▶ DLISAPPL  (DL03)  Application Entry
             ├── opt 4   ──▶ DLISSTAT  (DL04)  Status Inquiry
             ├── opt 5   ──▶ DLISELIG  (DL05)  Eligibility Check
             ├── opt 6   ──▶ DLISHIST  (DL06)  History Check
             ├── opt 7   ──▶ DLISPAY   (DL07)  Payment Entry
             ├── opt 8   ──▶ DLISAP1   (DL08)  First Approval
             ├── opt 9   ──▶ DLISAP2   (DL09)  Second Approval
             └── opt 10  ──▶ DLISISSU  (DL10)  Licence Issuance

DLISPAY ──LINK──▶ DLISRPRT   (receipt print stub)
DLISISSU ──LINK──▶ DLISLPRT  (licence print stub)
```

Navigation uses `EXEC CICS XCTL` (transfer of control — no return).  
Print helpers use `EXEC CICS LINK` (call-and-return).  
All transactions return to themselves (`EXEC CICS RETURN TRANSID(self)`) for pseudo-conversational looping.

---

## Transaction / Program / Map inventory

| Trans | Program | Mapset | Map | Timeout |
|---|---|---|---|---|
| DL01 | DLISMENU | DLISMM | DLISMM01 | 10 s |
| DL02 | DLISCAND | DLISCM | DLISCM01 | 10 s |
| DL03 | DLISAPPL | DLISAE | DLISAE01 | 10 s |
| DL04 | DLISSTAT | DLISST | DLISST01 | 10 s |
| DL05 | DLISELIG | DLISEC | DLISEC01 | 10 s |
| DL06 | DLISHIST | DLISHC | DLISHC01 | 10 s |
| DL07 | DLISPAY  | DLISPE | DLISPE01 | 10 s |
| DL08 | DLISAP1  | DLISA1 | DLISA1M01 | 30 s |
| DL09 | DLISAP2  | DLISA2 | DLISA2M01 | 30 s |
| DL10 | DLISISSU | DLISLI | DLISLIM01 | 30 s |

All transactions are in CICS group **DLISGRP**, listed in **DLISLIST**.  
All programs run `EXECKEY(USER)`, `CONCURRENCY(QUASIRENT)`.

---

## COMMAREA design

COMMAREA is 350 bytes, defined in copybook [`DLISCSEC.cpy`](../copybook/DLISCSEC.cpy).  
Three overlapping 01-level layouts share the same 350-byte area:

### `DLIS-COMMAREA` — main navigation area

| Field | Type | Purpose |
|---|---|---|
| CA-TRANSACTION-ID | PIC X(4) | Current transaction ID |
| CA-RETURN-SCREEN | PIC X(8) | Screen to return to |
| CA-CANDIDATE-ID | S9(10) COMP-3 | Active candidate key |
| CA-APPLICATION-ID | S9(10) COMP-3 | Active application key |
| CA-LICENSE-ID | S9(10) COMP-3 | Active licence key |
| CA-PAYMENT-ID | S9(10) COMP-3 | Active payment key |
| CA-USER-ID | PIC X(8) | Signed-on user |
| CA-RETURN-CODE | S9(4) COMP | Return code |
| CA-MSG-TEXT | PIC X(78) | Message to display |
| CA-MSG-COLOR | PIC X(5) | Message colour hint |
| CA-ACTION | PIC X(1) | Action flag |

### `DLIS-CAND-COMMAREA` — candidate screen data

Carries the full candidate record (name, DOB, ID number, address, contact, status) between programs.

### `DLIS-APPL-COMMAREA` — application status data

Carries application ID, candidate ID, licence type, all five gate statuses (elig / hist / pay / appr1 / appr2), and issued licence number.

---

## Application state machine

Each licence application progresses through a strict sequential pipeline.  
The `APPLICATION_STATUS` column tracks the position:

```
         ┌────────────────────────────────────────────────────┐
         │                                                    │
Submit   ▼  Elig Pass   ▼  Hist Pass   ▼  Payment   ▼       │
──▶ [PE] ──▶ [EC] ──▶ [HC] ──▶ [PA] ──▶ [A2] ──▶ [AP] ──▶ [IS]
        │          │          │          │          │
        │ Fail     │ Fail     │          │ Reject   │ Reject
        └──▶ [RE]  └──▶ [RE]  │          └──▶ [RE]  └──▶ [RE]
                              │
                         (payment moves directly
                          to A2 via DL08 approval)
```

| Code | Meaning |
|---|---|
| PE | Pending — awaiting eligibility check |
| EC | Eligibility checked — awaiting history check |
| HC | History checked — awaiting payment |
| PA | Payment received — awaiting first approval |
| A2 | First approval granted — awaiting second approval |
| AP | Fully approved — ready for licence issue |
| IS | Licence issued |
| RE | Rejected (can occur at any stage) |

---

## Eligibility rules (DLISELIG)

Age is computed from `DATE_OF_BIRTH` year vs current year (integer arithmetic).

| Licence type | Min age | Prior licence required |
|---|---|---|
| L — Learner | 16 | — |
| P — Probation | 17 | Active Learner (L) licence in `ISSUED_LICENSE` |
| O — Open | 18 | Active Probation (P) licence in `ISSUED_LICENSE` |

---

## History check rules (DLISHIST)

Cursor `HISTCUR` reads all active `DRIVING_HISTORY` rows for the candidate ordered by date descending (max 50 rows buffered).

| Condition | Result |
|---|---|
| Any incident type `SU` or `DQ` with `SUSP_END_DATE >= TODAY` or null | FAIL — active suspension/disqualification |
| Total demerit points > 12 | FAIL — demerit threshold exceeded |
| Any fine with `FINE_PAID_STATUS = 'N'` | FAIL — unpaid fines |
| None of the above | PASS |

History check is only allowed after eligibility check has passed (`ELIG_CHECK_STATUS = 'P'`).

---

## Dual-authority approval rules (DLISAP1 / DLISAP2)

- First approver must hold `AUTHORITY_LEVEL = '1'` in `AUTHORITY_USER`.
- Second approver must hold `AUTHORITY_LEVEL = '2'`.
- **Segregation of duties**: second approver's `AUTHORITY_NAME` must differ from first approver's authority stored in `APPROVAL_1_AUTHORITY`.
- Each approver's `LIC_TYPES_AUTH` (3-char string) must include the licence type being approved.

---

## Licence number generation (DLISISSU)

```
Format:  <PREFIX><VEHICLE-CLASS><CANDIDATE_ID><APPLICATION_ID>
Prefix:  LRN = Learner  |  PRB = Probation  |  OPN = Open
Example: LRNAnnnnnnnnnn  (where A = vehicle class, n = numeric IDs)
```

Expiry date is calculated from issue date + years:

| Type | Validity |
|---|---|
| L — Learner | 1 year |
| P — Probation | 2 years |
| O — Open | 5 years |

Demerit balance starts at **12** at issuance.
