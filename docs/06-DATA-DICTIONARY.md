# 06 — Data Dictionary
## Driver License Issuance System (DLIS)

---

## Entity: CANDIDATE
**File:** [`src/encyclopedia/entities/CANDIDATE.ENT`](../src/encyclopedia/entities/CANDIDATE.ENT)
**Description:** Represents a person registering to apply for a driver license.

| Attribute | Type | Nullable | Description | Valid Values |
|---|---|---|---|---|
| CANDIDATE-ID | NUMERIC(10) | No | System-generated primary key | Auto-assigned |
| FIRST-NAME | CHARACTER(30) | No | Legal first name | |
| LAST-NAME | CHARACTER(30) | No | Legal last name | |
| DATE-OF-BIRTH | DATE | No | Date of birth | Must be in past |
| ID-NUMBER | CHARACTER(20) | No | National ID or Passport number | Must be unique |
| ADDRESS-LINE-1 | CHARACTER(50) | No | Street address | |
| ADDRESS-LINE-2 | CHARACTER(50) | Yes | Additional address | |
| CITY | CHARACTER(30) | No | City | |
| STATE-PROVINCE | CHARACTER(30) | No | State or Province | |
| POSTAL-CODE | CHARACTER(10) | Yes | Postal / ZIP code | |
| COUNTRY | CHARACTER(30) | No | Country | |
| PHONE-NUMBER | CHARACTER(15) | Yes | Contact phone | |
| EMAIL-ADDRESS | CHARACTER(60) | Yes | Contact email | |
| CREATED-DATE | DATE | No | Date record created | System date |
| RECORD-STATUS | CHARACTER(1) | No | Active/Inactive flag | `A`=Active, `I`=Inactive |

**Relationships:**
- Has one or more `LICENSE-APPLICATION`
- Has zero or more `DRIVING-HISTORY`
- Has zero or more `PAYMENT`
- Has zero or more `ISSUED-LICENSE`

---

## Entity: LICENSE-APPLICATION
**File:** [`src/encyclopedia/entities/LICENSE-APPLICATION.ENT`](../src/encyclopedia/entities/LICENSE-APPLICATION.ENT)
**Description:** Tracks a driver license application from submission to issuance.

| Attribute | Type | Nullable | Description | Valid Values |
|---|---|---|---|---|
| APPLICATION-ID | NUMERIC(10) | No | System-generated primary key | Auto-assigned |
| CANDIDATE-ID | NUMERIC(10) | No | Foreign key to CANDIDATE | |
| LICENSE-TYPE | CHARACTER(1) | No | License category | `L`=Learner, `P`=Probation, `O`=Open |
| APPLICATION-DATE | DATE | No | Date application submitted | |
| APPLICATION-STATUS | CHARACTER(2) | No | Current workflow status | `PE`,`EC`,`HC`,`PA`,`A1`,`A2`,`AP`,`RE`,`IS` |
| ELIGIBILITY-CHECK-STATUS | CHARACTER(1) | Yes | Eligibility result | `P`=Pass, `F`=Fail, `U`=Unchecked |
| ELIGIBILITY-CHECK-DATE | DATE | Yes | Date of eligibility check | |
| ELIGIBILITY-CHECK-NOTES | CHARACTER(200) | Yes | Check notes / reason | |
| HISTORY-CHECK-STATUS | CHARACTER(1) | Yes | History result | `P`=Pass, `F`=Fail, `U`=Unchecked |
| HISTORY-CHECK-DATE | DATE | Yes | Date of history check | |
| HISTORY-CHECK-NOTES | CHARACTER(200) | Yes | Check notes / reason | |
| PAYMENT-STATUS | CHARACTER(1) | Yes | Payment result | `P`=Paid, `U`=Unpaid, `W`=Waived |
| PAYMENT-REFERENCE | CHARACTER(20) | Yes | Payment reference | |
| APPROVAL-1-STATUS | CHARACTER(1) | Yes | First approval result | `A`=Approved, `R`=Rejected, `U`=Unchecked |
| APPROVAL-1-AUTHORITY | CHARACTER(50) | Yes | First approving authority name | |
| APPROVAL-1-DATE | DATE | Yes | Date of first approval | |
| APPROVAL-1-NOTES | CHARACTER(200) | Yes | First approval notes | |
| APPROVAL-2-STATUS | CHARACTER(1) | Yes | Second approval result | `A`=Approved, `R`=Rejected, `U`=Unchecked |
| APPROVAL-2-AUTHORITY | CHARACTER(50) | Yes | Second approving authority name | |
| APPROVAL-2-DATE | DATE | Yes | Date of second approval | |
| APPROVAL-2-NOTES | CHARACTER(200) | Yes | Second approval notes | |
| REJECTION-REASON | CHARACTER(300) | Yes | Reason if rejected | |
| CREATED-DATE | DATE | No | Creation date | |
| LAST-UPDATED-DATE | DATE | No | Last update date | |
| CREATED-BY | CHARACTER(20) | No | Creating user ID | |
| LAST-UPDATED-BY | CHARACTER(20) | No | Last updating user ID | |

---

## Entity: DRIVING-HISTORY
**File:** [`src/encyclopedia/entities/DRIVING-HISTORY.ENT`](../src/encyclopedia/entities/DRIVING-HISTORY.ENT)
**Description:** Records of a candidate's prior road incidents, fines, suspensions, and demerit points.

| Attribute | Type | Nullable | Description | Valid Values |
|---|---|---|---|---|
| HISTORY-ID | NUMERIC(10) | No | System-generated primary key | Auto-assigned |
| CANDIDATE-ID | NUMERIC(10) | No | Foreign key to CANDIDATE | |
| INCIDENT-DATE | DATE | No | Date of incident | |
| INCIDENT-TYPE | CHARACTER(2) | No | Type of incident | `OF`=Offence, `AC`=Accident, `SU`=Suspension, `DQ`=Disqualification |
| INCIDENT-DESCRIPTION | CHARACTER(300) | Yes | Description of incident | |
| DEMERIT-POINTS | NUMERIC(3) | Yes | Points assigned | >= 0 |
| FINE-AMOUNT | NUMERIC(10,2) | Yes | Fine amount issued | >= 0 |
| FINE-PAID-STATUS | CHARACTER(1) | Yes | Fine payment status | `Y`=Paid, `N`=Unpaid |
| SUSPENSION-START-DATE | DATE | Yes | Start of suspension period | |
| SUSPENSION-END-DATE | DATE | Yes | End of suspension period (null=indefinite) | |
| COURT-CASE-NUMBER | CHARACTER(20) | Yes | Court reference if applicable | |
| RECORDED-BY-AUTHORITY | CHARACTER(50) | Yes | Authority who recorded | |
| RECORD-STATUS | CHARACTER(1) | No | Active/Inactive flag | `A`=Active, `I`=Inactive |

---

## Entity: PAYMENT
**File:** [`src/encyclopedia/entities/PAYMENT.ENT`](../src/encyclopedia/entities/PAYMENT.ENT)
**Description:** Records payment transactions for license applications.

| Attribute | Type | Nullable | Description | Valid Values |
|---|---|---|---|---|
| PAYMENT-ID | NUMERIC(10) | No | System-generated primary key | Auto-assigned |
| APPLICATION-ID | NUMERIC(10) | No | Foreign key to LICENSE-APPLICATION | |
| CANDIDATE-ID | NUMERIC(10) | No | Foreign key to CANDIDATE | |
| PAYMENT-DATE | DATE | No | Date payment was made | |
| PAYMENT-AMOUNT | NUMERIC(10,2) | No | Amount paid | > 0 |
| PAYMENT-METHOD | CHARACTER(2) | No | Payment method | `CC`=Credit Card, `DC`=Debit Card, `EF`=EFT, `CS`=Cash, `CH`=Cheque |
| PAYMENT-REFERENCE | CHARACTER(30) | No | Bank/gateway reference | |
| PAYMENT-STATUS | CHARACTER(1) | No | Status of payment | `S`=Success, `F`=Failed, `R`=Refunded, `P`=Pending |
| LICENSE-TYPE | CHARACTER(1) | No | License type paid for | `L`, `P`, `O` |
| FEE-TYPE | CHARACTER(2) | No | Type of fee | `IF`=Issue, `RF`=Renewal, `LF`=Late, `PF`=Processing |
| RECEIPT-NUMBER | CHARACTER(20) | Yes | System-generated receipt | |
| PROCESSED-BY | CHARACTER(20) | Yes | Officer who processed | |
| NOTES | CHARACTER(200) | Yes | Additional notes | |

---

## Entity: ISSUED-LICENSE
**File:** [`src/encyclopedia/entities/ISSUED-LICENSE.ENT`](../src/encyclopedia/entities/ISSUED-LICENSE.ENT)
**Description:** The issued driver license record, created upon successful completion of all workflow stages.

| Attribute | Type | Nullable | Description | Valid Values |
|---|---|---|---|---|
| LICENSE-ID | NUMERIC(10) | No | System-generated primary key | Auto-assigned |
| APPLICATION-ID | NUMERIC(10) | No | Foreign key to LICENSE-APPLICATION | |
| CANDIDATE-ID | NUMERIC(10) | No | Foreign key to CANDIDATE | |
| LICENSE-NUMBER | CHARACTER(20) | No | Unique license number | System generated |
| LICENSE-TYPE | CHARACTER(1) | No | License category | `L`, `P`, `O` |
| ISSUE-DATE | DATE | No | Date license was issued | |
| EXPIRY-DATE | DATE | No | License expiry date | > ISSUE-DATE |
| LICENSE-STATUS | CHARACTER(1) | No | Current license status | `A`=Active, `S`=Suspended, `E`=Expired, `C`=Cancelled, `R`=Revoked |
| VEHICLE-CLASS | CHARACTER(2) | No | Authorized vehicle class | `A`=Motorcycle, `B`=Light, `C`=Heavy, `D`=Bus |
| RESTRICTIONS | CHARACTER(200) | Yes | Special conditions | e.g., "Corrective Lenses Required" |
| DEMERIT-BALANCE | NUMERIC(3) | Yes | Remaining demerit points | Starts at 12 |
| ISSUED-BY-AUTHORITY | CHARACTER(50) | No | Issuing authority | |
| ISSUED-BY-OFFICER | CHARACTER(50) | No | Issuing officer name | |
| RENEWAL-COUNT | NUMERIC(3) | Yes | Number of renewals | |
| PREVIOUS-LICENSE-ID | NUMERIC(10) | Yes | Prior license ID (for upgrades) | |
| NOTES | CHARACTER(300) | Yes | Additional notes | |

---

## Entity: AUTHORITY-USER
**File:** [`src/encyclopedia/entities/AUTHORITY-USER.ENT`](../src/encyclopedia/entities/AUTHORITY-USER.ENT)
**Description:** Represents an authorised officer who can perform approvals in the DLIS workflow.

| Attribute | Type | Nullable | Description | Valid Values |
|---|---|---|---|---|
| AUTHORITY-USER-ID | NUMERIC(10) | No | System-generated primary key | Auto-assigned |
| USER-CODE | CHARACTER(20) | No | Login/reference code | Must be unique |
| USER-NAME | CHARACTER(60) | No | Full name | |
| AUTHORITY-NAME | CHARACTER(50) | No | Organisation / department name | |
| AUTHORITY-LEVEL | CHARACTER(1) | No | Approval level | `1`=First Approver, `2`=Second Approver |
| DEPARTMENT | CHARACTER(50) | Yes | Specific department | |
| PHONE-NUMBER | CHARACTER(15) | Yes | Contact phone | |
| EMAIL-ADDRESS | CHARACTER(60) | Yes | Contact email | |
| ACTIVE-STATUS | CHARACTER(1) | No | Active/Inactive | `A`=Active, `I`=Inactive |
| LICENSE-TYPES-AUTHORISED | CHARACTER(3) | Yes | License types permitted | e.g., `L`, `LP`, `LPO` |

---

## Entity: LICENSE-FEE-SCHEDULE
**File:** [`src/encyclopedia/entities/LICENSE-FEE-SCHEDULE.ENT`](../src/encyclopedia/entities/LICENSE-FEE-SCHEDULE.ENT)
**Description:** Defines fee amounts per license type and fee category. Supports effective date ranges for fee changes over time.

| Attribute | Type | Nullable | Description | Valid Values |
|---|---|---|---|---|
| FEE-SCHEDULE-ID | NUMERIC(10) | No | System-generated primary key | Auto-assigned |
| LICENSE-TYPE | CHARACTER(1) | No | License type | `L`, `P`, `O` |
| FEE-TYPE | CHARACTER(2) | No | Fee category | `IF`=Issue, `RF`=Renewal, `LF`=Late, `PF`=Processing |
| FEE-AMOUNT | NUMERIC(10,2) | No | Amount in currency | > 0 |
| EFFECTIVE-DATE | DATE | No | Date fee becomes active | |
| EXPIRY-DATE | DATE | Yes | Date fee expires (null=no expiry) | |
| CURRENCY-CODE | CHARACTER(3) | No | ISO currency code | e.g., `USD`, `AUD`, `ZAR` |
| DESCRIPTION | CHARACTER(200) | Yes | Fee description | |
| ACTIVE-STATUS | CHARACTER(1) | No | Active/Inactive | `A`=Active, `I`=Inactive |

---

## Reference Codes Summary

### Application Status Codes (APPLICATION-STATUS)
| Code | Description |
|---|---|
| `PE` | Pending — submitted, awaiting eligibility check |
| `EC` | Eligibility Checked and passed |
| `HC` | History Checked and passed |
| `PA` | Payment Approved |
| `A1` | Awaiting First Authority Approval |
| `A2` | Awaiting Second Authority Approval |
| `AP` | Approved — all gates passed, ready for issuance |
| `IS` | License Issued (terminal) |
| `RE` | Rejected (terminal) |

### License Type Codes (LICENSE-TYPE)
| Code | Description | Min Age | Validity |
|---|---|---|---|
| `L` | Learner | 16 | 1 year |
| `P` | Probation | 17 | 2 years |
| `O` | Open | 18 | 5 years |

### Vehicle Class Codes (VEHICLE-CLASS)
| Code | Description |
|---|---|
| `A` | Motorcycle |
| `B` | Light Motor Vehicle |
| `C` | Heavy Motor Vehicle |
| `D` | Bus / Coach |

### Incident Type Codes (INCIDENT-TYPE)
| Code | Description |
|---|---|
| `OF` | Traffic Offence |
| `AC` | Accident |
| `SU` | Suspension |
| `DQ` | Disqualification |

### Payment Method Codes (PAYMENT-METHOD)
| Code | Description |
|---|---|
| `CC` | Credit Card |
| `DC` | Debit Card |
| `EF` | Electronic Funds Transfer |
| `CS` | Cash |
| `CH` | Cheque |
