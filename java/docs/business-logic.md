# DLIS — Business Logic and Workflows

This document describes all business rules enforced by the DLIS system. Each rule is sourced directly from the EJB implementations in `dlis-core` and the domain model, which in turn derive from the original CA Gen action blocks (ABs).

---

## 1. License Types

| Code | Name | Minimum Age | Prerequisite |
|---|---|---|---|
| `L` | Learner | 16 years | None |
| `P` | Probation | 17 years | Active Learner (`L`) license |
| `O` | Open | 18 years | Active Probation (`P`) license |

> The upgrade-path requirement means a candidate cannot skip directly from Learner to Open. Each type must be held and active before applying for the next tier.

---

## 2. Application Status Lifecycle

An application progresses through the following statuses. Once an application is Rejected (`RE`) it is terminal and cannot be reactivated.

```
                  ┌──────────────────────────────────────────────┐
                  │                                              ↓
 [PE] ──────────▶ [EC] ──────────▶ [HC] ──────────▶ [PP] ──────▶ [PA]
Pending     Eligibility      History          Payment         Payment
           Checked           Checked          Pending         Approved
                                                                  │
                  ┌───────────────────────────────────────────────┘
                  ▼
               [A1] ──────────────▶ [A2] ──────────────▶ [AP] ──────▶ [IS]
          1st Approval          2nd Approval          Approved        Issued
             Pending               Pending
               │                     │
               ▼                     ▼
             [RE]                  [RE]
           Rejected              Rejected
```

### Status Code Reference

| Code | Name | Set By |
|---|---|---|
| `PE` | Pending | `createApplication` |
| `EC` | Eligibility Checked (passed) | `checkEligibility` |
| `HC` | History Checked (passed) | `checkHistory` |
| `PP` | Payment Pending | `checkHistory` (after passing) |
| `PA` | Payment Approved | `processPayment` |
| `A1` | 1st Approval Pending | `processPayment` (auto-advance) |
| `A2` | 2nd Approval Pending | `recordApproval1` (approved) |
| `AP` | Fully Approved | `recordApproval2` (approved) |
| `IS` | Issued | `issueLicense` |
| `RE` | Rejected | Any check/approval step that fails |

---

## 3. Candidate Management (AB-CREATE-CANDIDATE)

Candidates are Queensland, Australia residents. The `ID_NUMBER` field stores the candidate's Queensland driver licence number or Queensland proof-of-age card number (format: `1–8 digits`, e.g. `12345678`). The field is unique among active candidate records.

### Rules

1. **Unique ID Number** — An active candidate with the same Queensland ID number (`ID_NUMBER`) must not already exist. Return code `RC_DUPLICATE (1)` if violated.
2. **Date of Birth in the past** — `dateOfBirth` must be before today. A null or future DOB is rejected with `RC_INVALID_AGE (2)`.
3. `recordStatus` defaults to `A` (Active) on creation.
4. `createdDate` is set to the current date by the DAO.

### Lookup

- `findByIdNumber(idNumber)` — Returns only **active** candidates. Used at application creation and CICS inquiry.
- `findById(candidateId)` — Returns the candidate regardless of status (caller must check `isActive()`).

---

## 4. Application Creation (AB-CREATE-APPLICATION)

### Preconditions

| Check | Failure Code | Failure Message |
|---|---|---|
| Candidate exists and is active | `RC_NOT_FOUND (1)` | CANDIDATE NOT FOUND OR INACTIVE |
| License type is `L`, `P`, or `O` | `RC_INVALID_TYPE (2)` | INVALID LICENSE TYPE. MUST BE L, P OR O |
| No active application already exists for this candidate × license type | `RC_ALREADY_EXISTS (3)` | AN ACTIVE APPLICATION ALREADY EXISTS FOR THIS LICENSE TYPE |

### Outcome on success

- `applicationStatus = PE`
- `applicationDate = today`
- `eligibilityChkStatus = U`, `historyChkStatus = U`, `paymentStatus = U`
- Returns the generated `applicationId`.

---

## 5. Eligibility Check (AB-CHECK-ELIGIBILITY)

Performed by a licensing officer after creating the application.

### Age rules

| License Type | Minimum Age |
|---|---|
| `L` (Learner) | 16 |
| `P` (Probation) | 17 |
| `O` (Open) | 18 |

### Upgrade-path rule

| Applying for | Must hold active license of type |
|---|---|
| `P` (Probation) | `L` (Learner) |
| `O` (Open) | `P` (Probation) |

### Outcomes

| Result | `eligibilityChkStatus` | `applicationStatus` |
|---|---|---|
| Pass | `P` | `EC` |
| Fail (any rule) | `F` | `RE` (rejected) |

- Failure reason is stored in `eligibilityChkNotes`.
- History check cannot be run unless eligibility has passed (`eligibilityChkStatus = P`).

---

## 6. History Check (AB-CHECK-HISTORY)

### Preconditions

- Eligibility check must have passed (`eligibilityChkStatus = P`). If not, returns error code `2`.

### Rules evaluated against DRIVING_HISTORY records

| Rule | Failure Message |
|---|---|
| Any active suspension or disqualification | CANDIDATE IS UNDER AN ACTIVE SUSPENSION OR DISQUALIFICATION |
| Any unpaid fine (FINE_AMOUNT > 0, FINE_PAID_STATUS = N) | CANDIDATE HAS OUTSTANDING UNPAID FINES |
| Total demerit points across all active records > 12 | TOTAL DEMERIT POINTS EXCEED ALLOWABLE THRESHOLD OF 12 |

> **Active suspension/disqualification** means the history record has type `SU` or `DQ` and its `suspensionEndDate` is null or has not yet passed.

### Outcomes

| Result | `historyChkStatus` | `applicationStatus` |
|---|---|---|
| Pass | `P` | `HC` → advances to `PP` |
| Fail | `F` | `RE` (rejected) |

---

## 7. Payment Processing (AB-PROCESS-PAYMENT)

### Preconditions

- Application must be in `PP` (Payment Pending) status.
- A valid active fee schedule entry must exist for the given `licenseType` and `feeType`.

### Fee Determination

The fee is looked up from `LICENSE_FEE_SCHEDULE` by matching:
- `LICENSE_TYPE` = application's license type
- `FEE_TYPE` = `IF` (Issue Fee) for new applications
- `ACTIVE_STATUS = A`
- `EFFECTIVE_DATE <= today`
- `EXPIRY_DATE` is null or >= today

If no fee record is found, the service returns `RC_FEE_NOT_FOUND (2)`.

### Seeded Fee Schedule (effective 2024-01-01)

All amounts are in **Australian Dollars (AUD)**.

| License Type | Fee Type | Amount (AUD) | Description |
|---|---|---|---|
| L | IF | 25.00 | Learner License Issue Fee |
| L | RF | 20.00 | Learner License Renewal Fee |
| L | LF | 35.00 | Learner License Late Fee |
| P | IF | 50.00 | Probation License Issue Fee |
| P | RF | 40.00 | Probation License Renewal Fee |
| P | LF | 60.00 | Probation License Late Fee |
| O | IF | 75.00 | Open License Issue Fee |
| O | RF | 65.00 | Open License Renewal Fee |
| O | LF | 90.00 | Open License Late Fee |

### Payment Methods

| Code | Method |
|---|---|
| `CC` | Credit Card |
| `DC` | Debit Card |
| `EF` | Electronic Funds Transfer |
| `CS` | Cash |
| `CH` | Cheque |

### Payment Statuses

| Code | Meaning |
|---|---|
| `S` | Success |
| `F` | Failed |
| `R` | Refunded |
| `P` | Pending |

### Outcome on success

- A `PAYMENT` record is inserted with status `S` and a generated `receiptNumber`.
- `LICENSE_APPLICATION.PAYMENT_STATUS` is set to `P` (Paid).
- `LICENSE_APPLICATION.PAYMENT_REFERENCE` is set to the receipt number.
- `applicationStatus` advances from `PP` → `PA` → `A1`.

---

## 8. First Approval (AB-RECORD-APPROVAL-1)

### Preconditions

| Check | Failure |
|---|---|
| Decision is `A` (Approve) or `R` (Reject) | `RC_INVALID_DECISION (5)` |
| Application exists | `RC_NOT_FOUND (1)` |
| Payment is complete (`paymentStatus = P` or `W`) | `RC_PAYMENT_NOT_DONE (4)` |
| Authority user exists, is active, and has level `1` | `RC_NOT_FOUND (1)` |
| Authority user is authorised for the application's license type | `RC_NOT_AUTHORISED (3)` |

### Outcomes

| Decision | `approval1Status` | New `applicationStatus` |
|---|---|---|
| `A` (Approve) | `A` | `A2` (Second approval pending) |
| `R` (Reject) | `R` | `RE` (Rejected) |

---

## 9. Second Approval (AB-RECORD-APPROVAL-2)

### Preconditions

| Check | Failure |
|---|---|
| Decision is `A` or `R` | `RC_INVALID_DECISION (5)` |
| Application exists | `RC_NOT_FOUND (1)` |
| `applicationStatus = A2` | Error code `2` (first approval not yet granted) |
| Authority user exists, is active, and has level `2` | `RC_NOT_FOUND (1)` |
| Authority user is authorised for the application's license type | `RC_NOT_AUTHORISED (3)` |

### Outcomes

| Decision | `approval2Status` | New `applicationStatus` |
|---|---|---|
| `A` (Approve) | `A` | `AP` (Fully approved) |
| `R` (Reject) | `R` | `RE` (Rejected) |

> **Dual-authority requirement** — First and second approvals must be performed by different `AuthorityUser` records at their respective authority levels. The system enforces that a level-1 user cannot perform a level-2 approval and vice versa.

---

## 10. License Issuance (AB-ISSUE-LICENSE)

### Preconditions

- `applicationStatus` must be `AP` (Approved). If not, returns an error.

### Rules

1. A unique `licenseNumber` is generated (format: `DL-{candidateId}-{applicationId}-{yyyyMMdd}`).
2. `issueDate` = today.
3. `expiryDate` = issue date + validity period based on license type:
   - `L` (Learner): 1 year
   - `P` (Probation): 2 years
   - `O` (Open): 5 years
4. `demeritBalance` initialized to `12` (the demerit threshold limit).
5. `renewalCount` initialized to `0`.
6. `applicationStatus` is updated to `IS` (Issued).

### Outcome on success

- An `ISSUED_LICENSE` record is inserted.
- Returns the full `IssuedLicense` domain object including the generated `licenseNumber` and `expiryDate`.

---

## 11. Authority User Rules

- `authorityLevel = 1` → First Approver.
- `authorityLevel = 2` → Second Approver.
- `licenseTypesAuthorised` is a compact string (e.g. `"LPO"`, `"L"`, `"PO"`) — the authority user may only approve applications for license types contained in this string.
- `activeStatus` must be `A` at the time of the approval action.

---

## 12. Driving History Rules

| Incident Type | Code | Effect on History Check |
|---|---|---|
| Offence | `OF` | Contributes demerit points; unpaid fines block application |
| Accident | `AC` | Contributes demerit points; unpaid fines block application |
| Suspension | `SU` | Active suspension blocks application |
| Disqualification | `DQ` | Active disqualification blocks application |

**Demerit threshold**: Total demerit points across all active history records must not exceed **12**.

---

## 13. ServiceResult Return Codes

All service methods return a `ServiceResult<T>` with a numeric return code. The REST layer maps these to HTTP status codes.

| Constant | Code | REST Status | Meaning |
|---|---|---|---|
| `RC_OK` | 0 | 200/201 | Success |
| `RC_NOT_FOUND` | 1 | 404 | Record not found |
| `RC_DUPLICATE` | 1 | 422 | Duplicate record |
| `RC_INVALID_AGE` | 2 | 422 | Invalid age |
| `RC_INVALID_TYPE` | 2 | 422 | Invalid license/fee type |
| `RC_FEE_NOT_FOUND` | 2 | 422 | Fee schedule not found |
| `RC_ALREADY_EXISTS` | 3 | 422 | Active application already exists |
| `RC_ALREADY_PAID` | 3 | 422 | Payment already recorded |
| `RC_NOT_AUTHORISED` | 3 | 422 | Authority not authorised for license type |
| `RC_HISTORY_NOT_PASSED` | 4 | 422 | History check prerequisite not met |
| `RC_PAYMENT_NOT_DONE` | 4 | 422 | Payment prerequisite not met |
| `RC_INVALID_DECISION` | 5 | 422 | Approval decision must be A or R |
| `RC_ERROR` | 99 | 500 | Unexpected system error |
