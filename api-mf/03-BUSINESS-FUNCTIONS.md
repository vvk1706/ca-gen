# DLIS — Business Functions

Each section corresponds to one COBOL program. Functions are the named paragraphs that perform discrete business operations.

---

## DLISCAND — Candidate Maintenance
**CICS Transaction:** `DL02`  **BMS Map:** `DLISCM / DLISCM01`

### CREATE-CANDIDATE
Creates a new candidate record.

**Inputs:** firstName, lastName, dateOfBirth, idNumber, addressLine1, addressLine2, city, stateProvince, postalCode, country, phoneNumber, emailAddress

**Validations:**
- firstName, lastName, idNumber are mandatory.
- `idNumber` must be unique across active candidates (`RECORD_STATUS = 'A'`).

**Outputs:** candidateId (system-generated)

**DB operation:** `INSERT INTO DLIS.CANDIDATE`

---

### UPDATE-CANDIDATE
Updates contact and address details for an existing candidate.

**Inputs:** candidateId *(required)*, firstName, lastName, addressLine1, addressLine2, city, stateProvince, postalCode, country, phoneNumber, emailAddress, recordStatus

**Validations:**
- candidateId must exist in the database.

**Outputs:** success message

**DB operation:** `UPDATE DLIS.CANDIDATE`

---

### INQUIRE-CANDIDATE
Retrieves a candidate record by candidateId **or** idNumber.

**Inputs:** candidateId *(or)* idNumber — at least one required

**Outputs:** All candidate fields

**DB operation:** `SELECT` with `FETCH FIRST 1 ROW ONLY` (candidateId OR idNumber match)

---

### DEACTIVATE-CANDIDATE
Sets a candidate's `RECORD_STATUS` to `'I'` (Inactive).

**Inputs:** candidateId *(required)*

**Validations:**
- candidateId must exist.

**Outputs:** success message

**DB operation:** `UPDATE DLIS.CANDIDATE SET RECORD_STATUS = 'I'`

---

## DLISAPPL — License Application Entry
**CICS Transaction:** `DL03`  **BMS Map:** `DLISAE / DLISAE01`

### LOOKUP-CANDIDATE
Resolves a candidateId to a candidate name for display purposes. Also validates that the candidate record is active.

**Inputs:** candidateId

**Validations:**
- Candidate must exist and `RECORD_STATUS` must not be `'I'`.

**Outputs:** candidateName (firstName + lastName), message

---

### CALC-FEE
Looks up the current initial fee for a given licence type from the fee schedule.

**Inputs:** licenseType (`L`, `P`, or `O`)

**Validations:**
- A fee schedule row must exist with `FEE_TYPE = 'IF'`, `ACTIVE_STATUS = 'A'`, and `EFFECTIVE_DATE <= CURRENT DATE` and (`EXPIRY_DATE IS NULL OR EXPIRY_DATE >= CURRENT DATE`).

**Outputs:** feeAmount, currencyCode

**DB operation:** `SELECT` from `DLIS.LICENSE_FEE_SCHEDULE`

---

### SUBMIT-APPLICATION
Creates a new licence application for a candidate.

**Inputs:** candidateId, licenseType (`L`, `P`, or `O`)

**Validations:**
- candidateId is mandatory.
- licenseType must be `L`, `P`, or `O`.
- No active (non-`RE`, non-`IS`) application may already exist for the same candidate + licenseType combination.

**Business logic:**
- Application is inserted with `APPLICATION_STATUS = 'PE'` (Pending).

**Outputs:** applicationId (system-generated), applicationStatus (`PE`), applicationDate

**DB operations:**
1. `SELECT COUNT(*)` to check for existing active application.
2. `INSERT INTO DLIS.LICENSE_APPLICATION`
3. `SELECT IDENTITY_VAL_LOCAL()` to retrieve generated key.

---

## DLISSTAT — Application Status Inquiry
**CICS Transaction:** `DL04`  **BMS Map:** `DLISST / DLISST01`

### INQUIRE-STATUS
Retrieves the full current status of a licence application, including all gate statuses.

**Inputs:** applicationId

**Outputs:** licenseType, applicationStatus (code + human-readable description), eligCheckStatus + date + notes, histCheckStatus + date + notes, paymentStatus + reference, approval1Status + date + authority, approval2Status + date + authority, licenseNumber + expiryDate (if `IS`)

**DB operations:**
1. `SELECT` from `DLIS.LICENSE_APPLICATION`.
2. If `APPLICATION_STATUS = 'IS'`: additional `SELECT` from `DLIS.ISSUED_LICENSE` for licenseNumber and expiryDate.

---

## DLISELIG — Eligibility Check
**CICS Transaction:** `DL05`  **BMS Map:** `DLISEC / DLISEC01`

### LOAD-APPLICATION (Eligibility)
Loads application header plus candidate's date of birth and displays minimum age requirement for the licence type.

**Inputs:** applicationId

**Outputs:** candidateName, dateOfBirth, licenseType, licenseTypeDescription, minimumAgeRequired

---

### RUN-ELIGIBILITY-CHECK
Performs all eligibility rules against a licence application and records the result.

**Inputs:** applicationId, checkedBy (operator code)

**Business rules applied:**
1. **Age check:**
   - Learner (`L`): candidate must be ≥ 16 years old.
   - Probation (`P`): candidate must be ≥ 17 years old.
   - Open (`O`): candidate must be ≥ 18 years old.
2. **Upgrade path — Probation (`P`):** Candidate must hold an active (`LICENSE_STATUS = 'A'`) Learner licence in `DLIS.ISSUED_LICENSE`.
3. **Upgrade path — Open (`O`):** Candidate must hold an active (`LICENSE_STATUS = 'A'`) Probation licence in `DLIS.ISSUED_LICENSE`.

**Outcomes:**
- **Pass:** `ELIG_CHECK_STATUS = 'P'`, `APPLICATION_STATUS → 'EC'`
- **Fail:** `ELIG_CHECK_STATUS = 'F'`, `APPLICATION_STATUS → 'RE'`, failure reason recorded in `ELIG_CHECK_NOTES`

**DB operations:**
1. Load application (`SELECT` from `LICENSE_APPLICATION JOIN CANDIDATE`).
2. For upgrade checks: `SELECT COUNT(*)` from `DLIS.ISSUED_LICENSE`.
3. `UPDATE DLIS.LICENSE_APPLICATION` with check result.

---

## DLISHIST — Driving History Check
**CICS Transaction:** `DL06`  **BMS Map:** `DLISHC / DLISHC01`

### LOAD-APPLICATION (History)
Loads application header and simultaneously fetches and displays the candidate's full driving history, scrollable in 7-row pages.

**Inputs:** applicationId

**Outputs:** candidateName, licenseType, driving history rows (incidentDate, incidentType, incidentDesc), totalDemeritPoints, activeSuspensionFlag, unpaidFineFlag

---

### RUN-HISTORY-CHECK
Evaluates the candidate's driving record and records a pass/fail result.

**Prerequisite:** `ELIG_CHECK_STATUS` must be `'P'` (eligibility check must have been passed first).

**Business rules applied:**
1. **Active suspension/disqualification:** Any `INCIDENT_TYPE IN ('SU','DQ')` where `SUSP_END_DATE >= CURRENT DATE` or is null → **FAIL**.
2. **Demerit threshold:** Total demerit points across all active history records > 12 → **FAIL**.
3. **Unpaid fines:** Any history record with `FINE_AMOUNT > 0` and `FINE_PAID_STATUS = 'N'` → **FAIL**.

**Outcomes:**
- **Pass:** `HIST_CHECK_STATUS = 'P'`, `APPLICATION_STATUS → 'HC'`
- **Fail:** `HIST_CHECK_STATUS = 'F'`, `APPLICATION_STATUS → 'RE'`, reason in `HIST_CHECK_NOTES`

**DB operations:**
1. Verify eligibility check status.
2. Cursor fetch: `SELECT` all active rows from `DLIS.DRIVING_HISTORY` ordered by `INCIDENT_DATE DESC`.
3. `UPDATE DLIS.LICENSE_APPLICATION` with result.

---

## DLISPAY — Payment Entry and Processing
**CICS Transaction:** `DL07`  **BMS Map:** `DLISPE / DLISPE01`

### LOAD-APPLICATION (Payment)
Loads the application header and retrieves the current applicable fee from the fee schedule for display.

**Inputs:** applicationId

**Outputs:** candidateName, licenseType, licenseTypeDescription, feeAmount, currencyCode

---

### PROCESS-PAYMENT
Records a fee payment against a licence application.

**Prerequisite:** `HIST_CHECK_STATUS` must be `'P'` (history check must have been passed). Payment must not already be recorded (`PAYMENT_STATUS ≠ 'P'`).

**Inputs:** applicationId, paymentMethod, paymentReference, processedBy

**Accepted payment methods:** `CC` (Credit Card), `DC` (Debit Card), `EF` (EFT), `CS` (Cash), `CH` (Cheque)

**Business logic:**
- Receipt number generated as: `'RCP' + YYYYMMDD + applicationId`
- Payment amount taken from fee schedule at time of processing.
- Application updated: `PAYMENT_STATUS → 'P'`, `APPLICATION_STATUS → 'PA'`.

**Outputs:** paymentId, receiptNumber, paymentStatus

**DB operations:**
1. `INSERT INTO DLIS.PAYMENT`
2. `SELECT IDENTITY_VAL_LOCAL()`
3. `UPDATE DLIS.LICENSE_APPLICATION`

---

### PRINT-RECEIPT
Triggers printing of a payment receipt by linking to program `DLISRPRT`.

**Prerequisite:** Payment must have been processed in the current session (receiptNumber must be populated on screen).

---

## DLISAP1 — First Authority Approval
**CICS Transaction:** `DL08`  **BMS Map:** `DLISA1 / DLISA1M01`

### VALIDATE-AUTHORITY (Level 1)
Validates that a user code exists as an active authority user at level 1.

**Inputs:** authorityUserCode

**Validation:**
- `USER_CODE` must exist in `DLIS.AUTHORITY_USER` with `ACTIVE_STATUS = 'A'` and `AUTHORITY_LEVEL = '1'`.

**Outputs:** userName, authorityName

---

### SUBMIT-APPROVAL1
Records the first authority approval or rejection of a licence application.

**Prerequisites (all must be satisfied):**
1. `PAYMENT_STATUS = 'P'` — payment must be completed before first approval.
2. Authority user must be valid at level 1.
3. Authority user's `LIC_TYPES_AUTH` must include the application's `LICENSE_TYPE`.

**Inputs:** applicationId, authorityUserCode, decision (`A`=Approve / `R`=Reject), decisionNotes

**Business logic:**
- **Approve:** `APPROVAL_1_STATUS → 'A'`, `APPLICATION_STATUS → 'A2'` (awaiting second approval).
- **Reject:** `APPROVAL_1_STATUS → 'R'`, `APPLICATION_STATUS → 'RE'`.

**DB operation:** `UPDATE DLIS.LICENSE_APPLICATION`

---

## DLISAP2 — Second Authority Approval
**CICS Transaction:** `DL09`  **BMS Map:** `DLISA2 / DLISA2M01`

### VALIDATE-AUTHORITY (Level 2)
Validates that a user code exists as an active authority user at level 2.

**Inputs:** authorityUserCode

**Validation:**
- `USER_CODE` must exist in `DLIS.AUTHORITY_USER` with `ACTIVE_STATUS = 'A'` and `AUTHORITY_LEVEL = '2'`.

**Outputs:** userName, authorityName

---

### SUBMIT-APPROVAL2
Records the second (final) authority approval or rejection of a licence application.

**Prerequisites (all must be satisfied):**
1. `APPROVAL_1_STATUS = 'A'` — first approval must be granted.
2. Authority user must be valid at level 2.
3. **Segregation of duties:** The second approver's `AUTHORITY_NAME` must differ from the first approver's `APPROVAL_1_AUTHORITY` — the same authority cannot provide both approvals.
4. Authority user's `LIC_TYPES_AUTH` must include the application's `LICENSE_TYPE`.

**Inputs:** applicationId, authorityUserCode, decision (`A`=Approve / `R`=Reject), decisionNotes

**Business logic:**
- **Approve:** `APPROVAL_2_STATUS → 'A'`, `APPLICATION_STATUS → 'AP'` (ready for issue).
- **Reject:** `APPROVAL_2_STATUS → 'R'`, `APPLICATION_STATUS → 'RE'`.

**DB operation:** `UPDATE DLIS.LICENSE_APPLICATION`

---

## DLISISSU — Licence Issuance
**CICS Transaction:** `DL10`  **BMS Map:** `DLISLI / DLISLIM01`

### LOAD-APPLICATION (Issuance)
Loads full application summary including all gate statuses for final review before issue.

**Inputs:** applicationId

**Outputs:** candidateName, licenseType, eligCheckStatus, histCheckStatus, paymentStatus, approval1Status, approval2Status, applicationStatus

---

### ISSUE-LICENSE
Issues the physical driver licence and creates the `ISSUED_LICENSE` record.

**Prerequisite:** `APPLICATION_STATUS = 'AP'` (fully approved). All five gates must be satisfied: eligibility ✓, history ✓, payment ✓, approval 1 ✓, approval 2 ✓.

**Inputs:** applicationId, vehicleClass (`A`/`B`/`C`/`D`), issuingOfficer, issuingAuthority, restrictions

**Business logic:**
- Expiry date calculated from issue date:
  - Learner (`L`): +1 year
  - Probation (`P`): +2 years
  - Open (`O`): +5 years
- Licence number generated as: `{licPrefix}{vehicleClass}{candidateId}{applicationId}` where licPrefix = `LRN` (L) / `PRB` (P) / `OPN` (O)
- Demerit balance initialised to `12`.
- Application updated to `APPLICATION_STATUS → 'IS'`.

**Outputs:** licenseNumber, expiryDate

**DB operations:**
1. `INSERT INTO DLIS.ISSUED_LICENSE`
2. `UPDATE DLIS.LICENSE_APPLICATION SET APPLICATION_STATUS = 'IS'`

---

### PRINT-LICENSE
Triggers printing of the issued licence by linking to program `DLISLPRT`.

**Prerequisite:** Licence must have been issued in the current session (licenseNumber must be populated on screen).
