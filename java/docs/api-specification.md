# DLIS — API Specification

## Table of Contents

1. [REST API](#1-rest-api)
   - [Base URL and Authentication](#11-base-url-and-authentication)
   - [Common Response Format](#12-common-response-format)
   - [Candidate Endpoints](#13-candidate-endpoints)
   - [Application Endpoints](#14-application-endpoints)
   - [Payment and Approval Endpoints](#15-payment-and-approval-endpoints)
2. [CICS Transaction Interface](#2-cics-transaction-interface)
   - [CAND — Candidate Maintenance](#21-cand--candidate-maintenance)
   - [APPL — Application Entry and Status](#22-appl--application-entry-and-status)
   - [PAAP — Payment and Approval](#23-paap--payment-and-approval)

---

## 1. REST API

### 1.1 Base URL and Authentication

```
Base URL:  https://<host>:9443/dlis/api/v1
Protocol:  HTTPS (TLS 1.2+)
Auth:      HTTP Basic or container-managed form authentication
```

All endpoints require an authenticated user belonging to one of the roles defined below.

| Role | Permitted Endpoints |
|---|---|
| `DLIS_OFFICER` | All candidate, application, and payment endpoints |
| `DLIS_AUTHORITY` | Approval and issue endpoints (`/approval1`, `/approval2`, `/issue`) |
| `DLIS_ADMIN` | All endpoints |

Content type for all requests and responses: `application/json`.

---

### 1.2 Common Response Format

#### Success — 2xx

Successful `POST` creating a resource returns `HTTP 201 Created`.
Successful `GET` and action endpoints return `HTTP 200 OK`.

#### Error — 4xx / 5xx

```json
{
  "returnCode": 3,
  "message": "AN ACTIVE APPLICATION ALREADY EXISTS FOR THIS LICENSE TYPE"
}
```

| HTTP Status | Meaning |
|---|---|
| 400 Bad Request | Missing or malformed request body or query parameter |
| 404 Not Found | Resource not found (`returnCode: 1`) |
| 422 Unprocessable Entity | Business rule violation (`returnCode: 2–99`) |
| 500 Internal Server Error | Unexpected server error |

---

### 1.3 Candidate Endpoints

#### `POST /candidates` — Create Candidate

Creates a new license candidate. Candidates are Queensland, Australia residents.

**Request Body**

```json
{
  "firstName":    "Jane",
  "lastName":     "Smith",
  "dateOfBirth":  "1995-06-15",
  "idNumber":     "12345678",
  "addressLine1": "45 Oak Street",
  "addressLine2": "",
  "city":         "Brisbane",
  "stateProvince":"Queensland",
  "postalCode":   "4000",
  "country":      "AU",
  "phoneNumber":  "+61-7-3000-0100",
  "emailAddress": "jane.smith@example.com",
  "createdBy":    "OFFICER1"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `firstName` | string | yes | Given name (max 30 chars) |
| `lastName` | string | yes | Family name (max 30 chars) |
| `dateOfBirth` | string (ISO-8601) | yes | Format `YYYY-MM-DD`; must be in the past |
| `idNumber` | string | yes | Queensland driver licence number or QLD proof-of-age card number (max 20 chars); must be unique among active candidates |
| `addressLine1` | string | yes | First address line (max 50 chars) |
| `addressLine2` | string | no | Second address line (max 50 chars) |
| `city` | string | yes | City / suburb (max 30 chars) |
| `stateProvince` | string | yes | Australian state or territory — typically `Queensland` (max 30 chars) |
| `postalCode` | string | yes | Australian 4-digit postcode (max 10 chars) |
| `country` | string | yes | Country — `AU` for Australia (max 30 chars) |
| `phoneNumber` | string | no | Australian phone number, e.g. `+61-7-xxxx-xxxx` (max 15 chars) |
| `emailAddress` | string | no | Email address (max 60 chars) |
| `createdBy` | string | no | User ID; defaults to `"API"` |

**Response — 201 Created**

```json
{
  "id":      42,
  "message": "CANDIDATE CREATED SUCCESSFULLY"
}
```

**Error Responses**

| HTTP | `returnCode` | Message |
|---|---|---|
| 422 | 1 | DUPLICATE CANDIDATE ID NUMBER |
| 422 | 2 | INVALID DATE OF BIRTH |
| 400 | — | Invalid dateOfBirth format — use YYYY-MM-DD |

---

#### `GET /candidates/{id}` — Get Candidate by Primary Key

**Path Parameter:** `id` — numeric candidate ID.

**Response — 200 OK**

Returns the full `Candidate` domain object as JSON. Example:

```json
{
  "candidateId":  42,
  "firstName":    "Jane",
  "lastName":     "Smith",
  "dateOfBirth":  "1995-06-15",
  "idNumber":     "12345678",
  "addressLine1": "45 Oak Street",
  "city":         "Brisbane",
  "stateProvince":"Queensland",
  "postalCode":   "4000",
  "country":      "AU",
  "phoneNumber":  "+61-7-3000-0100",
  "emailAddress": "jane.smith@example.com",
  "createdDate":  "2024-03-01",
  "recordStatus": "A"
}
```

**Error Responses:** 404 if not found.

---

#### `GET /candidates?idNumber={idNumber}` — Find by Queensland ID Number

**Query Parameter:** `idNumber` — the candidate's Queensland driver licence number or proof-of-age card number.

Returns the matching active candidate or `404` if none is found.

---

### 1.4 Application Endpoints

#### `POST /applications` — Create Application

Initiates a new license application for a candidate.

**Request Body**

```json
{
  "candidateId": 42,
  "licenseType": "L",
  "createdBy":   "OFFICER1"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `candidateId` | number | yes | Primary key of an active candidate |
| `licenseType` | string | yes | `L` Learner, `P` Probation, `O` Open |
| `createdBy` | string | no | User ID; defaults to `"API"` |

**Response — 201 Created**

```json
{
  "id":      1001,
  "message": "APPLICATION CREATED SUCCESSFULLY"
}
```

**Error Responses**

| HTTP | `returnCode` | Message |
|---|---|---|
| 404 | 1 | CANDIDATE NOT FOUND OR INACTIVE |
| 422 | 2 | INVALID LICENSE TYPE. MUST BE L, P OR O |
| 422 | 3 | AN ACTIVE APPLICATION ALREADY EXISTS FOR THIS LICENSE TYPE |

---

#### `GET /applications/{id}` — Inquire Application Status

Returns the full `LicenseApplication` domain object including all check results, payment, and approval fields.

**Response — 200 OK**

```json
{
  "applicationId":        1001,
  "candidateId":          42,
  "licenseType":          "L",
  "applicationDate":      "2024-03-15",
  "applicationStatus":    "HC",
  "eligibilityChkStatus": "P",
  "eligibilityChkDate":   "2024-03-16",
  "historyChkStatus":     "P",
  "historyChkDate":       "2024-03-17",
  "paymentStatus":        "U",
  "approval1Status":      "U",
  "approval2Status":      "U",
  "createdDate":          "2024-03-15",
  "lastUpdatedDate":      "2024-03-17",
  "createdBy":            "OFFICER1",
  "lastUpdatedBy":        "OFFICER2"
}
```

**Error Responses:** 404 if not found.

---

#### `POST /applications/{id}/eligibility-check` — Run Eligibility Check

Performs the eligibility check for the specified application.

**Request Body** _(optional)_

```json
{
  "checkedBy": "OFFICER1"
}
```

**Response — 200 OK**

```json
{
  "status":  "P",
  "message": "ELIGIBILITY CHECK PASSED"
}
```

On failure the application is rejected and `status` = `"F"`:

```json
{
  "status":  "F",
  "message": "CANDIDATE MUST BE AT LEAST 16 YEARS OLD FOR LEARNER LICENSE"
}
```

**Error Responses:** 404 if application not found.

---

#### `POST /applications/{id}/history-check` — Run History Check

Performs the driving history check. Eligibility must have passed first.

**Request Body** _(optional)_

```json
{
  "checkedBy": "OFFICER1"
}
```

**Response — 200 OK**

```json
{
  "status":  "P",
  "message": "HISTORY CHECK PASSED"
}
```

**Error Responses**

| HTTP | `returnCode` | Message |
|---|---|---|
| 404 | 1 | APPLICATION NOT FOUND |
| 422 | 2 | ELIGIBILITY CHECK MUST BE PASSED BEFORE HISTORY CHECK |

---

### 1.5 Payment and Approval Endpoints

#### `POST /applications/{id}/payment` — Process Payment

Records the license fee payment for the application.

**Request Body**

```json
{
  "candidateId":       42,
  "paymentMethod":     "CC",
  "paymentReference":  "TXN-20240318-9921",
  "processedBy":       "OFFICER1"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `candidateId` | number | yes | Must match the application's candidate |
| `paymentMethod` | string | yes | `CC` Credit Card, `DC` Debit Card, `EF` EFT, `CS` Cash, `CH` Cheque |
| `paymentReference` | string | yes | External payment reference (max 30 chars) |
| `processedBy` | string | no | User ID; defaults to `"API"` |

**Response — 200 OK**

```json
{
  "paymentId":     501,
  "receiptNumber": "REC-0042-1001-20240318",
  "message":       "PAYMENT PROCESSED SUCCESSFULLY"
}
```

**Error Responses**

| HTTP | `returnCode` | Message |
|---|---|---|
| 404 | 1 | APPLICATION NOT FOUND |
| 422 | 2 | FEE SCHEDULE NOT FOUND |
| 422 | 3 | PAYMENT ALREADY RECORDED |

---

#### `POST /applications/{id}/approval1` — Record First Approval

Records the first-level authority decision.

**Required role:** `DLIS_AUTHORITY`

**Request Body**

```json
{
  "authorityUserCode": "AUTH01",
  "decision":          "A",
  "decisionNotes":     "All documentation verified and in order."
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `authorityUserCode` | string | yes | Authority user's unique code |
| `decision` | string | yes | `A` = Approve, `R` = Reject |
| `decisionNotes` | string | no | Notes (max 200 chars) |

**Response — 200 OK (approved)**

```json
{
  "message": "FIRST APPROVAL RECORDED - APPLICATION FORWARDED FOR SECOND APPROVAL"
}
```

**Response — 200 OK (rejected)**

```json
{
  "message": "FIRST APPROVAL REJECTED - APPLICATION CLOSED"
}
```

**Error Responses**

| HTTP | `returnCode` | Message |
|---|---|---|
| 404 | 1 | APPLICATION NOT FOUND / AUTHORITY USER NOT FOUND |
| 422 | 4 | PAYMENT MUST BE COMPLETED BEFORE FIRST APPROVAL |
| 422 | 3 | AUTHORITY USER IS NOT AUTHORISED FOR THIS LICENSE TYPE |
| 422 | 5 | DECISION MUST BE A=APPROVE OR R=REJECT |

---

#### `POST /applications/{id}/approval2` — Record Second Approval

Records the second-level authority decision. Application must be in `A2` status.

**Required role:** `DLIS_AUTHORITY`

**Request Body** — same structure as `/approval1`.

**Response — 200 OK (approved)**

```json
{
  "message": "SECOND APPROVAL GRANTED - APPLICATION FULLY APPROVED AND READY FOR ISSUE"
}
```

**Error Responses** — same as `/approval1` plus:

| HTTP | `returnCode` | Message |
|---|---|---|
| 422 | 2 | APPLICATION IS NOT IN A2 STATUS - FIRST APPROVAL NOT YET GRANTED |

---

#### `POST /applications/{id}/issue` — Issue License

Issues the physical license record. Application must be in `AP` (Approved) status.

**Request Body**

```json
{
  "vehicleClass":      "B",
  "restrictions":      "Corrective lenses required",
  "issuedByOfficer":   "OFFICER2",
  "issuedByAuthority": "Licensing Authority District 4"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `vehicleClass` | string | yes | `A` Motorcycle, `B` Light vehicle, `C` Heavy vehicle, `D` Bus |
| `restrictions` | string | no | Any license conditions or restrictions |
| `issuedByOfficer` | string | yes | Issuing officer's name/ID |
| `issuedByAuthority` | string | yes | Issuing authority name |

**Response — 201 Created**

Returns the full `IssuedLicense` object:

```json
{
  "licenseId":        701,
  "applicationId":    1001,
  "candidateId":      42,
  "licenseNumber":    "DL-42-1001-20240320",
  "licenseType":      "L",
  "issueDate":        "2024-03-20",
  "expiryDate":       "2025-03-20",
  "licenseStatus":    "A",
  "vehicleClass":     "B",
  "restrictions":     "Corrective lenses required",
  "demeritBalance":   12,
  "issuedByAuthority":"Licensing Authority District 4",
  "issuedByOfficer":  "OFFICER2",
  "renewalCount":     0
}
```

---

## 2. CICS Transaction Interface

All CICS programs are registered in the `DLISLRTY` Liberty JVM Server. They use fixed-length EBCDIC commareas. Character encoding: IBM-1047 (EBCDIC). Numeric fields are right-justified and zero-padded. Character fields are space-padded.

---

### 2.1 CAND — Candidate Maintenance

**Transaction ID:** `CAND`  
**Program:** `DLISCAND`  
**Java class:** `com.dlis.cics.program.DlisCandProgram`  
**COMMAREA size:** 396 bytes

#### Function Codes

| Code | Operation | Service Method Called |
|---|---|---|
| `CR` | Create candidate | `CandidateService.createCandidate()` |
| `INQ` | Inquire by ID number | `CandidateService.findByIdNumber()` |
| `UPD` | Update candidate | _(status update)_ |

#### COMMAREA Layout

| Offset | Length | Field | Type | Notes |
|---|---|---|---|---|
| 0 | 2 | Function code | CHAR | `CR`, `INQ`, `UPD` |
| 2 | 2 | Return code | CHAR | `00`=OK, `01`–`99`=error |
| 4 | 10 | Candidate ID | NUM | Right-justified, zero-padded |
| 14 | 30 | First name | CHAR | Space-padded |
| 44 | 30 | Last name | CHAR | Space-padded |
| 74 | 8 | Date of birth | CHAR | Format `YYYYMMDD` |
| 82 | 12 | (reserved) | — | — |
| 94 | 20 | ID number | CHAR | National ID |
| 114 | 50 | Address line 1 | CHAR | Space-padded |
| 164 | 50 | Address line 2 | CHAR | Space-padded |
| 214 | 30 | City | CHAR | Space-padded |
| 244 | 30 | State/Province | CHAR | Space-padded |
| 274 | 10 | Postal code | CHAR | Space-padded |
| 284 | 30 | Country | CHAR | Space-padded |
| 314 | 15 | Phone number | CHAR | Space-padded |
| 329 | 60 | Email address | CHAR | Space-padded |
| 389 | 1 | Record status | CHAR | `A`=Active, `I`=Inactive |
| 390 | 6 | Reserved | — | — |

#### Return Codes

| Code | Meaning |
|---|---|
| `00` | Success |
| `01` | Duplicate ID number |
| `02` | Invalid date of birth |
| `99` | System error |

---

### 2.2 APPL — Application Entry and Status

**Transaction ID:** `APPL`  
**Program:** `DLISAPPL`  
**Java class:** `com.dlis.cics.program.DlisApplProgram`  
**COMMAREA size:** 180 bytes

#### Function Codes

| Code | Operation | Service Method Called |
|---|---|---|
| `CRT` | Create application | `ApplicationService.createApplication()` |
| `ELG` | Run eligibility check | `ApplicationService.checkEligibility()` |
| `HST` | Run history check | `ApplicationService.checkHistory()` |
| `INQ` | Inquire application status | `ApplicationService.inquireStatus()` |

#### COMMAREA Layout (selected fields)

| Offset | Length | Field | Notes |
|---|---|---|---|
| 0 | 3 | Function code | `CRT`, `ELG`, `HST`, `INQ` |
| 3 | 2 | Return code | `00`=OK |
| 5 | 10 | Application ID | Numeric string |
| 15 | 10 | Candidate ID | Numeric string |
| 25 | 1 | License type | `L`, `P`, `O` |
| 26 | 8 | Application date | `YYYYMMDD` |
| 34 | 2 | Application status | 2-char code (PE, EC, etc.) |
| 36 | 1 | Eligibility check status | `P`, `F`, `U` |
| 37 | 8 | Eligibility check date | `YYYYMMDD` |
| 45 | 50 | Eligibility check notes | Space-padded |
| 95 | 1 | History check status | `P`, `F`, `U` |
| 96 | 8 | History check date | `YYYYMMDD` |
| 104 | 50 | History check notes | Space-padded |
| 154 | 1 | Payment status | `P`, `U`, `W` |
| 155 | 20 | Payment reference | Space-padded |
| 175 | 5 | Reserved | — |

---

### 2.3 PAAP — Payment and Approval

**Transaction ID:** `PAAP`  
**Program:** `DLISPAAP`  
**Java class:** `com.dlis.cics.program.DlisPayApprProgram`  
**COMMAREA size:** 544 bytes

#### Function Codes

| Code | Operation | Service Method Called |
|---|---|---|
| `PAY` | Process payment | `PaymentService.processPayment()` |
| `AP1` | Record first approval | `ApprovalService.recordApproval1()` |
| `AP2` | Record second approval | `ApprovalService.recordApproval2()` |
| `ISS` | Issue license | `ApprovalService.issueLicense()` |

#### COMMAREA Layout (selected fields)

| Offset | Length | Field | Notes |
|---|---|---|---|
| 0 | 3 | Function code | `PAY`, `AP1`, `AP2`, `ISS` |
| 3 | 2 | Return code | `00`=OK |
| 5 | 10 | Application ID | Numeric string |
| 15 | 10 | Candidate ID | Numeric string |
| 25 | 2 | Payment method | `CC`, `DC`, `EF`, `CS`, `CH` |
| 27 | 30 | Payment reference | Space-padded |
| 57 | 12 | Payment amount | Numeric (2 decimal places, no separator) |
| 69 | 20 | Receipt number | Space-padded |
| 89 | 20 | Authority user code | Space-padded |
| 109 | 1 | Decision | `A`=Approve, `R`=Reject |
| 110 | 200 | Decision notes | Space-padded |
| 310 | 2 | Vehicle class | Space-padded |
| 312 | 200 | Restrictions | Space-padded |
| 512 | 20 | License number | Space-padded |
| 532 | 8 | Expiry date | `YYYYMMDD` |
| 540 | 4 | Reserved | — |

#### Return Codes (all programs)

| Code | Meaning |
|---|---|
| `00` | Success |
| `01` | Not found |
| `02` | Invalid type / fee not found / prerequisite check not met |
| `03` | Already exists / not authorised |
| `04` | Payment/history prerequisite not done |
| `05` | Invalid decision |
| `99` | System error |
