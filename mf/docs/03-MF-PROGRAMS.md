# 03 — COBOL Programs

Each program is pseudo-conversational:  
- On first entry (`EIBCALEN = 0`) → send map, return with `TRANSID`.  
- On subsequent entries → receive map, evaluate AID key, process, re-send map, return.

All programs COPY three shared copybooks:
- [`DLISWS.cpy`](../copybook/DLISWS.cpy) — common working storage  
- [`DLISCSEC.cpy`](../copybook/DLISCSEC.cpy) — CICS COMMAREA layouts  
- [`DLISDCLG.cpy`](../copybook/DLISDCLG.cpy) — DB2 host variable structs (where DB2 is used)

---

## DLISMENU — Main Menu

**Source:** [`cobol/DLISMENU.cbl`](../cobol/DLISMENU.cbl)  
**Transaction:** DL01  **Map:** DLISMM / DLISMM01  
**Copybooks:** DLISWS, DLISCSEC (no DB2)

### Logic

```
MAIN-LOGIC
    EIBCALEN = 0 → INIT-SCREEN
    else         → PROCESS-INPUT

INIT-SCREEN
    Set DMMDATE = CURRENT-DATE(1:10)
    SEND MAP DLISMM01 ERASE FREEKB
    RETURN TRANSID DL01

PROCESS-INPUT
    RECEIVE MAP
    EIBAID = PF12  → RETURN (exit to CICS)
    EIBAID ≠ ENTER → message + REDISPLAY-MENU
    DMMOPTI empty  → 'PLEASE SELECT AN OPTION (1-10)' + REDISPLAY

    EVALUATE DMMOPTI
        1/2  → XCTL DLISCAND
        3    → XCTL DLISAPPL
        4    → XCTL DLISSTAT
        5    → XCTL DLISELIG
        6    → XCTL DLISHIST
        7    → XCTL DLISPAY
        8    → XCTL DLISAP1
        9    → XCTL DLISAP2
        10   → XCTL DLISISSU
        OTHER→ 'INVALID OPTION' + REDISPLAY

REDISPLAY-MENU
    SEND MAP DATAONLY
    RETURN TRANSID DL01
```

**Notes:**  
- Options 1 and 2 both route to DLISCAND. Option 2 is "inquiry" — the user is expected to press F3 on the candidate screen rather than there being a separate transaction.

---

## DLISCAND — Candidate Maintenance

**Source:** [`cobol/DLISCAND.cbl`](../cobol/DLISCAND.cbl)  
**Transaction:** DL02  **Map:** DLISCM / DLISCM01  
**Copybooks:** DLISWS, DLISCSEC, DLISDCLG  **DB2 tables:** DLIS.CANDIDATE

### PF key routing

| Key | Action |
|---|---|
| F1 | CREATE-CANDIDATE |
| F2 | UPDATE-CANDIDATE |
| F3 | INQUIRE-CANDIDATE |
| F4 | DEACTIVATE-CANDIDATE |
| F12 | Exit |

### CREATE-CANDIDATE

**Validations:**
1. FIRST_NAME not blank.
2. LAST_NAME not blank.
3. ID_NUMBER not blank.
4. Duplicate check — SELECT on `DLIS.CANDIDATE` where `ID_NUMBER = :input AND RECORD_STATUS = 'A'`. If SQLCODE = 0 (row found) → reject with "ALREADY EXISTS".

**Insert:**  
- All screen fields mapped to DCLDLIS-CANDIDATE host variables.
- `RECORD_STATUS = 'A'`.
- `CREATED_BY = EIBTRMID`.
- After INSERT, `SELECT IDENTITY_VAL_LOCAL()` retrieves the generated CANDIDATE_ID and displays it in DCMCIDO.

### UPDATE-CANDIDATE

- Requires CANDIDATE_ID (DCMCIDI).
- Updates all editable fields (name, address, phone, email, status) via single `UPDATE … WHERE CANDIDATE_ID = :id`.
- SQLCODE 100 → "NOT FOUND".

### INQUIRE-CANDIDATE

- Accepts either CANDIDATE_ID or ID_NUMBER (national ID) as search key.
- `SELECT … WHERE CANDIDATE_ID = :id OR ID_NUMBER = :nid FETCH FIRST 1 ROW ONLY`.
- Populates all output fields on the map.

### DEACTIVATE-CANDIDATE

- Requires CANDIDATE_ID.
- `UPDATE DLIS.CANDIDATE SET RECORD_STATUS = 'I' WHERE CANDIDATE_ID = :id`.
- Does **not** delete the row — soft-delete only.

---

## DLISAPPL — Licence Application Entry

**Source:** [`cobol/DLISAPPL.cbl`](../cobol/DLISAPPL.cbl)  
**Transaction:** DL03  **Map:** DLISAE / DLISAE01  
**DB2 tables:** DLIS.CANDIDATE · DLIS.LICENSE_APPLICATION · DLIS.LICENSE_FEE_SCHEDULE

### PF key routing

| Key | Action |
|---|---|
| F1 | SUBMIT-APPLICATION |
| F3 | LOOKUP-CANDIDATE |
| F5 | CALC-FEE |
| F12 | Exit |

### LOOKUP-CANDIDATE (F3)

- SELECT `FIRST_NAME, LAST_NAME, RECORD_STATUS` from CANDIDATE where CANDIDATE_ID = input.
- Inactive candidates (`RECORD_STATUS = 'I'`) are rejected.
- Displays full name in DAECNAMO.

### CALC-FEE (F5)

- Requires LICENSE_TYPE entered.
- SELECT from `DLIS.LICENSE_FEE_SCHEDULE` where `LICENSE_TYPE = :type AND FEE_TYPE = 'IF' AND ACTIVE_STATUS = 'A' AND EFFECTIVE_DATE <= CURRENT DATE AND (EXPIRY_DATE IS NULL OR EXPIRY_DATE >= CURRENT DATE)` `FETCH FIRST 1 ROW ONLY`.
- Displays formatted fee amount and currency.

### SUBMIT-APPLICATION (F1)

**Validations:**
1. CANDIDATE_ID required.
2. LICENSE_TYPE required and must be `L`, `P`, or `O`.
3. Duplicate check — COUNT(*) from `DLIS.LICENSE_APPLICATION` where `CANDIDATE_ID = :id AND LICENSE_TYPE = :type AND APPLICATION_STATUS NOT IN ('RE','IS')`. If count > 0 → "ACTIVE APPLICATION ALREADY EXISTS".

**Insert:**
- INSERT with `APPLICATION_STATUS = 'PE'` (Pending), `CREATED_BY = EIBTRMID`.
- After INSERT, retrieves APPLICATION_ID via `IDENTITY_VAL_LOCAL()`.
- Displays APPLICATION_ID, status `PE`, and current date.

---

## DLISSTAT — Application Status Inquiry

**Source:** [`cobol/DLISSTAT.cbl`](../cobol/DLISSTAT.cbl)  
**Transaction:** DL04  **Map:** DLISST / DLISST01  
**DB2 tables:** DLIS.LICENSE_APPLICATION · DLIS.ISSUED_LICENSE

### Logic

- ENTER key triggers INQUIRE-STATUS.
- Reads all status columns for the application in one SELECT.
- Translates `APPLICATION_STATUS` code to a human-readable description:

| Code | Description |
|---|---|
| PE | PENDING - AWAITING ELIGIBILITY CHECK |
| EC | ELIGIBILITY CHECKED - AWAITING HISTORY |
| HC | HISTORY CHECKED - AWAITING PAYMENT |
| PA | PAYMENT APPROVED - AWAITING AUTH 1 |
| A2 | AWAITING SECOND AUTHORITY APPROVAL |
| AP | FULLY APPROVED - READY FOR ISSUE |
| IS | LICENSE ISSUED |
| RE | REJECTED |

- If status is `IS`, performs a second SELECT on `DLIS.ISSUED_LICENSE` to retrieve `LICENSE_NUMBER` and `EXPIRY_DATE`.

---

## DLISELIG — Eligibility Check

**Source:** [`cobol/DLISELIG.cbl`](../cobol/DLISELIG.cbl)  
**Transaction:** DL05  **Map:** DLISEC / DLISEC01  
**DB2 tables:** DLIS.LICENSE_APPLICATION · DLIS.CANDIDATE · DLIS.ISSUED_LICENSE

### PF key routing

| Key | Action |
|---|---|
| F1 | RUN-ELIGIBILITY-CHECK |
| F3 | LOAD-APPLICATION |
| F12 | Exit |

### LOAD-APPLICATION (F3)

- JOIN `LICENSE_APPLICATION` with `CANDIDATE` on CANDIDATE_ID.
- Displays candidate name, DOB, licence type, minimum age required:
  - L → min age 16
  - P → min age 17
  - O → min age 18

### RUN-ELIGIBILITY-CHECK (F1)

Pre-condition: `DECCHKBYI` (Checked-By field) must be entered.

**Step 1 — Age calculation:**
```
WS-CALC-AGE = CURRENT-DATE(1:4) - DATE-OF-BIRTH(1:4)
```
(year subtraction only — simplified, does not adjust for exact birthday month/day)

**Step 2 — Age gate:**

| Licence | Min age | Fail reason |
|---|---|---|
| L | 16 | AGE BELOW 16 - LEARNER LICENSE REQUIRES MIN AGE 16 |
| P | 17 | AGE BELOW 17 - PROBATION LICENSE REQUIRES MIN AGE 17 |
| O | 18 | AGE BELOW 18 - OPEN LICENSE REQUIRES MIN AGE 18 |

**Step 3 — Upgrade path (if age gate passes):**

- Type P: COUNT(*) from `DLIS.ISSUED_LICENSE` where `CANDIDATE_ID = :id AND LICENSE_TYPE = 'L' AND LICENSE_STATUS = 'A'`. Must be ≥ 1.
- Type O: COUNT(*) from `DLIS.ISSUED_LICENSE` where `CANDIDATE_ID = :id AND LICENSE_TYPE = 'P' AND LICENSE_STATUS = 'A'`. Must be ≥ 1.

**Step 4 — Record result:**
- PASS → `ELIG_CHECK_STATUS = 'P'`, `APPLICATION_STATUS = 'EC'`
- FAIL → `ELIG_CHECK_STATUS = 'F'`, `APPLICATION_STATUS = 'RE'`, notes written to `ELIG_CHECK_NOTES`
- UPDATE written to `DLIS.LICENSE_APPLICATION`.

---

## DLISHIST — Driving History Check

**Source:** [`cobol/DLISHIST.cbl`](../cobol/DLISHIST.cbl)  
**Transaction:** DL06  **Map:** DLISHC / DLISHC01  
**DB2 tables:** DLIS.LICENSE_APPLICATION · DLIS.CANDIDATE · DLIS.DRIVING_HISTORY

### PF key routing

| Key | Action |
|---|---|
| F1 | RUN-HISTORY-CHECK |
| F3 | LOAD-APPLICATION |
| F7 | Scroll up (previous 7 rows) |
| F8 | Scroll down (next 7 rows) |
| F12 | Exit |

### LOAD-APPLICATION (F3)

JOIN call identical to DLISELIG. Triggers `LOAD-HISTORY-TABLE` and `DISPLAY-HISTORY`.

### LOAD-HISTORY-TABLE

Opens cursor `HISTCUR`:
```sql
SELECT INCIDENT_DATE, INCIDENT_TYPE, INCIDENT_DESC, DEMERIT_POINTS,
       FINE_AMOUNT, FINE_PAID_STATUS, SUSP_START_DATE, SUSP_END_DATE
FROM DLIS.DRIVING_HISTORY
WHERE CANDIDATE_ID = :id AND RECORD_STATUS = 'A'
ORDER BY INCIDENT_DATE DESC
```

Fetches up to 50 rows into an internal table `WS-HIST-TABLE`.  
Simultaneously accumulates:
- `WS-DEMERIT-TOTAL` — sum of all demerit points.
- `WS-SUSP-FLAG` — 'Y' if any `SU`/`DQ` incident has `SUSP_END_DATE >= TODAY` or null.
- `WS-FINE-FLAG` — 'Y' if any incident has a fine with `FINE_PAID_STATUS = 'N'`.

### DISPLAY-HISTORY

Displays a scrollable 7-line window from `WS-SCROLL-TOP` into `DHCHL1O`–`DHCHL7O`.  
F7 subtracts 7 from `WS-SCROLL-TOP` (min 1); F8 adds 7.

### RUN-HISTORY-CHECK (F1)

Pre-condition: `ELIG_CHECK_STATUS` must be `'P'` (eligibility must pass first).

**Fail conditions (evaluated in order, first match used):**

1. `WS-SUSP-FLAG = 'Y'` → "ACTIVE SUSPENSION OR DISQUALIFICATION ON FILE"
2. `WS-DEMERIT-TOTAL > 12` → "TOTAL DEMERIT POINTS EXCEED THRESHOLD OF 12"
3. `WS-FINE-FLAG = 'Y'` → "OUTSTANDING UNPAID FINES ON FILE"

**Result recording:**
- PASS → `HIST_CHECK_STATUS = 'P'`, `APPLICATION_STATUS = 'HC'`
- FAIL → `HIST_CHECK_STATUS = 'F'`, `APPLICATION_STATUS = 'RE'`
- UPDATE to `DLIS.LICENSE_APPLICATION`.

---

## DLISPAY — Payment Entry

**Source:** [`cobol/DLISPAY.cbl`](../cobol/DLISPAY.cbl)  
**Transaction:** DL07  **Map:** DLISPE / DLISPE01  
**DB2 tables:** DLIS.LICENSE_APPLICATION · DLIS.CANDIDATE · DLIS.LICENSE_FEE_SCHEDULE · DLIS.PAYMENT

### PF key routing

| Key | Action |
|---|---|
| F1 | PROCESS-PAYMENT |
| F3 | LOAD-APPLICATION |
| F5 | PRINT-RECEIPT |
| F12 | Exit |

### LOAD-APPLICATION (F3)

Retrieves `LICENSE_TYPE`, `HIST_CHECK_STATUS`, `PAYMENT_STATUS` plus candidate name.  
Auto-fetches applicable `IF` fee from `LICENSE_FEE_SCHEDULE`.

### PROCESS-PAYMENT (F1)

**Pre-conditions:**
1. APPLICATION_ID required.
2. `PAYMENT_METHOD` required; must be `CC`, `DC`, `EF`, `CS`, or `CH`.
3. `PAYMENT_REFERENCE` required.
4. `HIST_CHECK_STATUS` must be `'P'`.
5. `PAYMENT_STATUS` must not already be `'P'` (duplicate payment guard).

**Receipt number generation:**
```
WS-RECEIPT-NUMBER = 'RCP' + YYYYMMDD + APPLICATION_ID
```

**Insert:**
- INSERT into `DLIS.PAYMENT` with `PAYMENT_STATUS = 'S'` (Settled), `FEE_TYPE = 'IF'`.
- Retrieves generated `PAYMENT_ID` via `IDENTITY_VAL_LOCAL()`.

**Update application:**
- `UPDATE LICENSE_APPLICATION SET PAYMENT_STATUS = 'P', PAYMENT_REFERENCE = :ref, APPLICATION_STATUS = 'PA'`.

### PRINT-RECEIPT (F5)

- Requires receipt number already on screen.
- `EXEC CICS LINK PROGRAM('DLISRPRT')` — delegates to a print stub.

---

## DLISAP1 — First Authority Approval

**Source:** [`cobol/DLISAP1.cbl`](../cobol/DLISAP1.cbl)  
**Transaction:** DL08  **Map:** DLISA1 / DLISA1M01  
**DB2 tables:** DLIS.LICENSE_APPLICATION · DLIS.CANDIDATE · DLIS.AUTHORITY_USER

### PF key routing

| Key | Action |
|---|---|
| F1 | SUBMIT-APPROVAL1 |
| F3 | LOAD-APPLICATION |
| F4 | VALIDATE-AUTHORITY |
| F12 | Exit |

### LOAD-APPLICATION (F3)

Full application summary including all five gate statuses, check notes, and payment reference.

### VALIDATE-AUTHORITY (F4)

```sql
SELECT USER_NAME, AUTHORITY_NAME, AUTHORITY_LEVEL, LIC_TYPES_AUTH
FROM DLIS.AUTHORITY_USER
WHERE USER_CODE = :code AND ACTIVE_STATUS = 'A' AND AUTHORITY_LEVEL = '1'
```
- SQLCODE 100 → "AUTHORITY USER NOT FOUND OR NOT ACTIVE AT LEVEL 1".

### SUBMIT-APPROVAL1 (F1)

**Pre-conditions:**
1. APPLICATION_ID required.
2. Authority user code required.
3. Decision must be `A` (approve) or `R` (reject).
4. `PAYMENT_STATUS` must be `'P'`.
5. Authority validation must pass (calls VALIDATE-AUTHORITY internally).
6. `LIC_TYPES_AUTH` must contain the application's licence type.

**Decision recording:**
- `A` → `APPROVAL_1_STATUS = 'A'`, `APPLICATION_STATUS = 'A2'`
- `R` → `APPROVAL_1_STATUS = 'R'`, `APPLICATION_STATUS = 'RE'`
- Writes `APPROVAL_1_AUTHORITY`, `APPROVAL_1_DATE`, `APPROVAL_1_NOTES`.

---

## DLISAP2 — Second Authority Approval

**Source:** [`cobol/DLISAP2.cbl`](../cobol/DLISAP2.cbl)  
**Transaction:** DL09  **Map:** DLISA2 / DLISA2M01  
**DB2 tables:** DLIS.LICENSE_APPLICATION · DLIS.CANDIDATE · DLIS.AUTHORITY_USER

### PF key routing

Identical to DLISAP1 (F1/F3/F4/F12).

### VALIDATE-AUTHORITY (F4)

Same structure as DLISAP1 but queries `AUTHORITY_LEVEL = '2'`.

### SUBMIT-APPROVAL2 (F1)

**Pre-conditions (additions over DLISAP1):**
1. `APPROVAL_1_STATUS` must be `'A'` (first approval must precede second).
2. **Segregation check:** `AUTHORITY_NAME` of the Level-2 approver must **not** equal `WS-APPROVAL1-AUTH` (loaded from `APPROVAL_1_AUTHORITY` in the application row).

**Decision recording:**
- `A` → `APPROVAL_2_STATUS = 'A'`, `APPLICATION_STATUS = 'AP'` (fully approved)
- `R` → `APPROVAL_2_STATUS = 'R'`, `APPLICATION_STATUS = 'RE'`
- Writes `APPROVAL_2_AUTHORITY`, `APPROVAL_2_DATE`, `APPROVAL_2_NOTES`.

---

## DLISISSU — Licence Issuance

**Source:** [`cobol/DLISISSU.cbl`](../cobol/DLISISSU.cbl)  
**Transaction:** DL10  **Map:** DLISLI / DLISLIM01  
**DB2 tables:** DLIS.LICENSE_APPLICATION · DLIS.CANDIDATE · DLIS.ISSUED_LICENSE

### PF key routing

| Key | Action |
|---|---|
| F1 | ISSUE-LICENSE |
| F3 | LOAD-APPLICATION |
| F6 | PRINT-LICENSE |
| F12 | Exit |

### LOAD-APPLICATION (F3)

Retrieves all seven status indicators plus candidate name.

### ISSUE-LICENSE (F1)

**Pre-conditions:**
1. APPLICATION_ID required.
2. `VEHICLE_CLASS` required; must be `A`, `B`, `C`, or `D`.
3. `ISSUING_OFFICER` required.
4. `ISSUING_AUTHORITY` required.
5. `APPLICATION_STATUS` must be exactly `'AP'` — all five gates fully satisfied.

**Expiry date calculation:**
```
EXPIRY_YYYY = CURRENT_YYYY + years_valid
EXPIRY = EXPIRY_YYYY + CURRENT_MMDD
```
Years by type: L=1, P=2, O=5.

**Licence number generation:**
```
PREFIX + VEHICLE_CLASS + CANDIDATE_ID + APPLICATION_ID
LRN → Learner
PRB → Probation
OPN → Open
```

**Insert into ISSUED_LICENSE:**
- `LICENSE_STATUS = 'A'` (Active).
- `DEMERIT_BALANCE = 12` (starting balance, hardcoded).

**Update application:**
- `APPLICATION_STATUS = 'IS'` (Issued).

### PRINT-LICENSE (F6)

- Requires licence number already on screen.
- `EXEC CICS LINK PROGRAM('DLISLPRT')`.

---

## Shared copybooks

### DLISWS.cpy — Working storage

Defines reusable working storage areas, each as a separate 01-level:

| Group | Key fields |
|---|---|
| WS-COMMON-AREA | WS-RETURN-CODE, WS-USER-ID, current date components |
| WS-CANDIDATE | All candidate fields, full name |
| WS-APPLICATION | Application ID, type, all five gate statuses |
| WS-PAYMENT | Payment ID, method, ref, fee amount (numeric + display), receipt number |
| WS-ELIGIBILITY | Eligibility status, checked-by, calc age, fail reason, WS-FAIL-FLAG |
| WS-HISTORY | History status, total demerits, suspension flag, fine flag |
| WS-APPROVAL | Auth user code/name, decision, notes |
| WS-LICENSE | Licence ID/number, vehicle class, restrictions, expiry, prefix |
| WS-NULL-INDICATORS | DB2 null indicator fields for nullable columns |

### DLISCSEC.cpy — CICS communication areas

Three 01-levels (overlapping 350-byte DFHCOMMAREA):

- `DLIS-COMMAREA` — navigation, IDs, message, action flag.
- `DLIS-CAND-COMMAREA` — full candidate fields for passing between screens.
- `DLIS-APPL-COMMAREA` — application status summary for display.

Also defines `WS-CICS-RESPONSE` with WS-RESP1/RESP2 used in all EXEC CICS calls.

### DLISDCLG.cpy — DB2 host variable declarations

Six 01-level host variable structures (DCLGEN format):

| Structure | Maps to DB2 table |
|---|---|
| DCLDLIS-CANDIDATE | DLIS.CANDIDATE |
| DCLDLIS-LICENSE-APPL | DLIS.LICENSE_APPLICATION |
| DCLDLIS-DRIVING-HIST | DLIS.DRIVING_HISTORY |
| DCLDLIS-PAYMENT | DLIS.PAYMENT |
| DCLDLIS-ISSUED-LIC | DLIS.ISSUED_LICENSE |
| DCLDLIS-AUTH-USER | DLIS.AUTHORITY_USER |
| DCLDLIS-FEE-SCHED | DLIS.LICENSE_FEE_SCHEDULE |
