# 02 — Data Model

**Database:** DLISDB  
**Schema:** DLIS  
**Tablespaces:** DLISTS01 (candidates, applications, history) · DLISTS02 (payments, licences, users, fees)  
**Lock granularity:** ROW (`LOCKSIZE ROW`)

---

## Entity-relationship diagram

```
AUTHORITY_USER               LICENSE_FEE_SCHEDULE
(approver registry)          (fee schedule)
     │                              │
     │ looked up by                 │ looked up by
     │ DLISAP1/AP2                  │ DLISAPPL/DLISPAY
     │                              │
     └──────────────────┬───────────┘
                        │
CANDIDATE ──────────────┼──────── LICENSE_APPLICATION
   │  PK: CANDIDATE_ID  │              │  PK: APPLICATION_ID
   │                    │              │  FK: CANDIDATE_ID
   │                    │              │
   ├── DRIVING_HISTORY  │              ├── PAYMENT
   │     FK: CANDIDATE_ID              │     FK: APPLICATION_ID
   │                                   │     FK: CANDIDATE_ID
   └───────────────────────────────────┤
                                       └── ISSUED_LICENSE
                                             FK: APPLICATION_ID
                                             FK: CANDIDATE_ID
```

---

## Table: DLIS.CANDIDATE

Stores one row per person who wishes to obtain a driving licence.

| Column | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| CANDIDATE_ID | DECIMAL(10,0) | NOT NULL | PK · IDENTITY | Auto-generated |
| FIRST_NAME | CHAR(30) | NOT NULL | | |
| LAST_NAME | CHAR(30) | NOT NULL | | |
| DATE_OF_BIRTH | DATE | NOT NULL | | Used for age checks |
| ID_NUMBER | CHAR(20) | NOT NULL | UNIQUE | National ID — duplicate check on create |
| ADDRESS_LINE_1 | CHAR(50) | NOT NULL | | |
| ADDRESS_LINE_2 | CHAR(50) | nullable | | Optional second address line |
| CITY | CHAR(30) | NOT NULL | | |
| STATE_PROVINCE | CHAR(30) | NOT NULL | | |
| POSTAL_CODE | CHAR(10) | nullable | | |
| COUNTRY | CHAR(30) | NOT NULL | | |
| PHONE_NUMBER | CHAR(15) | nullable | | |
| EMAIL_ADDRESS | CHAR(60) | nullable | | |
| CREATED_DATE | DATE | NOT NULL | DEFAULT CURRENT DATE | |
| RECORD_STATUS | CHAR(1) | NOT NULL | CHECK IN ('A','I') | A=Active, I=Inactive |

**Tablespace:** DLISTS01  
**Check constraints:** `CAND_ST` — RECORD_STATUS IN ('A','I')

---

## Table: DLIS.LICENSE_APPLICATION

One row per licence application. Tracks all five gate statuses.

| Column | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| APPLICATION_ID | DECIMAL(10,0) | NOT NULL | PK · IDENTITY | Auto-generated |
| CANDIDATE_ID | DECIMAL(10,0) | NOT NULL | FK → CANDIDATE | |
| LICENSE_TYPE | CHAR(1) | NOT NULL | CHECK IN ('L','P','O') | L=Learner, P=Probation, O=Open |
| APPLICATION_DATE | DATE | NOT NULL | DEFAULT CURRENT DATE | |
| APPLICATION_STATUS | CHAR(2) | NOT NULL | DEFAULT 'PE' · CHECK | See status codes below |
| ELIG_CHECK_STATUS | CHAR(1) | NOT NULL | DEFAULT 'U' · CHECK | P=Pass, F=Fail, U=Unchecked |
| ELIG_CHECK_DATE | DATE | nullable | | Set by DLISELIG |
| ELIG_CHECK_NOTES | CHAR(200) | nullable | | Failure reason text |
| HIST_CHECK_STATUS | CHAR(1) | NOT NULL | DEFAULT 'U' · CHECK | P=Pass, F=Fail, U=Unchecked |
| HIST_CHECK_DATE | DATE | nullable | | Set by DLISHIST |
| HIST_CHECK_NOTES | CHAR(200) | nullable | | Failure reason text |
| PAYMENT_STATUS | CHAR(1) | NOT NULL | DEFAULT 'U' · CHECK | P=Paid, U=Unpaid, W=Waived |
| PAYMENT_REFERENCE | CHAR(20) | nullable | | Set by DLISPAY |
| APPROVAL_1_STATUS | CHAR(1) | NOT NULL | DEFAULT 'U' · CHECK | A=Approved, R=Rejected, U=Unchecked |
| APPROVAL_1_AUTHORITY | CHAR(50) | nullable | | Name of authority — used for AP2 segregation |
| APPROVAL_1_DATE | DATE | nullable | | |
| APPROVAL_1_NOTES | CHAR(200) | nullable | | Decision notes |
| APPROVAL_2_STATUS | CHAR(1) | NOT NULL | DEFAULT 'U' · CHECK | A=Approved, R=Rejected, U=Unchecked |
| APPROVAL_2_AUTHORITY | CHAR(50) | nullable | | |
| APPROVAL_2_DATE | DATE | nullable | | |
| APPROVAL_2_NOTES | CHAR(200) | nullable | | |
| REJECTION_REASON | CHAR(300) | nullable | | |
| CREATED_DATE | DATE | NOT NULL | DEFAULT CURRENT DATE | |
| LAST_UPDATED_DATE | DATE | NOT NULL | DEFAULT CURRENT DATE | |
| CREATED_BY | CHAR(20) | NOT NULL | | EIBTRMID from CICS |
| LAST_UPDATED_BY | CHAR(20) | NOT NULL | | |

**APPLICATION_STATUS check values:**  
`PE` · `EC` · `HC` · `PA` · `A1` · `A2` · `AP` · `RE` · `IS`

**Tablespace:** DLISTS01

---

## Table: DLIS.DRIVING_HISTORY

One row per recorded driving incident for a candidate.  
Populated externally (e.g., by a traffic authority feed); read by DLISHIST.

| Column | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| HISTORY_ID | DECIMAL(10,0) | NOT NULL | PK · IDENTITY | |
| CANDIDATE_ID | DECIMAL(10,0) | NOT NULL | FK → CANDIDATE | |
| INCIDENT_DATE | DATE | NOT NULL | | |
| INCIDENT_TYPE | CHAR(2) | NOT NULL | CHECK IN ('OF','AC','SU','DQ') | OF=Offence, AC=Accident, SU=Suspension, DQ=Disqualification |
| INCIDENT_DESC | CHAR(300) | nullable | | |
| DEMERIT_POINTS | DECIMAL(3,0) | nullable | | Accumulated to demerit total |
| FINE_AMOUNT | DECIMAL(10,2) | nullable | | |
| FINE_PAID_STATUS | CHAR(1) | nullable | CHECK IN ('Y','N') | Unpaid fines block approval |
| SUSP_START_DATE | DATE | nullable | | |
| SUSP_END_DATE | DATE | nullable | | Null = indefinite suspension |
| COURT_CASE_NUMBER | CHAR(20) | nullable | | |
| RECORDED_BY_AUTH | CHAR(50) | nullable | | |
| RECORD_STATUS | CHAR(1) | NOT NULL | DEFAULT 'A' · CHECK IN ('A','I') | |

**Tablespace:** DLISTS01

---

## Table: DLIS.PAYMENT

One row per successful payment transaction.

| Column | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| PAYMENT_ID | DECIMAL(10,0) | NOT NULL | PK · IDENTITY | |
| APPLICATION_ID | DECIMAL(10,0) | NOT NULL | FK → LICENSE_APPLICATION | |
| CANDIDATE_ID | DECIMAL(10,0) | NOT NULL | FK → CANDIDATE | |
| PAYMENT_DATE | DATE | NOT NULL | DEFAULT CURRENT DATE | |
| PAYMENT_AMOUNT | DECIMAL(10,2) | NOT NULL | | Copied from fee schedule |
| PAYMENT_METHOD | CHAR(2) | NOT NULL | CHECK IN ('CC','DC','EF','CS','CH') | CC=Credit card, DC=Debit, EF=EFT, CS=Cash, CH=Cheque |
| PAYMENT_REFERENCE | CHAR(30) | NOT NULL | | Bank or terminal reference |
| PAYMENT_STATUS | CHAR(1) | NOT NULL | DEFAULT 'P' · CHECK IN ('S','F','R','P') | S=Settled, F=Failed, R=Refunded, P=Pending |
| LICENSE_TYPE | CHAR(1) | NOT NULL | CHECK IN ('L','P','O') | |
| FEE_TYPE | CHAR(2) | NOT NULL | CHECK IN ('IF','RF','LF','PF') | IF=Initial fee |
| RECEIPT_NUMBER | CHAR(20) | nullable | UNIQUE | Format: RCP+YYYYMMDD+APPLICATION_ID |
| PROCESSED_BY | CHAR(20) | nullable | | Operator ID |
| NOTES | CHAR(200) | nullable | | |

**Tablespace:** DLISTS02

---

## Table: DLIS.ISSUED_LICENSE

One row per issued physical licence.

| Column | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| LICENSE_ID | DECIMAL(10,0) | NOT NULL | PK · IDENTITY | |
| APPLICATION_ID | DECIMAL(10,0) | NOT NULL | FK → LICENSE_APPLICATION | |
| CANDIDATE_ID | DECIMAL(10,0) | NOT NULL | FK → CANDIDATE | |
| LICENSE_NUMBER | CHAR(20) | NOT NULL | UNIQUE | LRN/PRB/OPN + class + IDs |
| LICENSE_TYPE | CHAR(1) | NOT NULL | CHECK IN ('L','P','O') | |
| ISSUE_DATE | DATE | NOT NULL | DEFAULT CURRENT DATE | |
| EXPIRY_DATE | DATE | NOT NULL | | Computed at issuance |
| LICENSE_STATUS | CHAR(1) | NOT NULL | DEFAULT 'A' · CHECK IN ('A','S','E','C','R') | A=Active, S=Suspended, E=Expired, C=Cancelled, R=Renewed |
| VEHICLE_CLASS | CHAR(2) | NOT NULL | CHECK IN ('A','B','C','D') | |
| RESTRICTIONS | CHAR(200) | nullable | | Free text restrictions |
| DEMERIT_BALANCE | DECIMAL(3,0) | | DEFAULT 12 | Starting balance |
| ISSUED_BY_AUTH | CHAR(50) | NOT NULL | | Issuing authority name |
| ISSUED_BY_OFFICER | CHAR(50) | NOT NULL | | Officer name |
| RENEWAL_COUNT | DECIMAL(3,0) | | DEFAULT 0 | |
| PREV_LICENSE_ID | DECIMAL(10,0) | nullable | | For renewals |
| NOTES | CHAR(300) | nullable | | |

**Tablespace:** DLISTS02

---

## Table: DLIS.AUTHORITY_USER

Registry of officials who may approve licence applications.

| Column | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| AUTHORITY_USER_ID | DECIMAL(10,0) | NOT NULL | PK · IDENTITY | |
| USER_CODE | CHAR(20) | NOT NULL | UNIQUE | Login code entered on approval screens |
| USER_NAME | CHAR(60) | NOT NULL | | Display name |
| AUTHORITY_NAME | CHAR(50) | NOT NULL | | Name of the authority body |
| AUTHORITY_LEVEL | CHAR(1) | NOT NULL | CHECK IN ('1','2') | 1=First approver, 2=Second approver |
| DEPARTMENT | CHAR(50) | nullable | | |
| PHONE_NUMBER | CHAR(15) | nullable | | |
| EMAIL_ADDRESS | CHAR(60) | nullable | | |
| ACTIVE_STATUS | CHAR(1) | NOT NULL | DEFAULT 'A' · CHECK IN ('A','I') | |
| LIC_TYPES_AUTH | CHAR(3) | nullable | | Bitmask-style: e.g., 'LPO' = authorised for all three |

**Tablespace:** DLISTS02  
**Note:** Segregation of duties is enforced programmatically — `APPROVAL_2_AUTHORITY` in `LICENSE_APPLICATION` must differ from `AUTHORITY_NAME` of the Level-2 approver.

---

## Table: DLIS.LICENSE_FEE_SCHEDULE

Date-effective fee schedule. Multiple rows can exist per licence type; the most current active row with `EFFECTIVE_DATE <= CURRENT DATE` is used.

| Column | Type | Nullable | Constraints | Notes |
|---|---|---|---|---|
| FEE_SCHEDULE_ID | DECIMAL(10,0) | NOT NULL | PK · IDENTITY | |
| LICENSE_TYPE | CHAR(1) | NOT NULL | CHECK IN ('L','P','O') | |
| FEE_TYPE | CHAR(2) | NOT NULL | CHECK IN ('IF','RF','LF','PF') | IF=Initial fee, RF=Renewal fee, LF=Late fee, PF=Processing fee |
| FEE_AMOUNT | DECIMAL(10,2) | NOT NULL | | |
| EFFECTIVE_DATE | DATE | NOT NULL | | |
| EXPIRY_DATE | DATE | nullable | | Null = no expiry |
| CURRENCY_CODE | CHAR(3) | NOT NULL | | ISO 4217 |
| DESCRIPTION | CHAR(200) | nullable | | |
| ACTIVE_STATUS | CHAR(1) | NOT NULL | DEFAULT 'A' · CHECK IN ('A','I') | |

**Tablespace:** DLISTS02

---

## Foreign key relationships

| Constraint | Child table / column | Parent table / column |
|---|---|---|
| APPL_CAND | LICENSE_APPLICATION.CANDIDATE_ID | CANDIDATE.CANDIDATE_ID |
| HIST_CAND | DRIVING_HISTORY.CANDIDATE_ID | CANDIDATE.CANDIDATE_ID |
| PAY_APPL | PAYMENT.APPLICATION_ID | LICENSE_APPLICATION.APPLICATION_ID |
| PAY_CAND | PAYMENT.CANDIDATE_ID | CANDIDATE.CANDIDATE_ID |
| LIC_APPL | ISSUED_LICENSE.APPLICATION_ID | LICENSE_APPLICATION.APPLICATION_ID |
| LIC_CAND | ISSUED_LICENSE.CANDIDATE_ID | CANDIDATE.CANDIDATE_ID |

---

## Indexes

### CANDIDATE
| Index | Unique | Columns |
|---|---|---|
| XCAND01 | YES | CANDIDATE_ID |
| XCAND02 | YES | ID_NUMBER |
| XCAND03 | NO | LAST_NAME, FIRST_NAME |

### LICENSE_APPLICATION
| Index | Unique | Columns |
|---|---|---|
| XAPPL01 | YES | APPLICATION_ID |
| XAPPL02 | NO | CANDIDATE_ID, LICENSE_TYPE, APPLICATION_STATUS |
| XAPPL03 | NO | APPLICATION_STATUS |

### DRIVING_HISTORY
| Index | Unique | Columns |
|---|---|---|
| XHIST01 | YES | HISTORY_ID |
| XHIST02 | NO | CANDIDATE_ID, RECORD_STATUS |
| XHIST03 | NO | CANDIDATE_ID, INCIDENT_TYPE, RECORD_STATUS |

### PAYMENT
| Index | Unique | Columns |
|---|---|---|
| XPAY01 | YES | PAYMENT_ID |
| XPAY02 | NO | APPLICATION_ID |
| XPAY03 | YES | RECEIPT_NUMBER |

### ISSUED_LICENSE
| Index | Unique | Columns |
|---|---|---|
| XLIC01 | YES | LICENSE_ID |
| XLIC02 | YES | LICENSE_NUMBER |
| XLIC03 | NO | CANDIDATE_ID, LICENSE_TYPE, LICENSE_STATUS |
| XLIC04 | NO | APPLICATION_ID |

### AUTHORITY_USER
| Index | Unique | Columns |
|---|---|---|
| XAUTH01 | YES | AUTHORITY_USER_ID |
| XAUTH02 | YES | USER_CODE |
| XAUTH03 | NO | AUTHORITY_LEVEL, ACTIVE_STATUS |

### LICENSE_FEE_SCHEDULE
| Index | Unique | Columns |
|---|---|---|
| XFEE01 | YES | FEE_SCHEDULE_ID |
| XFEE02 | NO | LICENSE_TYPE, FEE_TYPE, ACTIVE_STATUS, EFFECTIVE_DATE |

---

## DB2 grants

| Grantee | Privilege | Tables |
|---|---|---|
| DLISAPPL | SELECT, INSERT, UPDATE, DELETE | All 7 tables |
| DLISCICS | SELECT, INSERT, UPDATE, DELETE | All 7 tables |

DLISAPPL is the batch job application ID; DLISCICS is the CICS region user ID.  
DB2 connection is via CICS DB2ENTRY `DLISDB2E`, plan `DLISPLAN`, thread limit 10.
