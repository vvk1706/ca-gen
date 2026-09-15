# 02 — Architecture
## ODM DLIS Application Architecture

---

## 1. System Architecture Overview

The ODM DLIS architecture is a three-tier hybrid: an ODM rule engine tier, a mainframe tier, and a REST API tier.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT TIER                                  │
│   Modern Web App / Mobile / Batch Trigger                           │
│   HTTP/JSON  →  REST API  (DlisRuleResource.java)                   │
└───────────────────────────────┬─────────────────────────────────────┘
                                │  JAX-RS / JSON
┌───────────────────────────────▼─────────────────────────────────────┐
│                    ODM RULE ENGINE TIER                              │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  DlisRuleExecutionService                                    │    │
│  │  1. Load RuleContext (call CICS DLISQLUP for DB2 data)       │    │
│  │  2. Execute ODM Ruleset (call RES REST API)                  │    │
│  │  3. On success: call CICS adapter for DB2 writes             │    │
│  └──────────┬────────────────────────────┬────────────────────┘    │
│             │                            │                           │
│  ┌──────────▼──────────┐    ┌────────────▼────────────────────┐    │
│  │  IBM ODM Rule        │    │  ExciCicsAdapter.java           │    │
│  │  Execution Server    │    │  EXCI bridge via CTG            │    │
│  │  (RES)               │    └────────────────────────────────┘    │
│  │  ┌───────────────┐   │                                           │
│  │  │ Ruleflows     │   │                                           │
│  │  │ BAL Rules     │   │                                           │
│  │  │ Decision      │   │                                           │
│  │  │ Tables        │   │                                           │
│  │  └───────────────┘   │                                           │
│  └─────────────────────┘                                            │
└───────────────────────────────┬─────────────────────────────────────┘
                                │  EXCI TCP/IP (IBM CICS TG)
┌───────────────────────────────▼─────────────────────────────────────┐
│                     MAINFRAME TIER  (z/OS)                          │
│                                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐    │
│  │  DLISQLUP    │   │  DLISPAY     │   │  DLISLICS            │    │
│  │  CICS COBOL  │   │  CICS COBOL  │   │  CICS COBOL          │    │
│  │  DB2 Reads   │   │  DB2 Writes  │   │  DB2 Writes          │    │
│  │  (pre-rule   │   │  PAYMENT     │   │  ISSUED_LICENSE      │    │
│  │  data load)  │   │  APPLICATION │   │  APPLICATION         │    │
│  └──────┬───────┘   └──────┬───────┘   └──────────┬───────────┘    │
│         │                  │                       │                 │
│  ┌──────▼──────────────────▼───────────────────────▼────────────┐   │
│  │           IBM Db2 for z/OS — Schema: DLIS                     │   │
│  │  CANDIDATE  LICENSE_APPLICATION  DRIVING_HISTORY  PAYMENT     │   │
│  │  ISSUED_LICENSE  AUTHORITY_USER  LICENSE_FEE_SCHEDULE         │   │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. ODM Component Breakdown

### 2.1 Business Object Model (BOM)
Located in: `bom/src/main/java/com/dlis/odm/model/`

| Class | Role | CA Gen Origin |
|---|---|---|
| `Candidate` | Candidate data fact | `CANDIDATE.ENT` |
| `LicenseApplication` | Central workflow fact | `LICENSE-APPLICATION.ENT` |
| `DrivingHistory` | History records list | `DRIVING-HISTORY.ENT` |
| `Payment` | Payment record | `PAYMENT.ENT` |
| `IssuedLicense` | Issued license output | `ISSUED-LICENSE.ENT` |
| `AuthorityUser` | Approval officer | `AUTHORITY-USER.ENT` |
| `LicenseFeeSchedule` | Fee lookup | `LICENSE-FEE-SCHEDULE.ENT` |
| `RuleContext` | Aggregated session fact | Combined working storage |
| `RuleViolation` | Error/violation record | Return code + message |

### 2.2 Ruleflows
Located in: `ruleflows/`

| Ruleflow | CA Gen Origin | Purpose |
|---|---|---|
| `DLIS-Main-Workflow` | Full pipeline | Top-level orchestrator |
| `DLIS-Eligibility-Check` | AB-CHECK-ELIGIBILITY | Age + upgrade path |
| `DLIS-History-Check` | AB-CHECK-HISTORY | Suspension, demerits, fines |
| `DLIS-Payment-Processing` | AB-PROCESS-PAYMENT | Fee validation + payment |
| `DLIS-Approval-Level1` | AB-RECORD-APPROVAL-1 | First authority approval |
| `DLIS-Approval-Level2` | AB-RECORD-APPROVAL-2 | Second authority approval |
| `DLIS-License-Issuance` | AB-ISSUE-LICENSE | Final issuance + expiry |

### 2.3 Rule Packages
Located in: `rules/`

| Package | Rules | Description |
|---|---|---|
| `candidate` | BR-07, BR-08, BR-09, BR-27 | Candidate and application creation |
| `eligibility` | BR-01 to BR-06 | Eligibility checks |
| `history` | BR-10 to BR-13 | History checks |
| `payment` | BR-14, BR-22, BR-23, BR-28 | Payment processing |
| `approval` | BR-15, BR-16, BR-18 to BR-21 | Dual approval flow |
| `issuance` | BR-17, BR-24 to BR-26, BR-29 | License issuance |

### 2.4 Decision Tables
Located in: `decision-tables/`

| Table | Replaces | Rules |
|---|---|---|
| `LicenseAgeAndValidityTable` | 3 IF-THEN age chains + 3 IF-THEN validity chains | BR-02, BR-03, BR-04, BR-24 |
| `LicenseUpgradePathTable` | 2 IF-THEN upgrade path chains | BR-05, BR-06 |

---

## 3. Data Flow — Per Request

For every REST API call, the following sequence executes:

```
Client
  │
  │  POST /applications/{id}/eligibility-check
  ▼
DlisRuleResource.runEligibilityCheck()
  │
  │  loadContextForApplication(applicationId)
  ▼
ExciCicsAdapter.link("DLISQLUP", commarea)      ← CICS: DB2 reads
  │  Returns: application, candidate, fee schedule, existing licenses
  │
  ▼
RuleContext populated with DB2 data
  │
  │  executeRuleset("eligibility/eligibility-rules", context)
  ▼
ODM Rule Execution Server
  │  Fires: BR-02 / BR-03 / BR-04  (age check)
  │  Fires: BR-05 / BR-06           (upgrade path)
  │  Fires: ELIG-REC-001/002        (record pass/fail)
  │
  ▼
RuleContext updated with result
  │
  │  (if no violations)
  │  persistApplicationStatus(result)
  ▼
ExciCicsAdapter.link("DLISUPD", commarea)       ← CICS: DB2 UPDATE
  │  Updates: APPLICATION_STATUS, ELIGIBILITY_CHECK_STATUS
  │
  ▼
Response JSON returned to client
```

---

## 4. Separation of Concerns

A key architectural improvement over the CA Gen design is strict separation:

| Concern | CA Gen | ODM Architecture |
|---|---|---|
| Business rule evaluation | Mixed into action blocks with I/O | Pure ODM rulesets — no I/O |
| Data retrieval | Done inside action blocks via views | Done by DLISQLUP before ODM call |
| Data persistence | Done inside action blocks via views | Done by DLISPAY/DLISLICS after ODM |
| Rule change deployment | Requires full CA Gen regeneration | Hot-deployed via Decision Center |
| Rule audit history | None | Full version history in Decision Center |

---

## 5. Mainframe Integration Pattern

The mainframe infrastructure is used for two purposes:

**Pre-rule data loading** (read path):
- `DLISQLUP` is invoked before every ODM ruleset call
- Loads: application, candidate, history records, fee schedule, existing licenses
- Returns a fixed-format COMMAREA that the Java layer deserialises into `RuleContext`

**Post-rule transactional writes** (write path):
- `DLISPAY` — invoked after payment rules pass → INSERT PAYMENT + UPDATE APPLICATION
- `DLISLICS` — invoked after issuance rules pass → INSERT ISSUED_LICENSE + UPDATE APPLICATION
- All other status updates use a generic `DLISUPD` program

This pattern ensures:
1. ODM rules are stateless and side-effect-free (pure evaluation)
2. DB2 writes only occur after rules have confirmed business validity
3. The existing z/OS/CICS/DB2 infrastructure investment is fully preserved
