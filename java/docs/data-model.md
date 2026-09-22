# DLIS — Data Model

## 1. Schema Overview

All DLIS tables reside in the `DLIS` schema on DB2. The schema is compatible with:
- DB2 for z/OS v12+
- DB2 LUW 11.5 (on zLinux)

The DDL sources are:
- `dlis-core/src/main/resources/db2/DLIS-CREATE-TABLES.sql`
- `dlis-core/src/main/resources/db2/DLIS-CREATE-INDEXES.sql`
- `dlis-core/src/main/resources/db2/DLIS-SEED-DATA.sql`

---

## 2. Entity Relationship Diagram

```
CANDIDATE (1) ──────────────────────── (N) LICENSE_APPLICATION
     │                                          │
     │                                          │
     │ (1)                                      │ (1)
     │                                          │
     ├──────────────── (N) DRIVING_HISTORY      │
     │                                          │
     └──────────────── (N) ISSUED_LICENSE (N) ──┘
                                │
                                │ (N)
                             PAYMENT

LICENSE_FEE_SCHEDULE  (standalone lookup table)
AUTHORITY_USER        (standalone lookup table)
```

---

## 3. Table Descriptions

### 3.1 CANDIDATE

Stores personal and contact information for each license applicant. All candidates are Queensland, Australia residents.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `CANDIDATE_ID` | DECIMAL(10,0) | PK, IDENTITY | Auto-generated primary key |
| `FIRST_NAME` | VARCHAR(30) | NOT NULL | Given name |
| `LAST_NAME` | VARCHAR(30) | NOT NULL | Family name |
| `DATE_OF_BIRTH` | DATE | NOT NULL, < CURRENT DATE | Must be in the past |
| `ID_NUMBER` | VARCHAR(20) | NOT NULL | Queensland driver licence number or QLD proof-of-age card number (up to 8 digits) |
| `ADDRESS_LINE_1` | VARCHAR(50) | NOT NULL | Primary address |
| `ADDRESS_LINE_2` | VARCHAR(50) | — | Secondary address / unit number (optional) |
| `CITY` | VARCHAR(30) | NOT NULL | City or suburb |
| `STATE_PROVINCE` | VARCHAR(30) | NOT NULL | Australian state or territory (typically `Queensland`) |
| `POSTAL_CODE` | VARCHAR(10) | NOT NULL | Australian 4-digit postcode |
| `COUNTRY` | VARCHAR(30) | NOT NULL | Country — `AU` for Australia |
| `PHONE_NUMBER` | VARCHAR(15) | — | Australian phone number (e.g. +61-7-xxxx-xxxx) |
| `EMAIL_ADDRESS` | VARCHAR(60) | — | Email address |
| `CREATED_DATE` | DATE | NOT NULL | Record creation date |
| `RECORD_STATUS` | CHAR(1) | NOT NULL, DEFAULT 'A' | `A`=Active, `I`=Inactive |

**Constraints:** `CHK_CAND_STATUS` (status in `A`,`I`), `CHK_CAND_DOB` (DOB < current date).

---

### 3.2 LICENSE_APPLICATION

Tracks the full lifecycle of each license application from submission through to issuance or rejection.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `APPLICATION_ID` | DECIMAL(10,0) | PK, IDENTITY (start 1000) | Auto-generated primary key |
| `CANDIDATE_ID` | DECIMAL(10,0) | NOT NULL, FK → CANDIDATE | Owning candidate |
| `LICENSE_TYPE` | CHAR(1) | NOT NULL, IN ('L','P','O') | License type |
| `APPLICATION_DATE` | DATE | NOT NULL | Date of submission |
| `APPLICATION_STATUS` | CHAR(2) | NOT NULL, DEFAULT 'PE' | Current workflow status (see codes below) |
| `ELIGIBILITY_CHK_STATUS` | CHAR(1) | DEFAULT 'U' | `P`=Pass, `F`=Fail, `U`=Unchecked |
| `ELIGIBILITY_CHK_DATE` | DATE | — | Date eligibility check ran |
| `ELIGIBILITY_CHK_NOTES` | VARCHAR(200) | — | Notes or failure reason |
| `HISTORY_CHK_STATUS` | CHAR(1) | DEFAULT 'U' | `P`=Pass, `F`=Fail, `U`=Unchecked |
| `HISTORY_CHK_DATE` | DATE | — | Date history check ran |
| `HISTORY_CHK_NOTES` | VARCHAR(200) | — | Notes or failure reason |
| `PAYMENT_STATUS` | CHAR(1) | DEFAULT 'U' | `P`=Paid, `U`=Unpaid, `W`=Waived |
| `PAYMENT_REFERENCE` | VARCHAR(20) | — | Receipt number or external reference |
| `APPROVAL_1_STATUS` | CHAR(1) | DEFAULT 'U' | `A`=Approved, `R`=Rejected, `U`=Pending |
| `APPROVAL_1_AUTHORITY` | VARCHAR(50) | — | Authority name who gave first approval |
| `APPROVAL_1_DATE` | DATE | — | First approval date |
| `APPROVAL_1_NOTES` | VARCHAR(200) | — | First approval decision notes |
| `APPROVAL_2_STATUS` | CHAR(1) | DEFAULT 'U' | `A`=Approved, `R`=Rejected, `U`=Pending |
| `APPROVAL_2_AUTHORITY` | VARCHAR(50) | — | Authority name who gave second approval |
| `APPROVAL_2_DATE` | DATE | — | Second approval date |
| `APPROVAL_2_NOTES` | VARCHAR(200) | — | Second approval decision notes |
| `REJECTION_REASON` | VARCHAR(300) | — | Reason when `APPLICATION_STATUS = RE` |
| `CREATED_DATE` | DATE | NOT NULL | Record creation date |
| `LAST_UPDATED_DATE` | DATE | NOT NULL | Last modification date |
| `CREATED_BY` | VARCHAR(20) | NOT NULL | User who created the record |
| `LAST_UPDATED_BY` | VARCHAR(20) | NOT NULL | User who last modified the record |

#### Application Status Codes

| Code | Meaning |
|---|---|
| `PE` | Pending — newly created |
| `EC` | Eligibility Checked (passed) |
| `HC` | History Checked (passed) |
| `PP` | Payment Pending |
| `PA` | Payment Approved (received) |
| `A1` | First Approval Pending |
| `A2` | Second Approval Pending |
| `AP` | Fully Approved |
| `RE` | Rejected |
| `IS` | Issued |

---

### 3.3 DRIVING_HISTORY

Records incidents on a candidate's driving record.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `HISTORY_ID` | DECIMAL(10,0) | PK, IDENTITY | Auto-generated primary key |
| `CANDIDATE_ID` | DECIMAL(10,0) | NOT NULL, FK → CANDIDATE | Owning candidate |
| `INCIDENT_DATE` | DATE | NOT NULL | Date of incident |
| `INCIDENT_TYPE` | CHAR(2) | NOT NULL, IN ('OF','AC','SU','DQ') | Incident type (see below) |
| `INCIDENT_DESCRIPTION` | VARCHAR(300) | — | Narrative description |
| `DEMERIT_POINTS` | DECIMAL(3,0) | DEFAULT 0, >= 0 | Demerit points assigned |
| `FINE_AMOUNT` | DECIMAL(10,2) | DEFAULT 0, >= 0 | Fine imposed |
| `FINE_PAID_STATUS` | CHAR(1) | IN ('Y','N') | `Y`=Paid, `N`=Unpaid |
| `SUSPENSION_START_DATE` | DATE | — | Start of suspension/disqualification |
| `SUSPENSION_END_DATE` | DATE | — | End of suspension (null = open-ended) |
| `COURT_CASE_NUMBER` | VARCHAR(20) | — | Court case reference |
| `RECORDED_BY_AUTHORITY` | VARCHAR(50) | — | Recording authority |
| `RECORD_STATUS` | CHAR(1) | NOT NULL, DEFAULT 'A' | `A`=Active, `I`=Inactive |

#### Incident Type Codes

| Code | Meaning |
|---|---|
| `OF` | Offence (traffic violation) |
| `AC` | Accident |
| `SU` | Suspension |
| `DQ` | Disqualification |

---

### 3.4 ISSUED_LICENSE

Holds the record of each issued driving license.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `LICENSE_ID` | DECIMAL(10,0) | PK, IDENTITY | Auto-generated primary key |
| `APPLICATION_ID` | DECIMAL(10,0) | NOT NULL, FK → LICENSE_APPLICATION | Source application |
| `CANDIDATE_ID` | DECIMAL(10,0) | NOT NULL, FK → CANDIDATE | License holder |
| `LICENSE_NUMBER` | VARCHAR(20) | NOT NULL, UNIQUE | System-generated license number |
| `LICENSE_TYPE` | CHAR(1) | NOT NULL, IN ('L','P','O') | License type |
| `ISSUE_DATE` | DATE | NOT NULL | Date license was issued |
| `EXPIRY_DATE` | DATE | NOT NULL, > ISSUE_DATE | License expiry date |
| `LICENSE_STATUS` | CHAR(1) | NOT NULL, DEFAULT 'A' | Current status (see below) |
| `VEHICLE_CLASS` | CHAR(2) | NOT NULL, IN ('A','B','C','D') | Vehicle category |
| `RESTRICTIONS` | VARCHAR(200) | — | Restrictions or conditions |
| `DEMERIT_BALANCE` | DECIMAL(3,0) | DEFAULT 12, >= 0 | Remaining demerit points |
| `ISSUED_BY_AUTHORITY` | VARCHAR(50) | NOT NULL | Issuing authority |
| `ISSUED_BY_OFFICER` | VARCHAR(50) | NOT NULL | Issuing officer |
| `RENEWAL_COUNT` | DECIMAL(3,0) | DEFAULT 0 | Number of renewals |
| `PREVIOUS_LICENSE_ID` | DECIMAL(10,0) | — | ID of replaced license (for renewals) |
| `NOTES` | VARCHAR(300) | — | Additional notes |

#### License Status Codes

| Code | Meaning |
|---|---|
| `A` | Active |
| `S` | Suspended |
| `E` | Expired |
| `C` | Cancelled |
| `R` | Revoked |

#### Vehicle Class Codes

| Code | Meaning |
|---|---|
| `A` | Motorcycle |
| `B` | Light vehicle (up to 3500 kg) |
| `C` | Heavy vehicle / truck |
| `D` | Bus / passenger transport |

#### License Validity Periods

| License Type | Validity |
|---|---|
| L (Learner) | 1 year |
| P (Probation) | 2 years |
| O (Open) | 5 years |

---

### 3.5 PAYMENT

Records each fee payment associated with a license application.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `PAYMENT_ID` | DECIMAL(10,0) | PK, IDENTITY | Auto-generated primary key |
| `APPLICATION_ID` | DECIMAL(10,0) | NOT NULL, FK → LICENSE_APPLICATION | Associated application |
| `CANDIDATE_ID` | DECIMAL(10,0) | NOT NULL, FK → CANDIDATE | Payer |
| `PAYMENT_DATE` | DATE | NOT NULL | Date payment was received |
| `PAYMENT_AMOUNT` | DECIMAL(10,2) | NOT NULL, > 0 | Amount paid |
| `PAYMENT_METHOD` | CHAR(2) | NOT NULL, IN ('CC','DC','EF','CS','CH') | Payment method |
| `PAYMENT_REFERENCE` | VARCHAR(30) | NOT NULL | External payment reference |
| `PAYMENT_STATUS` | CHAR(1) | NOT NULL, DEFAULT 'P' | `S`=Success, `F`=Failed, `R`=Refunded, `P`=Pending |
| `LICENSE_TYPE` | CHAR(1) | NOT NULL, IN ('L','P','O') | License type paid for |
| `FEE_TYPE` | CHAR(2) | NOT NULL, IN ('IF','RF','LF','PF') | Fee category |
| `RECEIPT_NUMBER` | VARCHAR(20) | — | System-generated receipt number |
| `PROCESSED_BY` | VARCHAR(20) | — | Officer who processed the payment |
| `NOTES` | VARCHAR(200) | — | Additional notes |

#### Fee Type Codes

| Code | Meaning |
|---|---|
| `IF` | Issue Fee (new license) |
| `RF` | Renewal Fee |
| `LF` | Late Fee (overdue renewal) |
| `PF` | Penalty Fee |

#### Payment Method Codes

| Code | Meaning |
|---|---|
| `CC` | Credit Card |
| `DC` | Debit Card |
| `EF` | Electronic Funds Transfer |
| `CS` | Cash |
| `CH` | Cheque |

---

### 3.6 LICENSE_FEE_SCHEDULE

Lookup table holding the fee amounts for each license type / fee type combination.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `FEE_SCHEDULE_ID` | DECIMAL(10,0) | PK, IDENTITY | Auto-generated primary key |
| `LICENSE_TYPE` | CHAR(1) | NOT NULL, IN ('L','P','O') | License type |
| `FEE_TYPE` | CHAR(2) | NOT NULL, IN ('IF','RF','LF','PF') | Fee category |
| `FEE_AMOUNT` | DECIMAL(10,2) | NOT NULL, > 0 | Fee amount |
| `EFFECTIVE_DATE` | DATE | NOT NULL | Date fee schedule takes effect |
| `EXPIRY_DATE` | DATE | — | Date fee schedule expires (null = no expiry) |
| `CURRENCY_CODE` | CHAR(3) | NOT NULL, DEFAULT 'AUD' | ISO 4217 currency code — `AUD` (Australian Dollar) |
| `DESCRIPTION` | VARCHAR(200) | — | Description of fee |
| `ACTIVE_STATUS` | CHAR(1) | NOT NULL, DEFAULT 'A' | `A`=Active, `I`=Inactive |

#### Seeded Fee Data (effective 2024-01-01)

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

---

### 3.7 AUTHORITY_USER

Registry of officials authorised to approve license applications.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `AUTHORITY_USER_ID` | DECIMAL(10,0) | PK, IDENTITY | Auto-generated primary key |
| `USER_CODE` | VARCHAR(20) | NOT NULL, UNIQUE | Short user code used in approvals and CICS |
| `USER_NAME` | VARCHAR(60) | NOT NULL | Full name |
| `AUTHORITY_NAME` | VARCHAR(50) | NOT NULL | Name of authority body |
| `AUTHORITY_LEVEL` | CHAR(1) | NOT NULL, IN ('1','2') | `1`=First approver, `2`=Second approver |
| `DEPARTMENT` | VARCHAR(50) | — | Department |
| `PHONE_NUMBER` | VARCHAR(15) | — | Phone number |
| `EMAIL_ADDRESS` | VARCHAR(60) | — | Email address |
| `ACTIVE_STATUS` | CHAR(1) | NOT NULL, DEFAULT 'A' | `A`=Active, `I`=Inactive |
| `LICENSE_TYPES_AUTHORISED` | VARCHAR(3) | — | Compact code e.g. `"LPO"`, `"L"`, `"PO"` |

---

## 4. Indexes

Created by `DLIS-CREATE-INDEXES.sql`:

| Index | Table | Columns | Purpose |
|---|---|---|---|
| `IDX_CAND_NAME` | CANDIDATE | LAST_NAME, FIRST_NAME | Name search |
| `IDX_CAND_IDNO` | CANDIDATE | ID_NUMBER | National ID lookup |
| `IDX_CAND_STATUS` | CANDIDATE | RECORD_STATUS | Active candidate filter |
| `IDX_APP_CAND` | LICENSE_APPLICATION | CANDIDATE_ID | Applications per candidate |
| `IDX_APP_STATUS` | LICENSE_APPLICATION | APPLICATION_STATUS | Status-based queries |
| `IDX_APP_TYPE` | LICENSE_APPLICATION | LICENSE_TYPE | Type-based queries |
| `IDX_APP_CAND_TYPE` | LICENSE_APPLICATION | CANDIDATE_ID, LICENSE_TYPE | Duplicate-application guard |
| `IDX_LIC_CAND` | ISSUED_LICENSE | CANDIDATE_ID | Licenses per candidate |
| `IDX_LIC_NUMBER` | ISSUED_LICENSE | LICENSE_NUMBER | License number lookup |
| `IDX_LIC_TYPE_STATUS` | ISSUED_LICENSE | LICENSE_TYPE, LICENSE_STATUS | Active license check (upgrade path) |
| `IDX_PAY_APP` | PAYMENT | APPLICATION_ID | Payments per application |
| `IDX_PAY_CAND` | PAYMENT | CANDIDATE_ID | Payments per candidate |
| `IDX_FEE_LOOKUP` | LICENSE_FEE_SCHEDULE | LICENSE_TYPE, FEE_TYPE, ACTIVE_STATUS | Fee schedule lookup |
| `IDX_DHIST_CAND` | DRIVING_HISTORY | CANDIDATE_ID | History per candidate |

---

## 5. Domain Model (Java)

Each DB table maps to a corresponding Java domain class in `com.dlis.core.domain`:

| Java Class | DB Table | Key Fields / Helper Methods |
|---|---|---|
| `Candidate` | `DLIS.CANDIDATE` | `isActive()` |
| `LicenseApplication` | `DLIS.LICENSE_APPLICATION` | `isEligibilityPassed()`, `isHistoryPassed()`, `isPaymentComplete()`, `isApproval1Complete()`, `isApproval2Complete()`, `isFullyApproved()`, `isIssued()`, `isRejected()` |
| `IssuedLicense` | `DLIS.ISSUED_LICENSE` | `isActive()`, `isExpired()` |
| `Payment` | `DLIS.PAYMENT` | — |
| `DrivingHistory` | `DLIS.DRIVING_HISTORY` | `isActiveSuspensionOrDisqualification()`, `hasUnpaidFine()` |
| `LicenseFeeSchedule` | `DLIS.LICENSE_FEE_SCHEDULE` | `isActive()` |
| `AuthorityUser` | `DLIS.AUTHORITY_USER` | `isActive()`, `isAuthorisedFor(licenseType)`, `isLevel1()`, `isLevel2()` |

All domain classes implement `java.io.Serializable` (required for EJB passivation) and use plain Java `LocalDate` / `BigDecimal` types — there are no JPA annotations.
