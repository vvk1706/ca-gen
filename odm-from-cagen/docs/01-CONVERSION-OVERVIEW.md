# 01 — Conversion Overview
## CA Gen DLIS → IBM ODM Migration

---

## 1. Purpose and Scope

This document describes the rationale, methodology, and outcome of converting the **Driver License Issuance System (DLIS)** from IBM CA Gen to IBM Operational Decision Manager (ODM).

The original CA Gen application implements an 8-stage sequential workflow governed by 29 formally catalogued business rules. The conversion produces an ODM rule project that:

- Externalises all business rules from procedural COBOL/CICS code into manageable, auditable BAL rule artefacts
- Preserves 100% of the original business logic with traceable rule-to-source mappings
- Integrates with the existing mainframe DB2 infrastructure via a CICS EXCI adapter
- Exposes all workflow stages as RESTful endpoints for modern channel consumption

---

## 2. Why Convert from CA Gen to ODM?

| Challenge in CA Gen | Solution in ODM |
|---|---|
| Business rules embedded in generated COBOL — changes require full regeneration | Rules live in Decision Center — editable by business users without developer involvement |
| No audit trail for rule changes | Decision Center provides full version history, comments, and governance workflow |
| Testing requires full application regeneration | ODM Rule Unit Test framework enables isolated rule testing |
| Duplicate rule logic across multiple action blocks | Single authoritative rule definition, referenced from any ruleflow |
| No business-user visibility into rules | Decision Center provides a browser-based rule editor with natural language BAL |
| Tight coupling: rule logic + DB2 I/O in the same action block | Rules separated from persistence: ODM evaluates rules; CICS adapter writes to DB2 |

---

## 3. CA Gen to ODM Concept Mapping

| CA Gen Concept | CA Gen Artefact | ODM Equivalent | ODM Artefact |
|---|---|---|---|
| Data entity | `CANDIDATE.ENT` | BOM class | `Candidate.java` |
| Data view | `VAPPLICATION-ALL.VEW` | BOM class field subset | `RuleContext.application` |
| Action Block | `AB-CHECK-ELIGIBILITY.ACB` | Ruleflow + BAL rules | `DLIS-Eligibility-Check.rflo` + `eligibility-rules.brl` |
| Procedure Step | `PROC-CHECK-AGE-ELIGIBILITY` | Rule Task node | `CheckMinimumAge` (in ruleflow) |
| Working Storage | `WA-CANDIDATE-AGE` | RuleContext field | `context.candidateAge` |
| Import/Export | `WS-APPLICATION-ID` | RuleContext field | `context.application.applicationId` |
| IF-THEN condition | `IF WA-CANDIDATE-AGE < 16` | BAL `when` clause | `the candidateAge of context < 16` |
| MOVE to output | `MOVE 'F' TO WS-ELIGIBILITY-STATUS` | BAL `then` action | `set the eligibilityCheckStatus ... to "F"` |
| Error return code | `MOVE 1 TO WS-RETURN-CODE` | BAL `then` action | `set the returnCode of context to 1` |
| Decision table logic | Multiple IF-THEN chains | ODM Decision Table | `LicenseAgeAndValidityTable.dtt` |
| DB2 INSERT/UPDATE | `CREATE LV-PAYMENT-NEW` | CICS adapter | `DLISPAY.cbl` (called after ODM) |
| CICS transaction | CA Gen generated CICS code | EXCI bridge | `ExciCicsAdapter.java` |
| Screen trigger | `TRG-PAYMENT-APPROVAL-ISSUE.TRG` | REST endpoint | `POST /applications/{id}/payment` |

---

## 4. What Was NOT Changed

The following elements are preserved intact from the original CA Gen application:

| Element | Why Preserved |
|---|---|
| DB2 table structures | Converted from ENT files to DDL — identical schema |
| Business rule logic | All 29 rules converted with exact same conditions and messages |
| Application status codes | PE, EC, HC, PA, A2, AP, IS, RE — unchanged |
| Segregation of duties control | BR-21 (different approving authorities) preserved in BAL |
| Sequential gate ordering | Enforced by ruleflow gate guards (BR-13 through BR-17) |
| License validity periods | BR-24: L=1yr, P=2yr, O=5yr — unchanged |
| License number format | `{PREFIX}{CLASS}{CANDIDATE-ID}{APP-ID}` — unchanged |

---

## 5. Conversion Approach

### Phase 1 — Analysis
1. Read all `.ENT` entity definitions → produced BOM class mappings
2. Read all `.VEW` view definitions → mapped to BOM field visibility
3. Read all `.ACB` action blocks → extracted conditions, actions, return codes
4. Read `05-BUSINESS-RULES.md` → confirmed 29 business rules as formal ODM rules
5. Read `02-WORKFLOW.md` → mapped 8 pipeline steps to ruleflow topology

### Phase 2 — BOM Design
- 7 entity classes + 1 aggregating `RuleContext` + 1 `RuleViolation` utility class
- `RuleContext` is the single fact object passed into all rulesets
- All DB2 I/O result sets are pre-loaded into `RuleContext` before ODM is called

### Phase 3 — Ruleflow Design
- 1 main workflow ruleflow + 6 domain sub-ruleflows
- Each sub-ruleflow corresponds to one CA Gen action block
- Gate guard rules enforce the same sequential ordering as CA Gen

### Phase 4 — Rule Authoring
- 29 business rules + helper rules authored in BAL
- Organised into 5 rule packages: `candidate`, `eligibility`, `history`, `payment`, `approval`, `issuance`
- 2 decision tables replace repeated IF-THEN chains

### Phase 5 — Mainframe Integration
- 3 CICS programs authored in COBOL: `DLISPAY`, `DLISLICS`, `DLISQLUP`
- Java EXCI adapter bridges ODM REST layer to CICS
- DB2 DDL generated for IBM Db2 for z/OS

### Phase 6 — REST API
- JAX-RS resource class `DlisRuleResource` exposes 8 endpoints
- One endpoint per workflow stage
- ODM rule execution service orchestrates: CICS load → ODM execute → CICS write

---

## 6. Artefact Inventory

| Category | Count | Location |
|---|---|---|
| BOM Java classes | 9 | `bom/src/main/java/com/dlis/odm/model/` |
| Ruleflows | 7 | `ruleflows/` |
| BAL rule files | 5 | `rules/` |
| BAL rules (total) | 35 | (29 business + 6 helper) |
| Decision tables | 2 | `decision-tables/` |
| CICS COBOL programs | 3 | `mainframe/cics/` |
| DB2 DDL | 1 | `mainframe/db2/` |
| REST resource classes | 2 | `service/src/main/java/com/dlis/odm/rest/` |
| Documentation files | 7 | `docs/` |

---

## 7. Business Rule Traceability Summary

| ODM Rule ID | BR# | CA Gen Source |
|---|---|---|
| BR-01 | License Type Validation | AB-CREATE-APPLICATION.ACB lines 46–49 |
| BR-02 | Learner Min Age 16 | AB-CHECK-ELIGIBILITY.ACB lines 63–69 |
| BR-03 | Probation Min Age 17 | AB-CHECK-ELIGIBILITY.ACB lines 71–77 |
| BR-04 | Open Min Age 18 | AB-CHECK-ELIGIBILITY.ACB lines 79–86 |
| BR-05 | Probation Needs Learner | AB-CHECK-ELIGIBILITY.ACB lines 89–108 |
| BR-06 | Open Needs Probation | AB-CHECK-ELIGIBILITY.ACB lines 89–108 |
| BR-07 | Unique National ID | AB-CREATE-CANDIDATE.ACB lines 38–46 |
| BR-08 | DOB in Past | AB-CREATE-CANDIDATE.ACB lines 52–57 |
| BR-09 | No Duplicate Application | AB-CREATE-APPLICATION.ACB lines 52–66 |
| BR-10 | Active Suspension | AB-CHECK-HISTORY.ACB lines 65–73 |
| BR-11 | Demerit Threshold | AB-CHECK-HISTORY.ACB lines 90–94 |
| BR-12 | Unpaid Fines | AB-CHECK-HISTORY.ACB lines 78–86 |
| BR-13 | Eligibility Before History | AB-CHECK-HISTORY.ACB lines 53–58 |
| BR-14 | History Before Payment | AB-PROCESS-PAYMENT.ACB lines 49–54 |
| BR-15 | Payment Before Approval 1 | AB-RECORD-APPROVAL-1.ACB lines 51–56 |
| BR-16 | Approval 1 Before Approval 2 | AB-RECORD-APPROVAL-2.ACB lines 51–56 |
| BR-17 | All Gates Before Issuance | AB-ISSUE-LICENSE.ACB lines 62–97 |
| BR-18 | Approver Active | AB-RECORD-APPROVAL-1/2.ACB |
| BR-19 | Approver Level | AB-RECORD-APPROVAL-1/2.ACB |
| BR-20 | License Type Authorisation | AB-RECORD-APPROVAL-1/2.ACB |
| BR-21 | Different Authorities | AB-RECORD-APPROVAL-2.ACB lines 76–82 |
| BR-22 | No Duplicate Payment | AB-PROCESS-PAYMENT.ACB lines 56–60 |
| BR-23 | Active Fee Schedule | AB-PROCESS-PAYMENT.ACB lines 63–74 |
| BR-24 | Validity Periods | AB-ISSUE-LICENSE.ACB lines 102–115 |
| BR-25 | Vehicle Class | AB-ISSUE-LICENSE.ACB lines 46–50 |
| BR-26 | Starting Demerit 12 | AB-ISSUE-LICENSE.ACB line 135 |
| BR-27 | Candidate Active | AB-CREATE-APPLICATION.ACB lines 30–43 |
| BR-28 | Fee Amount Positive | Entity validation + fee lookup |
| BR-29 | Expiry After Issue | Entity validation + AB-ISSUE-LICENSE |
