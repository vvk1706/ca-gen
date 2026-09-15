# 05 — Business Rules Catalogue
## Driver License Issuance System (DLIS)

---

## 1. License Type Rules

### BR-01 — License Type Values
**Rule:** The `LICENSE-TYPE` field must contain one of: `L` (Learner), `P` (Probation), `O` (Open).
**Enforced by:** `AB-CREATE-APPLICATION`, entity validation
**Error:** "INVALID LICENSE TYPE. MUST BE L, P OR O"

### BR-02 — Minimum Age — Learner License
**Rule:** A candidate applying for a Learner license must be at least **16 years old** at time of application.
**Enforced by:** `AB-CHECK-ELIGIBILITY`
**Error:** "CANDIDATE MUST BE AT LEAST 16 YEARS OLD FOR LEARNER LICENSE"

### BR-03 — Minimum Age — Probation License
**Rule:** A candidate applying for a Probation license must be at least **17 years old** at time of application.
**Enforced by:** `AB-CHECK-ELIGIBILITY`
**Error:** "CANDIDATE MUST BE AT LEAST 17 YEARS OLD FOR PROBATION LICENSE"

### BR-04 — Minimum Age — Open License
**Rule:** A candidate applying for an Open license must be at least **18 years old** at time of application.
**Enforced by:** `AB-CHECK-ELIGIBILITY`
**Error:** "CANDIDATE MUST BE AT LEAST 18 YEARS OLD FOR OPEN LICENSE"

### BR-05 — Upgrade Path: Learner → Probation
**Rule:** A candidate may only apply for a **Probation** license if they currently hold an **active Learner** license.
**Enforced by:** `AB-CHECK-ELIGIBILITY`
**Error:** "CANDIDATE MUST HOLD AN ACTIVE LEARNER LICENSE TO APPLY FOR PROBATION"

### BR-06 — Upgrade Path: Probation → Open
**Rule:** A candidate may only apply for an **Open** license if they currently hold an **active Probation** license.
**Enforced by:** `AB-CHECK-ELIGIBILITY`
**Error:** "CANDIDATE MUST HOLD AN ACTIVE PROBATION LICENSE TO APPLY FOR OPEN"

---

## 2. Candidate Rules

### BR-07 — Unique National ID
**Rule:** No two active candidates may share the same `ID-NUMBER`.
**Enforced by:** `AB-CREATE-CANDIDATE`
**Error:** "CANDIDATE WITH THIS ID NUMBER ALREADY EXISTS"

### BR-08 — Date of Birth Must Be in Past
**Rule:** `DATE-OF-BIRTH` must be strictly less than the current date.
**Enforced by:** `AB-CREATE-CANDIDATE`
**Error:** "DATE OF BIRTH MUST BE IN THE PAST"

### BR-09 — No Duplicate Active Application
**Rule:** A candidate cannot have more than one active application for the same license type simultaneously. Applications in status `RE` (Rejected) or `IS` (Issued) do not count.
**Enforced by:** `AB-CREATE-APPLICATION`
**Error:** "AN ACTIVE APPLICATION ALREADY EXISTS FOR THIS LICENSE TYPE"

---

## 3. History Check Rules

### BR-10 — Active Suspension Disqualifies
**Rule:** If a candidate has a `DRIVING-HISTORY` record of type `SU` or `DQ` where `SUSPENSION-END-DATE` is null or in the future, the history check must fail.
**Enforced by:** `AB-CHECK-HISTORY`
**Error:** "CANDIDATE IS UNDER AN ACTIVE SUSPENSION OR DISQUALIFICATION"

### BR-11 — Demerit Points Threshold
**Rule:** The sum of all active demerit points from a candidate's history must not exceed **12**. Exceeding this threshold fails the history check.
**Enforced by:** `AB-CHECK-HISTORY`
**Error:** "TOTAL DEMERIT POINTS EXCEED ALLOWABLE THRESHOLD OF 12"

### BR-12 — Unpaid Fines Disqualify
**Rule:** Any active `DRIVING-HISTORY` record with `FINE-AMOUNT > 0` and `FINE-PAID-STATUS = 'N'` fails the history check.
**Enforced by:** `AB-CHECK-HISTORY`
**Error:** "CANDIDATE HAS OUTSTANDING UNPAID FINES"

---

## 4. Sequential Gate Rules

### BR-13 — Eligibility Before History
**Rule:** The history check cannot be executed until the eligibility check has been **passed** (`ELIGIBILITY-CHECK-STATUS = 'P'`).
**Enforced by:** `AB-CHECK-HISTORY`
**Error:** "ELIGIBILITY CHECK MUST BE PASSED BEFORE HISTORY CHECK"

### BR-14 — History Before Payment
**Rule:** Payment cannot be processed until the history check has been **passed** (`HISTORY-CHECK-STATUS = 'P'`).
**Enforced by:** `AB-PROCESS-PAYMENT`
**Error:** "HISTORY CHECK MUST BE PASSED BEFORE PAYMENT"

### BR-15 — Payment Before First Approval
**Rule:** First authority approval cannot be submitted until payment has been **completed** (`PAYMENT-STATUS = 'P'`).
**Enforced by:** `AB-RECORD-APPROVAL-1`
**Error:** "PAYMENT MUST BE COMPLETED BEFORE FIRST APPROVAL"

### BR-16 — First Approval Before Second Approval
**Rule:** Second authority approval cannot be submitted until first approval has been **granted** (`APPROVAL-1-STATUS = 'A'`).
**Enforced by:** `AB-RECORD-APPROVAL-2`
**Error:** "FIRST APPROVAL MUST BE GRANTED BEFORE SECOND APPROVAL"

### BR-17 — All Gates Before Issuance
**Rule:** A license cannot be issued unless the application has `APPLICATION-STATUS = 'AP'`. The issuance action block independently re-checks all five gates as a final safety guard.
**Enforced by:** `AB-ISSUE-LICENSE`
**Error:** "APPLICATION MUST BE FULLY APPROVED (STATUS=AP) BEFORE LICENSE CAN BE ISSUED"

---

## 5. Approval Rules

### BR-18 — Approver Must Be Active
**Rule:** The `AUTHORITY-USER` record for any approver must have `ACTIVE-STATUS = 'A'`.
**Enforced by:** `AB-RECORD-APPROVAL-1`, `AB-RECORD-APPROVAL-2`
**Error:** "AUTHORITY USER NOT FOUND OR NOT ACTIVE AT LEVEL 1/2"

### BR-19 — Approver Level Enforcement
**Rule:** First approval requires `AUTHORITY-LEVEL = '1'`. Second approval requires `AUTHORITY-LEVEL = '2'`. An officer cannot approve at the wrong level.
**Enforced by:** `AB-RECORD-APPROVAL-1`, `AB-RECORD-APPROVAL-2`

### BR-20 — License Type Authorisation
**Rule:** An authority officer may only approve applications for the license types listed in their `LICENSE-TYPES-AUTHORISED` field (e.g., `"LPO"` = all types, `"L"` = Learner only).
**Enforced by:** `AB-RECORD-APPROVAL-1`, `AB-RECORD-APPROVAL-2`
**Error:** "AUTHORITY USER IS NOT AUTHORISED FOR THIS LICENSE TYPE"

### BR-21 — Segregation of Duties: Different Approving Authorities
**Rule:** The `APPROVAL-2-AUTHORITY` must differ from the `APPROVAL-1-AUTHORITY`. A second approver from the **same authority** as the first approver is not permitted. This enforces true dual-authority control.
**Enforced by:** `AB-RECORD-APPROVAL-2`
**Error:** "SECOND APPROVER MUST BE FROM A DIFFERENT AUTHORITY THAN FIRST APPROVER"

---

## 6. Payment Rules

### BR-22 — No Duplicate Payment
**Rule:** Once an application has `PAYMENT-STATUS = 'P'`, a second payment cannot be recorded against it.
**Enforced by:** `AB-PROCESS-PAYMENT`
**Error:** "PAYMENT HAS ALREADY BEEN RECORDED FOR THIS APPLICATION"

### BR-23 — Active Fee Schedule Required
**Rule:** A payment can only be processed if an active fee schedule entry exists for the relevant license type and `FEE-TYPE = 'IF'` (Issue Fee), with `EFFECTIVE-DATE <= CURRENT-DATE` and `EXPIRY-DATE` null or in the future.
**Enforced by:** `AB-PROCESS-PAYMENT`
**Error:** "NO ACTIVE FEE SCHEDULE FOUND FOR THIS LICENSE TYPE"

---

## 7. License Issuance Rules

### BR-24 — License Validity Periods
**Rule:** Expiry dates are calculated from the issue date as follows:

| License Type | Validity |
|---|---|
| L — Learner | 1 year |
| P — Probation | 2 years |
| O — Open | 5 years |

**Enforced by:** `AB-ISSUE-LICENSE`

### BR-25 — Valid Vehicle Class
**Rule:** `VEHICLE-CLASS` must be one of: `A` (Motorcycle), `B` (Light Vehicle), `C` (Heavy Vehicle), `D` (Bus/Coach).
**Enforced by:** `AB-ISSUE-LICENSE`
**Error:** "VEHICLE CLASS MUST BE A, B, C OR D"

### BR-26 — Starting Demerit Balance
**Rule:** All newly issued licenses are assigned a starting `DEMERIT-BALANCE` of **12**.
**Enforced by:** `AB-ISSUE-LICENSE`

---

## 8. Data Integrity Rules

### BR-27 — Candidate Active Status
**Rule:** Applications may only be created for candidates with `RECORD-STATUS = 'A'`.
**Enforced by:** `AB-CREATE-APPLICATION`

### BR-28 — Fee Amount Must Be Positive
**Rule:** `FEE-AMOUNT` in the fee schedule must be greater than zero.
**Enforced by:** Entity attribute validation

### BR-29 — Expiry Date After Issue Date
**Rule:** `EXPIRY-DATE` on an issued license must always be greater than `ISSUE-DATE`.
**Enforced by:** Entity attribute validation + `AB-ISSUE-LICENSE`

---

## 9. Business Rule Summary Matrix

| BR# | Area | Enforced By | Type |
|---|---|---|---|
| BR-01 | License Type | AB-CREATE-APPLICATION | Validation |
| BR-02 | Eligibility | AB-CHECK-ELIGIBILITY | Age Rule |
| BR-03 | Eligibility | AB-CHECK-ELIGIBILITY | Age Rule |
| BR-04 | Eligibility | AB-CHECK-ELIGIBILITY | Age Rule |
| BR-05 | Eligibility | AB-CHECK-ELIGIBILITY | Upgrade Path |
| BR-06 | Eligibility | AB-CHECK-ELIGIBILITY | Upgrade Path |
| BR-07 | Candidate | AB-CREATE-CANDIDATE | Uniqueness |
| BR-08 | Candidate | AB-CREATE-CANDIDATE | Validation |
| BR-09 | Application | AB-CREATE-APPLICATION | Uniqueness |
| BR-10 | History | AB-CHECK-HISTORY | Disqualifier |
| BR-11 | History | AB-CHECK-HISTORY | Threshold |
| BR-12 | History | AB-CHECK-HISTORY | Disqualifier |
| BR-13 | Workflow | AB-CHECK-HISTORY | Gate Control |
| BR-14 | Workflow | AB-PROCESS-PAYMENT | Gate Control |
| BR-15 | Workflow | AB-RECORD-APPROVAL-1 | Gate Control |
| BR-16 | Workflow | AB-RECORD-APPROVAL-2 | Gate Control |
| BR-17 | Workflow | AB-ISSUE-LICENSE | Gate Control |
| BR-18 | Approval | AB-RECORD-APPROVAL-1/2 | Authority |
| BR-19 | Approval | AB-RECORD-APPROVAL-1/2 | Authority |
| BR-20 | Approval | AB-RECORD-APPROVAL-1/2 | Authority |
| BR-21 | Approval | AB-RECORD-APPROVAL-2 | Segregation |
| BR-22 | Payment | AB-PROCESS-PAYMENT | Uniqueness |
| BR-23 | Payment | AB-PROCESS-PAYMENT | Data Required |
| BR-24 | Issuance | AB-ISSUE-LICENSE | Calculation |
| BR-25 | Issuance | AB-ISSUE-LICENSE | Validation |
| BR-26 | Issuance | AB-ISSUE-LICENSE | Default |
| BR-27 | Integrity | AB-CREATE-APPLICATION | Status Check |
| BR-28 | Integrity | Entity Validation | Constraint |
| BR-29 | Integrity | Entity + AB-ISSUE-LICENSE | Constraint |
