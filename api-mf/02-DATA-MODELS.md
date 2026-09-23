# DLIS — Data Models

Derived from COBOL copybooks (`DLISDCLG.cpy`, `DLISWS.cpy`) and Db2 DDL (`DLIS0001-CREATE-TABLES.sql`).

---

## Candidate

Source: `DLIS.CANDIDATE` / `DCLDLIS-CANDIDATE`

| Field            | Type        | Max Length | Nullable | Constraints / Notes                     |
|-----------------|------------|-----------|---------|----------------------------------------|
| candidateId      | integer     | —         | No       | System-generated identity, PK          |
| firstName        | string      | 30        | No       |                                        |
| lastName         | string      | 30        | No       |                                        |
| dateOfBirth      | date (ISO)  | —         | No       | YYYY-MM-DD                             |
| idNumber         | string      | 20        | No       | National ID — must be unique           |
| addressLine1     | string      | 50        | No       |                                        |
| addressLine2     | string      | 50        | Yes      |                                        |
| city             | string      | 30        | No       |                                        |
| stateProvince    | string      | 30        | No       |                                        |
| postalCode       | string      | 10        | Yes      |                                        |
| country          | string      | 30        | No       |                                        |
| phoneNumber      | string      | 15        | Yes      |                                        |
| emailAddress     | string      | 60        | Yes      |                                        |
| recordStatus     | enum        | 1         | No       | `A`=Active, `I`=Inactive               |
| createdDate      | date (ISO)  | —         | No       | System-set on insert                   |

---

## LicenseApplication

Source: `DLIS.LICENSE_APPLICATION` / `DCLDLIS-LICENSE-APPL`

| Field              | Type       | Max Length | Nullable | Constraints / Notes                                                             |
|-------------------|-----------|-----------|---------|--------------------------------------------------------------------------------|
| applicationId      | integer    | —         | No       | System-generated identity, PK                                                  |
| candidateId        | integer    | —         | No       | FK → Candidate                                                                 |
| licenseType        | enum       | 1         | No       | `L`=Learner, `P`=Probation, `O`=Open                                          |
| applicationDate    | date       | —         | No       | System-set on insert                                                           |
| applicationStatus  | enum       | 2         | No       | `PE` `EC` `HC` `PA` `A2` `AP` `IS` `RE` (see lifecycle)                       |
| eligCheckStatus    | enum       | 1         | No       | `P`=Pass, `F`=Fail, `U`=Unchecked                                             |
| eligCheckDate      | date       | —         | Yes      |                                                                                |
| eligCheckNotes     | string     | 200       | Yes      | Failure reason if `F`                                                          |
| histCheckStatus    | enum       | 1         | No       | `P`=Pass, `F`=Fail, `U`=Unchecked                                             |
| histCheckDate      | date       | —         | Yes      |                                                                                |
| histCheckNotes     | string     | 200       | Yes      | Failure reason if `F`                                                          |
| paymentStatus      | enum       | 1         | No       | `P`=Paid, `U`=Unpaid, `W`=Waived                                              |
| paymentReference   | string     | 20        | Yes      |                                                                                |
| approval1Status    | enum       | 1         | No       | `A`=Approved, `R`=Rejected, `U`=Pending                                       |
| approval1Authority | string     | 50        | Yes      | Authority name of first approver                                               |
| approval1Date      | date       | —         | Yes      |                                                                                |
| approval1Notes     | string     | 200       | Yes      |                                                                                |
| approval2Status    | enum       | 1         | No       | `A`=Approved, `R`=Rejected, `U`=Pending                                       |
| approval2Authority | string     | 50        | Yes      | Authority name of second approver; must differ from approval1Authority         |
| approval2Date      | date       | —         | Yes      |                                                                                |
| approval2Notes     | string     | 200       | Yes      |                                                                                |
| rejectionReason    | string     | 300       | Yes      |                                                                                |
| createdDate        | date       | —         | No       | System-set                                                                     |
| lastUpdatedDate    | date       | —         | No       | System-set                                                                     |
| createdBy          | string     | 20        | No       | Terminal/user that created the record                                          |
| lastUpdatedBy      | string     | 20        | No       |                                                                                |

---

## DrivingHistory

Source: `DLIS.DRIVING_HISTORY` / `DCLDLIS-DRIVING-HIST`

| Field           | Type      | Max Length | Nullable | Constraints / Notes                                    |
|----------------|----------|-----------|---------|-------------------------------------------------------|
| historyId       | integer   | —         | No       | System-generated PK                                   |
| candidateId     | integer   | —         | No       | FK → Candidate                                        |
| incidentDate    | date      | —         | No       |                                                       |
| incidentType    | enum      | 2         | No       | `OF`=Offence, `AC`=Accident, `SU`=Suspension, `DQ`=Disqualification |
| incidentDesc    | string    | 300       | Yes      |                                                       |
| demeritPoints   | integer   | 3 digits  | Yes      |                                                       |
| fineAmount      | decimal   | 10,2      | Yes      |                                                       |
| finePaidStatus  | enum      | 1         | Yes      | `Y`=Paid, `N`=Unpaid                                  |
| suspStartDate   | date      | —         | Yes      |                                                       |
| suspEndDate     | date      | —         | Yes      | Null or future = active suspension                    |
| courtCaseNumber | string    | 20        | Yes      |                                                       |
| recordedByAuth  | string    | 50        | Yes      |                                                       |
| recordStatus    | enum      | 1         | No       | `A`=Active, `I`=Inactive                              |

---

## Payment

Source: `DLIS.PAYMENT` / `DCLDLIS-PAYMENT`

| Field             | Type     | Max Length | Nullable | Constraints / Notes                              |
|------------------|---------|-----------|---------|--------------------------------------------------|
| paymentId         | integer  | —         | No       | System-generated PK                              |
| applicationId     | integer  | —         | No       | FK → LicenseApplication                          |
| candidateId       | integer  | —         | No       | FK → Candidate                                   |
| paymentDate       | date     | —         | No       | System-set on insert                             |
| paymentAmount     | decimal  | 10,2      | No       | Taken from fee schedule at time of payment       |
| paymentMethod     | enum     | 2         | No       | `CC`=Credit Card, `DC`=Debit Card, `EF`=EFT, `CS`=Cash, `CH`=Cheque |
| paymentReference  | string   | 30        | No       | External reference (bank ref, etc.)              |
| paymentStatus     | enum     | 1         | No       | `S`=Success, `F`=Failed, `R`=Reversed, `P`=Pending |
| licenseType       | enum     | 1         | No       | `L` `P` `O`                                     |
| feeType           | enum     | 2         | No       | `IF`=Initial Fee, `RF`=Renewal, `LF`=Late, `PF`=Penalty |
| receiptNumber     | string   | 20        | Yes      | System-generated: `RCP` + YYYYMMDD + applicationId |
| processedBy       | string   | 20        | Yes      |                                                  |
| notes             | string   | 200       | Yes      |                                                  |

---

## IssuedLicense

Source: `DLIS.ISSUED_LICENSE` / `DCLDLIS-ISSUED-LIC`

| Field            | Type     | Max Length | Nullable | Constraints / Notes                                      |
|-----------------|---------|-----------|---------|----------------------------------------------------------|
| licenseId        | integer  | —         | No       | System-generated PK                                      |
| applicationId    | integer  | —         | No       | FK → LicenseApplication                                  |
| candidateId      | integer  | —         | No       | FK → Candidate                                           |
| licenseNumber    | string   | 20        | No       | Unique. Format: `{prefix}{vehicleClass}{candidateId}{applicationId}` |
| licenseType      | enum     | 1         | No       | `L` `P` `O`                                             |
| issueDate        | date     | —         | No       | System-set to current date                               |
| expiryDate       | date     | —         | No       | issueDate + 1yr (L) / 2yr (P) / 5yr (O)                 |
| licenseStatus    | enum     | 1         | No       | `A`=Active, `S`=Suspended, `E`=Expired, `C`=Cancelled, `R`=Revoked |
| vehicleClass     | enum     | 2         | No       | `A` `B` `C` `D`                                         |
| restrictions     | string   | 200       | Yes      |                                                          |
| demeritBalance   | integer  | —         | No       | Initial value: 12                                        |
| issuedByAuth     | string   | 50        | No       | Issuing authority name                                   |
| issuedByOfficer  | string   | 50        | No       | Officer code/name                                        |
| renewalCount     | integer  | —         | No       | Default 0                                                |
| prevLicenseId    | integer  | —         | Yes      | FK → previous licence if renewal                        |
| notes            | string   | 300       | Yes      |                                                          |

---

## AuthorityUser

Source: `DLIS.AUTHORITY_USER` / `DCLDLIS-AUTH-USER`

| Field            | Type    | Max Length | Nullable | Constraints / Notes                               |
|-----------------|--------|-----------|---------|--------------------------------------------------|
| authorityUserId  | integer | —         | No       | System-generated PK                               |
| userCode         | string  | 20        | No       | Unique login code                                 |
| userName         | string  | 60        | No       |                                                   |
| authorityName    | string  | 50        | No       | Organisation/authority name                       |
| authorityLevel   | enum    | 1         | No       | `1`=First approver, `2`=Second approver           |
| department       | string  | 50        | Yes      |                                                   |
| phoneNumber      | string  | 15        | Yes      |                                                   |
| emailAddress     | string  | 60        | Yes      |                                                   |
| activeStatus     | enum    | 1         | No       | `A`=Active, `I`=Inactive                          |
| licTypesAuth     | string  | 3         | Yes      | Licence types this user may approve (e.g. `LPO`)  |

---

## LicenseFeeSchedule

Source: `DLIS.LICENSE_FEE_SCHEDULE` / `DCLDLIS-FEE-SCHED`

| Field          | Type     | Max Length | Nullable | Constraints / Notes                          |
|---------------|---------|-----------|---------|----------------------------------------------|
| feeScheduleId  | integer  | —         | No       | System-generated PK                          |
| licenseType    | enum     | 1         | No       | `L` `P` `O`                                 |
| feeType        | enum     | 2         | No       | `IF`=Initial, `RF`=Renewal, `LF`=Late, `PF`=Penalty |
| feeAmount      | decimal  | 10,2      | No       |                                              |
| effectiveDate  | date     | —         | No       | Fee applies on and after this date           |
| expiryDate     | date     | —         | Yes      | Null = no expiry                             |
| currencyCode   | string   | 3         | No       | ISO 4217 (e.g. `USD`, `ZAR`)                |
| description    | string   | 200       | Yes      |                                              |
| activeStatus   | enum     | 1         | No       | `A`=Active, `I`=Inactive                    |

---

## Shared Enumerations

### licenseType
| Value | Meaning   |
|-------|-----------|
| `L`   | Learner   |
| `P`   | Probation |
| `O`   | Open      |

### applicationStatus
| Value | Meaning                          |
|-------|----------------------------------|
| `PE`  | Pending                          |
| `EC`  | Eligibility checked              |
| `HC`  | History checked                  |
| `PA`  | Payment approved                 |
| `A2`  | Awaiting second approval         |
| `AP`  | Fully approved                   |
| `IS`  | Issued                           |
| `RE`  | Rejected                         |

### checkStatus (eligibility / history)
| Value | Meaning    |
|-------|-----------|
| `P`   | Pass       |
| `F`   | Fail       |
| `U`   | Unchecked  |

### approvalStatus
| Value | Meaning  |
|-------|---------|
| `A`   | Approved |
| `R`   | Rejected |
| `U`   | Pending  |
