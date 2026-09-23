# DLIS — Driver License Issuance System: API Overview

## System Description

DLIS is a CICS/COBOL mainframe application that manages the full lifecycle of driver licence applications — from candidate registration through eligibility and history checking, fee payment, dual-authority approval, and final licence issuance. It runs on IBM z/OS with CICS transaction processing and Db2 for z/OS persistence.

## Domain

Driver Licensing — Government / Regulatory

## Technology Stack (Mainframe)

| Layer        | Technology                     |
|-------------|--------------------------------|
| Transaction | CICS (Customer Information Control System) |
| Language    | COBOL (IBM Enterprise COBOL)   |
| Database    | IBM Db2 for z/OS               |
| Screen maps | BMS (Basic Mapping Support)    |
| Schema      | DLIS (Db2 schema)              |

---

## Programme / Transaction Map

| Menu Option | CICS Trans | Program    | Function                         |
|------------|-----------|-----------|----------------------------------|
| 1, 2       | DL02      | DLISCAND  | Candidate Maintenance (Create / Update / Inquire / Deactivate) |
| 3          | DL03      | DLISAPPL  | License Application Entry        |
| 4          | DL04      | DLISSTAT  | Application Status Inquiry       |
| 5          | DL05      | DLISELIG  | Eligibility Check                |
| 6          | DL06      | DLISHIST  | Driving History Check            |
| 7          | DL07      | DLISPAY   | Payment Entry and Processing     |
| 8          | DL08      | DLISAP1   | First Authority Approval         |
| 9          | DL09      | DLISAP2   | Second Authority Approval        |
| 10         | DL10      | DLISISSU  | Licence Issuance                 |
| —          | DL01      | DLISMENU  | Main Menu (routing only)         |

---

## Database Tables

| Table                     | Purpose                              |
|--------------------------|--------------------------------------|
| DLIS.CANDIDATE            | Licence applicant (person) records   |
| DLIS.LICENSE_APPLICATION  | One application per candidate/type   |
| DLIS.DRIVING_HISTORY      | Incidents, fines, suspensions        |
| DLIS.PAYMENT              | Fee payments against applications    |
| DLIS.ISSUED_LICENSE       | Issued licence records               |
| DLIS.AUTHORITY_USER       | Authorised approvers (level 1 & 2)   |
| DLIS.LICENSE_FEE_SCHEDULE | Fee amounts by licence type          |

---

## Licence Types

| Code | Name       | Min Age | Validity | Prerequisite          |
|-----|-----------|---------|---------|----------------------|
| L   | Learner    | 16      | 1 year  | None                 |
| P   | Probation  | 17      | 2 years | Active Learner (L)   |
| O   | Open       | 18      | 5 years | Active Probation (P) |

---

## Application Status Lifecycle

```
PE → EC → HC → PA → A2 → AP → IS
           ↓    ↓    ↓    ↓    ↓
           RE   RE   RE   RE   RE
```

| Code | Meaning                                |
|-----|----------------------------------------|
| PE  | Pending — awaiting eligibility check   |
| EC  | Eligibility checked — awaiting history |
| HC  | History checked — awaiting payment     |
| PA  | Payment approved — awaiting 1st auth   |
| A2  | Awaiting second authority approval     |
| AP  | Fully approved — ready for issue       |
| IS  | Licence issued                         |
| RE  | Rejected                               |

---

## API Modules (Files in this directory)

| File | Contents |
|------|----------|
| `01-OVERVIEW.md`           | This file — system map and domain context |
| `02-DATA-MODELS.md`        | All request/response data schemas         |
| `03-BUSINESS-FUNCTIONS.md` | Business function descriptions per program |
| `04-API-SPEC.yaml`         | Full OpenAPI 3.0 specification            |
| `05-WORKFLOW.md`           | End-to-end lifecycle and state machine    |
| `06-BUSINESS-RULES.md`     | All business rules extracted from COBOL   |
