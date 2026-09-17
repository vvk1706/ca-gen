# 04 — Action Block Reference
## Driver License Issuance System (DLIS)

---

## Overview

Action Blocks are the executable logic units of the DLIS CA Gen model. Each Action Block is self-contained, receives data via **Imports**, performs validation and entity operations via **Views**, and returns results via **Exports** including a standardised return code.

**Return Code Convention:**

| Code | Meaning |
|---|---|
| 0 | Success |
| 1 | Primary entity not found |
| 2 | Secondary validation failure |
| 3 | Duplicate / already processed |
| 4 | Prerequisite step not complete |
| 5 | Segregation of duties violation |
| 6 | Invalid input value |
| 99 | Unexpected / system error |

---

## AB-CREATE-CANDIDATE

**File:** [`src/action-blocks/AB-CREATE-CANDIDATE.ACB`](../src/action-blocks/AB-CREATE-CANDIDATE.ACB)
**Process:** `PROC-CREATE-CANDIDATE`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-FIRST-NAME | CHARACTER(30) | Yes | First name |
| WS-LAST-NAME | CHARACTER(30) | Yes | Last name |
| WS-DATE-OF-BIRTH | DATE | Yes | Date of birth |
| WS-ID-NUMBER | CHARACTER(20) | Yes | National ID / Passport |
| WS-ADDRESS-LINE-1 | CHARACTER(50) | Yes | Street address |
| WS-ADDRESS-LINE-2 | CHARACTER(50) | No | Additional address line |
| WS-CITY | CHARACTER(30) | Yes | City |
| WS-STATE-PROVINCE | CHARACTER(30) | Yes | State/Province |
| WS-POSTAL-CODE | CHARACTER(10) | No | Postal / ZIP code |
| WS-COUNTRY | CHARACTER(30) | Yes | Country |
| WS-PHONE-NUMBER | CHARACTER(15) | No | Contact phone |
| WS-EMAIL-ADDRESS | CHARACTER(60) | No | Contact email |
| WS-CREATED-BY | CHARACTER(20) | Yes | User ID |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-CANDIDATE-ID | NUMERIC(10) | Assigned candidate ID |
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=Dup ID, 2=Invalid Age |
| WS-RETURN-MSG | CHARACTER(100) | Result message |

### Logic Summary
1. Check for existing active candidate with same `ID-NUMBER` → RC=1 if found
2. Validate `DATE-OF-BIRTH` is in the past → RC=2 if not
3. Create `CANDIDATE` record with `RECORD-STATUS='A'`

---

## AB-CREATE-APPLICATION

**File:** [`src/action-blocks/AB-CREATE-APPLICATION.ACB`](../src/action-blocks/AB-CREATE-APPLICATION.ACB)
**Process:** `PROC-CREATE-APPLICATION`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-CANDIDATE-ID | NUMERIC(10) | Yes | Candidate ID |
| WS-LICENSE-TYPE | CHARACTER(1) | Yes | L, P, or O |
| WS-CREATED-BY | CHARACTER(20) | Yes | User ID |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | New application ID |
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=Invalid Candidate, 2=Invalid Type, 3=Already Pending |
| WS-RETURN-MSG | CHARACTER(100) | Result message |

### Logic Summary
1. Validate candidate exists and `RECORD-STATUS='A'`
2. Validate `LICENSE-TYPE` in (`L`, `P`, `O`)
3. Check no existing active application for same candidate + type
4. Create `LICENSE-APPLICATION` with `APPLICATION-STATUS='PE'`

---

## AB-CHECK-ELIGIBILITY

**File:** [`src/action-blocks/AB-CHECK-ELIGIBILITY.ACB`](../src/action-blocks/AB-CHECK-ELIGIBILITY.ACB)
**Process:** `PROC-CHECK-AGE-ELIGIBILITY`, `PROC-CHECK-EXISTING-LICENSE`, `PROC-RECORD-ELIGIBILITY-RESULT`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | Yes | Application ID |
| WS-CHECKED-BY | CHARACTER(20) | Yes | Officer user ID |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-ELIGIBILITY-STATUS | CHARACTER(1) | P=Pass, F=Fail |
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=App Not Found |
| WS-RETURN-MSG | CHARACTER(100) | Result or failure reason |

### Logic Summary
1. Read application and candidate
2. Calculate age = (CurrentDate − DateOfBirth) / 365
3. Check minimum age by license type (L≥16, P≥17, O≥18)
4. For P and O: verify an active license of the prerequisite type exists
5. Record PASS or FAIL on application; set status `EC` or `RE`

---

## AB-CHECK-HISTORY

**File:** [`src/action-blocks/AB-CHECK-HISTORY.ACB`](../src/action-blocks/AB-CHECK-HISTORY.ACB)
**Process:** `PROC-RETRIEVE-DRIVING-HISTORY`, `PROC-CHECK-ACTIVE-SUSPENSION`, `PROC-CHECK-DEMERIT-POINTS`, `PROC-CHECK-UNPAID-FINES`, `PROC-RECORD-HISTORY-RESULT`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | Yes | Application ID |
| WS-CHECKED-BY | CHARACTER(20) | Yes | Officer user ID |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-HISTORY-STATUS | CHARACTER(1) | P=Pass, F=Fail |
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=App Not Found, 2=Eligibility Not Passed |
| WS-RETURN-MSG | CHARACTER(100) | Result or failure reason |

### Logic Summary
1. Verify eligibility check was passed (`RC=2` if not)
2. Scan all active `DRIVING-HISTORY` records for candidate
3. Check for active suspension/disqualification (end date null or future)
4. Accumulate demerit points; fail if total > 12
5. Check for any unpaid fines
6. Record PASS or FAIL; set status `HC` or `RE`

---

## AB-PROCESS-PAYMENT

**File:** [`src/action-blocks/AB-PROCESS-PAYMENT.ACB`](../src/action-blocks/AB-PROCESS-PAYMENT.ACB)
**Process:** `PROC-CALCULATE-FEE`, `PROC-RECORD-PAYMENT`, `PROC-VERIFY-PAYMENT`, `PROC-GENERATE-RECEIPT`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | Yes | Application ID |
| WS-CANDIDATE-ID | NUMERIC(10) | Yes | Candidate ID |
| WS-PAYMENT-METHOD | CHARACTER(2) | Yes | CC, DC, EF, CS, CH |
| WS-PAYMENT-REFERENCE | CHARACTER(30) | Yes | Payment reference |
| WS-PROCESSED-BY | CHARACTER(20) | Yes | Officer user ID |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-PAYMENT-ID | NUMERIC(10) | New payment record ID |
| WS-FEE-AMOUNT | NUMERIC(10,2) | Fee charged |
| WS-RECEIPT-NUMBER | CHARACTER(20) | Generated receipt number |
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=App Not Found, 2=Fee Not Found, 3=Already Paid, 4=History Not Passed |
| WS-RETURN-MSG | CHARACTER(100) | Result message |

### Logic Summary
1. Verify history check passed
2. Verify not already paid
3. Look up active fee from `LICENSE-FEE-SCHEDULE` for license type + `FEE-TYPE='IF'`
4. Create `PAYMENT` record; generate receipt number
5. Update application `PAYMENT-STATUS='P'`, `APPLICATION-STATUS='PA'`

---

## AB-RECORD-APPROVAL-1

**File:** [`src/action-blocks/AB-RECORD-APPROVAL-1.ACB`](../src/action-blocks/AB-RECORD-APPROVAL-1.ACB)
**Process:** `PROC-VALIDATE-AUTHORITY1-USER`, `PROC-RECORD-APPROVAL1`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | Yes | Application ID |
| WS-AUTHORITY-USER-CODE | CHARACTER(20) | Yes | Approver user code |
| WS-DECISION | CHARACTER(1) | Yes | A=Approve, R=Reject |
| WS-DECISION-NOTES | CHARACTER(200) | No | Approval notes |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=App Not Found, 2=Auth Not Found, 3=Not Authorised, 4=Payment Not Done, 5=Invalid Decision |
| WS-RETURN-MSG | CHARACTER(100) | Result message |

### Logic Summary
1. Validate decision is `A` or `R`
2. Verify payment completed
3. Validate authority user: active, `AUTHORITY-LEVEL='1'`, authorised for license type
4. Record decision; set status `A2` (approved) or `RE` (rejected)

---

## AB-RECORD-APPROVAL-2

**File:** [`src/action-blocks/AB-RECORD-APPROVAL-2.ACB`](../src/action-blocks/AB-RECORD-APPROVAL-2.ACB)
**Process:** `PROC-VALIDATE-AUTHORITY2-USER`, `PROC-RECORD-APPROVAL2`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | Yes | Application ID |
| WS-AUTHORITY-USER-CODE | CHARACTER(20) | Yes | Approver user code |
| WS-DECISION | CHARACTER(1) | Yes | A=Approve, R=Reject |
| WS-DECISION-NOTES | CHARACTER(200) | No | Approval notes |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=App Not Found, 2=Auth Not Found, 3=Not Authorised, 4=Auth1 Not Done, 5=Same Approver, 6=Invalid Decision |
| WS-RETURN-MSG | CHARACTER(100) | Result message |

### Logic Summary
1. Validate decision is `A` or `R`
2. Verify first approval was granted
3. Validate authority user: active, `AUTHORITY-LEVEL='2'`, authorised for license type
4. **Enforce different authority:** reject if `APPROVAL-1-AUTHORITY = AUTHORITY-NAME`
5. Record decision; set status `AP` (approved) or `RE` (rejected)

---

## AB-ISSUE-LICENSE

**File:** [`src/action-blocks/AB-ISSUE-LICENSE.ACB`](../src/action-blocks/AB-ISSUE-LICENSE.ACB)
**Process:** `PROC-VALIDATE-READY-FOR-ISSUE`, `PROC-GENERATE-LICENSE-NUMBER`, `PROC-SET-LICENSE-EXPIRY`, `PROC-CREATE-ISSUED-LICENSE`, `PROC-UPDATE-APPLICATION-ISSUED`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | Yes | Application ID |
| WS-VEHICLE-CLASS | CHARACTER(2) | Yes | A, B, C, or D |
| WS-RESTRICTIONS | CHARACTER(200) | No | Conditions on license |
| WS-ISSUED-BY-OFFICER | CHARACTER(50) | Yes | Issuing officer name |
| WS-ISSUED-BY-AUTHORITY | CHARACTER(50) | Yes | Issuing authority name |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-LICENSE-ID | NUMERIC(10) | New license record ID |
| WS-LICENSE-NUMBER | CHARACTER(20) | Generated license number |
| WS-EXPIRY-DATE | DATE | Calculated expiry date |
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=App Not Found, 2=Not Fully Approved, 3=Invalid Vehicle Class |
| WS-RETURN-MSG | CHARACTER(100) | Result message |

### Logic Summary
1. Validate vehicle class in (`A`, `B`, `C`, `D`)
2. Verify application status is `AP` (fully approved)
3. Individually re-verify all 5 gates: eligibility, history, payment, auth1, auth2
4. Set expiry: L=+1yr, P=+2yr, O=+5yr
5. Generate license number: `{PREFIX}{CLASS}{CANDIDATE-ID}{APP-ID}`
6. Create `ISSUED-LICENSE` record with `DEMERIT-BALANCE=12`
7. Update application `APPLICATION-STATUS='IS'`

---

## AB-INQUIRE-APPLICATION-STATUS

**File:** [`src/action-blocks/AB-INQUIRE-APPLICATION-STATUS.ACB`](../src/action-blocks/AB-INQUIRE-APPLICATION-STATUS.ACB)
**Process:** `PROC-INQUIRE-APPLICATION-STATUS`

### Imports
| Field | Type | Required | Description |
|---|---|---|---|
| WS-APPLICATION-ID | NUMERIC(10) | Yes | Application ID |

### Exports
| Field | Type | Description |
|---|---|---|
| WS-LICENSE-TYPE | CHARACTER(1) | L, P, or O |
| WS-APP-STATUS | CHARACTER(2) | Status code |
| WS-APP-STATUS-DESC | CHARACTER(50) | Human-readable description |
| WS-ELIG-STATUS | CHARACTER(1) | P/F/U |
| WS-HIST-STATUS | CHARACTER(1) | P/F/U |
| WS-PAY-STATUS | CHARACTER(1) | P/U/W |
| WS-APP1-STATUS | CHARACTER(1) | A/R/U |
| WS-APP2-STATUS | CHARACTER(1) | A/R/U |
| WS-LICENSE-NUMBER | CHARACTER(20) | Populated if status=IS |
| WS-RETURN-CODE | NUMERIC(4) | 0=OK, 1=Not Found |
| WS-RETURN-MSG | CHARACTER(100) | Result message |

### Logic Summary
1. Read application status view
2. Map all status fields to exports
3. Derive human-readable description via EVALUATE on status code
4. If status=`IS`, read `ISSUED-LICENSE` and return license number
