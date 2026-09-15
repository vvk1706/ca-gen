# DLIS — Driver License Issuance System

**Platform:** IBM z/OS · CICS/TS · DB2 for z/OS  
**Language:** COBOL (IBM Enterprise COBOL)  
**Screen technology:** BMS 3270 (24×80)

---

## What the system does

DLIS is an online transaction processing system that manages the full lifecycle of driver licence applications — from registering a candidate through eligibility and history checking, fee payment, dual-authority approval, and final licence issuance.

---

## Documentation index

| Document | Contents |
|---|---|
| [01-MF-ARCHITECTURE.md](01-MF-ARCHITECTURE.md) | System layers, CICS program map, transaction routing, COMMAREA flow |
| [02-MF-DATA-MODEL.md](02-MF-DATA-MODEL.md) | DB2 schema, table definitions, foreign keys, indexes, check constraints |
| [03-MF-PROGRAMS.md](03-MF-PROGRAMS.md) | Detailed per-program business logic extracted from COBOL source |
| [04-MF-SCREENS.md](04-MF-SCREENS.md) | BMS map layouts, screen fields, PF-key assignments |
| [05-MF-BUILD-DEPLOY.md](05-MF-BUILD-DEPLOY.md) | JCL jobs, PROCs, CSD definitions, full build & deploy sequence |

---

## Quick reference — transactions

| Trans | Program | Map | Description |
|---|---|---|---|
| DL01 | DLISMENU | DLISMM / DLISMM01 | Main menu — routes to all other transactions |
| DL02 | DLISCAND | DLISCM / DLISCM01 | Candidate maintenance (create / update / inquire / deactivate) |
| DL03 | DLISAPPL | DLISAE / DLISAE01 | Licence application entry |
| DL04 | DLISSTAT | DLISST / DLISST01 | Application status inquiry |
| DL05 | DLISELIG | DLISEC / DLISEC01 | Eligibility check (age + prior licence) |
| DL06 | DLISHIST | DLISHC / DLISHC01 | Driving history check (demerits, suspensions, fines) |
| DL07 | DLISPAY  | DLISPE / DLISPE01 | Payment entry and receipt |
| DL08 | DLISAP1  | DLISA1 / DLISA1M01 | First authority approval |
| DL09 | DLISAP2  | DLISA2 / DLISA2M01 | Second authority approval (segregation enforced) |
| DL10 | DLISISSU | DLISLI / DLISLIM01 | Licence issuance |

---

## Application workflow (high level)

```
Candidate Registration (DL02)
        │
        ▼
Application Submission (DL03)   ── status PE (Pending)
        │
        ▼
Eligibility Check (DL05)        ── status EC (pass) or RE (fail)
        │
        ▼
Driving History Check (DL06)    ── status HC (pass) or RE (fail)
        │
        ▼
Payment Entry (DL07)            ── status PA (paid)
        │
        ▼
First Authority Approval (DL08) ── status A2 (approved) or RE (rejected)
        │
        ▼
Second Authority Approval (DL09)── status AP (fully approved) or RE (rejected)
        │
        ▼
Licence Issuance (DL10)         ── status IS (issued)
```

---

## Source directory layout

```
mf/
├── bms/          BMS mapset source (3270 screen definitions)
│   ├── DLISMM.bms
│   ├── DLISCM.bms
│   ├── DLISAE.bms
│   └── DLISECHPE.bms   (DLISEC + DLISHC + DLISPE combined)
├── cobol/        COBOL/CICS program source
│   ├── DLISMENU.cbl    DLISCAND.cbl    DLISAPPL.cbl
│   ├── DLISSTAT.cbl    DLISELIG.cbl    DLISHIST.cbl
│   ├── DLISPAY.cbl     DLISAP1.cbl     DLISAP2.cbl
│   └── DLISISSU.cbl
├── copybook/     Shared COBOL copybooks
│   ├── DLISWS.cpy      Working storage (candidate, application, payment…)
│   ├── DLISCSEC.cpy    CICS COMMAREA layouts
│   └── DLISDCLG.cpy    DB2 host variable (DCLGEN) layouts
├── csd/          CICS CSD resource definitions
│   └── DLISCSD.csd
├── db2/          DB2 DDL
│   ├── DLIS0001-CREATE-TABLES.sql
│   ├── DLIS0002-CREATE-INDEXES.sql
│   └── DLIS0003-GRANTS.sql
├── jcl/          Batch JCL jobs
│   ├── DLISDB2.jcl     DLISBMS.jcl
│   ├── DLISCOMP.jcl    DLISBIND.jcl
│   └── DLISCSD.jcl
└── proc/         JCL catalogued procedures
    ├── DLISCLNK.proc   (DB2 precompile + COBOL compile + link-edit)
    └── DLISBASM.proc   (BMS assembly phases 1 & 2 + link-edit)
```
