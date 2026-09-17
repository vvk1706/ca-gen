# Driver License Issuance System (DLIS)
## CA Gen Project — Documentation Index

---

### Overview

The **Driver License Issuance System (DLIS)** is a CA Gen model-driven application that manages the end-to-end process of issuing driver licenses. It supports three license types (Learner, Probation, Open) and enforces a structured workflow of eligibility checking, driving history review, payment, dual-authority approval, and final license issuance.

---

### Documentation Index

| Document | Description |
|---|---|
| [01-ARCHITECTURE.md](01-ARCHITECTURE.md) | System architecture, Entity-Relationship Diagram, layer overview |
| [02-WORKFLOW.md](02-WORKFLOW.md) | End-to-end process flow, workflow state machine, business rules |
| [03-SCREEN-NAVIGATION.md](03-SCREEN-NAVIGATION.md) | Screen map, navigation flows, PF-key reference |
| [04-ACTION-BLOCK-REFERENCE.md](04-ACTION-BLOCK-REFERENCE.md) | All action blocks — imports, exports, logic summary |
| [05-BUSINESS-RULES.md](05-BUSINESS-RULES.md) | Complete business rules catalogue |
| [06-DATA-DICTIONARY.md](06-DATA-DICTIONARY.md) | Full data dictionary for all entities and attributes |
| [07-DEPLOYMENT-GUIDE.md](07-DEPLOYMENT-GUIDE.md) | CA Gen generation, deployment and environment setup |

---

### Quick Start — Workflow Summary

```
Register Candidate  →  Create Application  →  Eligibility Check
→  History Check  →  Payment  →  First Authority Approval
→  Second Authority Approval  →  Issue License
```

---

### License Types

| Code | Name | Min Age | Validity | Prerequisite |
|---|---|---|---|---|
| L | Learner | 16 | 1 year | None |
| P | Probation | 17 | 2 years | Active Learner license |
| O | Open | 18 | 5 years | Active Probation license |

---

### Project Structure

```
ca-gen/
├── docs/                          ← This documentation
│   ├── README.md
│   ├── 01-ARCHITECTURE.md
│   ├── 02-WORKFLOW.md
│   ├── 03-SCREEN-NAVIGATION.md
│   ├── 04-ACTION-BLOCK-REFERENCE.md
│   ├── 05-BUSINESS-RULES.md
│   ├── 06-DATA-DICTIONARY.md
│   └── 07-DEPLOYMENT-GUIDE.md
├── src/
│   ├── encyclopedia/
│   │   ├── entities/              ← 7 entity definitions (.ENT)
│   │   ├── views/                 ← 27 views across 6 files (.VEW)
│   │   └── functions/             ← Business function decomposition (.BFN)
│   ├── action-blocks/             ← 9 action blocks (.ACB)
│   ├── screens/                   ← 10 screen maps (.SCR)
│   └── triggers/                  ← 4 trigger files (.TRG, 19 triggers total)
├── mf/                            ← Mainframe target implementation
│   ├── bms/                       ← BMS screen definitions (5 maps)
│   ├── cobol/                     ← Generated COBOL programs (10 modules)
│   ├── copybook/                  ← Shared copybooks (3 files)
│   ├── csd/                       ← CICS resource definitions
│   ├── db2/                       ← DB2 DDL scripts (3 SQL files)
│   ├── jcl/                       ← JCL build and deployment jobs (5 files)
│   ├── proc/                      ← JCL procedure library (2 procs)
│   └── docs/                      ← Mainframe-specific documentation
├── odm-from-cagen/                ← IBM ODM rule project extracted from CA Gen model
│   ├── bom/                       ← Business Object Model (Java)
│   ├── decision-tables/           ← ODM decision tables (2 .dtt files)
│   ├── ruleflows/                 ← Rule execution flows (7 .rflo files)
│   ├── rules/                     ← Rule artefacts by domain (6 .brl files)
│   ├── mainframe/                 ← Mainframe CICS/DB2 integration stubs
│   ├── service/                   ← REST rule service (Java)
│   └── docs/                      ← ODM conversion documentation
└── reports/
    └── various analysis and comparison reports
```
