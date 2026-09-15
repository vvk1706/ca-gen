# ODM DLIS — Driver License Issuance System

IBM Operational Decision Manager implementation of the DLIS application.
**Converted from:** IBM CA Gen DLIS encyclopedia (9 action blocks, 29 business rules, 7 entities).

---

## Quick Start

```bash
# Build BOM and service layer
mvn clean install

# Deploy rules to RES (see docs/07-DEPLOYMENT-GUIDE.md for full steps)
# Run eligibility check
curl -X POST http://localhost:9080/api/dlis/applications/1001/eligibility-check \
  -H "Content-Type: application/json" -d '{"checkedBy":"OFFICER1"}'
```

---

## Project Structure

```
odm-from-cagen/
├── bom/                          # Business Object Model (Java)
│   └── src/main/java/com/dlis/odm/model/
│       ├── Candidate.java         ← CANDIDATE.ENT
│       ├── LicenseApplication.java← LICENSE-APPLICATION.ENT
│       ├── DrivingHistory.java    ← DRIVING-HISTORY.ENT
│       ├── Payment.java           ← PAYMENT.ENT
│       ├── IssuedLicense.java     ← ISSUED-LICENSE.ENT
│       ├── AuthorityUser.java     ← AUTHORITY-USER.ENT
│       ├── LicenseFeeSchedule.java← LICENSE-FEE-SCHEDULE.ENT
│       ├── RuleContext.java       ← Session/working storage fact
│       └── RuleViolation.java     ← Error accumulator
│
├── ruleflows/                     # ODM Ruleflow XML
│   ├── DLIS-Main-Workflow.rflo    ← Full 8-stage pipeline
│   ├── DLIS-Eligibility-Check.rflo← AB-CHECK-ELIGIBILITY
│   ├── DLIS-History-Check.rflo   ← AB-CHECK-HISTORY
│   ├── DLIS-Payment-Processing.rflo← AB-PROCESS-PAYMENT
│   ├── DLIS-Approval-Level1.rflo ← AB-RECORD-APPROVAL-1
│   ├── DLIS-Approval-Level2.rflo ← AB-RECORD-APPROVAL-2
│   └── DLIS-License-Issuance.rflo← AB-ISSUE-LICENSE
│
├── rules/                         # BAL Business Rules
│   ├── candidate/candidate-rules.brl    (BR-07,08,09,27)
│   ├── eligibility/eligibility-rules.brl(BR-01 to BR-06)
│   ├── history/history-rules.brl        (BR-10 to BR-13)
│   ├── payment/payment-rules.brl        (BR-14,22,23,28)
│   ├── approval/approval-rules.brl      (BR-15,16,18-21)
│   └── issuance/issuance-rules.brl      (BR-17,24-26,29)
│
├── decision-tables/               # ODM Decision Tables
│   ├── LicenseAgeAndValidityTable.dtt  (BR-02,03,04,24)
│   └── LicenseUpgradePathTable.dtt     (BR-05,06)
│
├── mainframe/
│   ├── cics/
│   │   ├── DLISQLUP.cbl           ← Pre-rule DB2 data loader
│   │   ├── DLISPAY.cbl            ← Payment DB2 writer
│   │   └── DLISLICS.cbl           ← License issuance DB2 writer
│   └── db2/
│       └── DLIS-DDL.sql           ← All 7 tables for IBM Db2 z/OS
│
├── service/                       # Java REST + CICS adapter
│   └── src/main/java/com/dlis/odm/
│       ├── rest/DlisRuleResource.java  ← JAX-RS endpoints
│       └── service/ExciCicsAdapter.java← EXCI bridge
│
├── docs/                          # Documentation
│   ├── README.md                  ← Index
│   ├── 01-CONVERSION-OVERVIEW.md  ← Migration analysis
│   ├── 02-ARCHITECTURE.md         ← System design
│   ├── 03-BOM-REFERENCE.md        ← All BOM classes
│   ├── 04-RULEFLOW-REFERENCE.md   ← All ruleflows
│   ├── 05-BUSINESS-RULES-CATALOGUE.md ← All 29 rules
│   ├── 06-MAINFRAME-INTEGRATION.md    ← CICS/DB2/EXCI
│   └── 07-DEPLOYMENT-GUIDE.md     ← Step-by-step deployment
│
├── dlis-rules.ruleproject         ← ODM Rule Designer import
└── pom.xml                        ← Maven parent POM
```

---

## Business Rules Coverage

| Domain | Rules | Count |
|---|---|---|
| License Type | BR-01 | 1 |
| Candidate | BR-07, BR-08, BR-09, BR-27 | 4 |
| Eligibility (age + upgrade) | BR-02, BR-03, BR-04, BR-05, BR-06 | 5 |
| History (suspension, demerits, fines) | BR-10, BR-11, BR-12, BR-13 | 4 |
| Sequential gates | BR-14, BR-15, BR-16, BR-17 | 4 |
| Approval (level, auth, segregation) | BR-18, BR-19, BR-20, BR-21 | 4 |
| Payment | BR-22, BR-23, BR-28 | 3 |
| Issuance (expiry, vehicle class, demerits) | BR-24, BR-25, BR-26, BR-29 | 4 |
| **Total** | | **29** |

---

## REST API Summary

| Method | Endpoint | CA Gen Origin |
|---|---|---|
| `POST` | `/api/dlis/candidates` | AB-CREATE-CANDIDATE |
| `POST` | `/api/dlis/applications` | AB-CREATE-APPLICATION |
| `POST` | `/api/dlis/applications/{id}/eligibility-check` | AB-CHECK-ELIGIBILITY |
| `POST` | `/api/dlis/applications/{id}/history-check` | AB-CHECK-HISTORY |
| `POST` | `/api/dlis/applications/{id}/payment` | AB-PROCESS-PAYMENT |
| `POST` | `/api/dlis/applications/{id}/approvals/1` | AB-RECORD-APPROVAL-1 |
| `POST` | `/api/dlis/applications/{id}/approvals/2` | AB-RECORD-APPROVAL-2 |
| `POST` | `/api/dlis/applications/{id}/issue` | AB-ISSUE-LICENSE |
| `GET`  | `/api/dlis/applications/{id}/status` | AB-INQUIRE-APPLICATION-STATUS |

---

## Documentation

Full documentation is in [`docs/`](docs/README.md).
