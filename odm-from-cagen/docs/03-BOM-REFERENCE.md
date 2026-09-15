# 03 — BOM Reference
## Business Object Model — All Classes

---

## Overview

The ODM Business Object Model (BOM) consists of 9 Java classes in package `com.dlis.odm.model`.
All classes are converted directly from CA Gen encyclopedia entities (`.ENT` files).

The BOM is divided into two groups:
- **Data classes** — direct entity conversions (7 classes)
- **Session classes** — ODM-specific aggregation and result tracking (2 classes)

---

## Data Classes

### `Candidate`
**File:** [`bom/src/main/java/com/dlis/odm/model/Candidate.java`](../bom/src/main/java/com/dlis/odm/model/Candidate.java)
**CA Gen origin:** `CANDIDATE.ENT`

| Field | Type | CA Gen Attribute | Notes |
|---|---|---|---|
| `candidateId` | `long` | `CANDIDATE-ID` | System-generated PK |
| `firstName` | `String` | `FIRST-NAME` | |
| `lastName` | `String` | `LAST-NAME` | |
| `dateOfBirth` | `LocalDate` | `DATE-OF-BIRTH` | Must be in past (BR-08) |
| `idNumber` | `String` | `ID-NUMBER` | Must be unique (BR-07) |
| `addressLine1` | `String` | `ADDRESS-LINE-1` | |
| `addressLine2` | `String` | `ADDRESS-LINE-2` | Optional |
| `city` | `String` | `CITY` | |
| `stateProvince` | `String` | `STATE-PROVINCE` | |
| `postalCode` | `String` | `POSTAL-CODE` | Optional |
| `country` | `String` | `COUNTRY` | |
| `phoneNumber` | `String` | `PHONE-NUMBER` | Optional |
| `emailAddress` | `String` | `EMAIL-ADDRESS` | Optional |
| `createdDate` | `LocalDate` | `CREATED-DATE` | |
| `recordStatus` | `String` | `RECORD-STATUS` | `"A"`=Active, `"I"`=Inactive |

---

### `LicenseApplication`
**File:** [`bom/src/main/java/com/dlis/odm/model/LicenseApplication.java`](../bom/src/main/java/com/dlis/odm/model/LicenseApplication.java)
**CA Gen origin:** `LICENSE-APPLICATION.ENT`

This is the central fact object read and written by every ruleflow.

| Field | Type | CA Gen Attribute | Values |
|---|---|---|---|
| `applicationId` | `long` | `APPLICATION-ID` | System PK |
| `candidateId` | `long` | `CANDIDATE-ID` | FK |
| `licenseType` | `String` | `LICENSE-TYPE` | `L`, `P`, `O` |
| `applicationDate` | `LocalDate` | `APPLICATION-DATE` | |
| `applicationStatus` | `String` | `APPLICATION-STATUS` | `PE EC HC PA A2 AP IS RE` |
| `eligibilityCheckStatus` | `String` | `ELIGIBILITY-CHECK-STATUS` | `P F U` |
| `eligibilityCheckDate` | `LocalDate` | `ELIGIBILITY-CHECK-DATE` | |
| `eligibilityCheckNotes` | `String` | `ELIGIBILITY-CHECK-NOTES` | Failure reason |
| `historyCheckStatus` | `String` | `HISTORY-CHECK-STATUS` | `P F U` |
| `historyCheckDate` | `LocalDate` | `HISTORY-CHECK-DATE` | |
| `historyCheckNotes` | `String` | `HISTORY-CHECK-NOTES` | Failure reason |
| `paymentStatus` | `String` | `PAYMENT-STATUS` | `P U W` |
| `paymentReference` | `String` | `PAYMENT-REFERENCE` | |
| `approval1Status` | `String` | `APPROVAL-1-STATUS` | `A R U` |
| `approval1Authority` | `String` | `APPROVAL-1-AUTHORITY` | Authority name |
| `approval1Date` | `LocalDate` | `APPROVAL-1-DATE` | |
| `approval2Status` | `String` | `APPROVAL-2-STATUS` | `A R U` |
| `approval2Authority` | `String` | `APPROVAL-2-AUTHORITY` | Must differ from Auth1 |
| `approval2Date` | `LocalDate` | `APPROVAL-2-DATE` | |
| `rejectionReason` | `String` | `REJECTION-REASON` | |

---

### `DrivingHistory`
**File:** [`bom/src/main/java/com/dlis/odm/model/DrivingHistory.java`](../bom/src/main/java/com/dlis/odm/model/DrivingHistory.java)
**CA Gen origin:** `DRIVING-HISTORY.ENT`

Loaded as a list into `RuleContext.drivingHistoryRecords` for iteration in history rules.

| Field | Type | CA Gen Attribute | Values |
|---|---|---|---|
| `historyId` | `long` | `HISTORY-ID` | |
| `candidateId` | `long` | `CANDIDATE-ID` | FK |
| `incidentDate` | `LocalDate` | `INCIDENT-DATE` | |
| `incidentType` | `String` | `INCIDENT-TYPE` | `OF AC SU DQ` |
| `demeritPoints` | `int` | `DEMERIT-POINTS` | ≥ 0 |
| `fineAmount` | `BigDecimal` | `FINE-AMOUNT` | ≥ 0 |
| `finePaidStatus` | `String` | `FINE-PAID-STATUS` | `Y N` |
| `suspensionStartDate` | `LocalDate` | `SUSPENSION-START-DATE` | |
| `suspensionEndDate` | `LocalDate` | `SUSPENSION-END-DATE` | null = indefinite |
| `recordStatus` | `String` | `RECORD-STATUS` | `A I` |

---

### `Payment`
**File:** [`bom/src/main/java/com/dlis/odm/model/Payment.java`](../bom/src/main/java/com/dlis/odm/model/Payment.java)
**CA Gen origin:** `PAYMENT.ENT`

| Field | Type | Values |
|---|---|---|
| `paymentMethod` | `String` | `CC DC EF CS CH` |
| `paymentStatus` | `String` | `S F R P` |
| `feeType` | `String` | `IF RF LF PF` |

---

### `IssuedLicense`
**File:** [`bom/src/main/java/com/dlis/odm/model/IssuedLicense.java`](../bom/src/main/java/com/dlis/odm/model/IssuedLicense.java)
**CA Gen origin:** `ISSUED-LICENSE.ENT`

Loaded into `RuleContext.existingLicenses` for upgrade path checks (BR-05, BR-06).

| Field | Values |
|---|---|
| `licenseType` | `L P O` |
| `licenseStatus` | `A S E C R` |
| `vehicleClass` | `A B C D` |
| `demeritBalance` | Starts at 12 (BR-26) |

---

### `AuthorityUser`
**File:** [`bom/src/main/java/com/dlis/odm/model/AuthorityUser.java`](../bom/src/main/java/com/dlis/odm/model/AuthorityUser.java)
**CA Gen origin:** `AUTHORITY-USER.ENT`

Loaded into `RuleContext.approvalUser` before approval rulesets.

| Field | Values |
|---|---|
| `authorityLevel` | `"1"` or `"2"` |
| `activeStatus` | `A I` |
| `licenseTypesAuthorised` | `"L"`, `"LP"`, `"LPO"` etc. |

---

### `LicenseFeeSchedule`
**File:** [`bom/src/main/java/com/dlis/odm/model/LicenseFeeSchedule.java`](../bom/src/main/java/com/dlis/odm/model/LicenseFeeSchedule.java)
**CA Gen origin:** `LICENSE-FEE-SCHEDULE.ENT`

Loaded into `RuleContext.applicableFeeSchedule` before payment rulesets.

---

## Session Classes

### `RuleContext`
**File:** [`bom/src/main/java/com/dlis/odm/model/RuleContext.java`](../bom/src/main/java/com/dlis/odm/model/RuleContext.java)

The top-level fact object passed into every ODM ruleset execution.

```
RuleContext
├── application: LicenseApplication      ← current application being processed
├── candidate: Candidate                  ← candidate linked to application
├── drivingHistoryRecords: List<DrivingHistory>  ← all active history
├── existingLicenses: List<IssuedLicense>         ← for upgrade checks
├── approvalUser: AuthorityUser           ← officer submitting approval
├── applicableFeeSchedule: LicenseFeeSchedule     ← active fee
├── approvalDecision: String              ← "A" or "R"
├── vehicleClass: String                  ← for issuance
├── returnCode: int                       ← 0=OK, non-zero=error
├── returnMessage: String                 ← human-readable result
├── violations: List<RuleViolation>       ← accumulated errors
├── calculatedExpiryDate: LocalDate       ← set by issuance rules
├── generatedLicenseNumber: String        ← set by issuance rules
├── generatedReceiptNumber: String        ← set by payment rules
├── candidateAge: int                     ← computed by eligibility rules
├── totalDemeritPoints: int               ← accumulated by history rules
└── today: LocalDate                      ← injected at invocation time
```

---

### `RuleViolation`
**File:** [`bom/src/main/java/com/dlis/odm/model/RuleViolation.java`](../bom/src/main/java/com/dlis/odm/model/RuleViolation.java)

Accumulated in `RuleContext.violations` during execution.

| Field | Description |
|---|---|
| `ruleId` | Business rule ID, e.g., `"BR-02"` |
| `message` | Exact error message (matches CA Gen return messages) |
| `severity` | `"ERROR"` or `"WARNING"` |
| `fieldName` | The offending field, if applicable |
