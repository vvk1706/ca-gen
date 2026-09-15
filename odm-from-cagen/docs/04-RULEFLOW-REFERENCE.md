# 04 — Ruleflow Reference
## All Ruleflows — Nodes, Conditions, CA Gen Mapping

---

## DLIS-Main-Workflow

**File:** [`ruleflows/DLIS-Main-Workflow.rflo`](../ruleflows/DLIS-Main-Workflow.rflo)
**CA Gen origin:** Full DLIS pipeline (`docs/02-WORKFLOW.md`)

Top-level orchestrator. Calls each sub-ruleflow in sequence. A gate failure halts the flow.

```
Start
  │
  ▼
EligibilityCheck (sub-flow) ──FAIL──► ApplicationRejected [RE]
  │ PASS
  ▼
HistoryCheck (sub-flow) ──FAIL──► ApplicationRejected [RE]
  │ PASS
  ▼
PaymentProcessing (sub-flow) ──ERROR──► PaymentError
  │ PASS
  ▼
FirstApproval (sub-flow) ──REJECT──► ApplicationRejected [RE]
  │ APPROVE
  ▼
SecondApproval (sub-flow) ──REJECT──► ApplicationRejected [RE]
  │ APPROVE
  ▼
LicenseIssuance (sub-flow)
  │
  ▼
LicenseIssued ✓
```

---

## DLIS-Eligibility-Check

**File:** [`ruleflows/DLIS-Eligibility-Check.rflo`](../ruleflows/DLIS-Eligibility-Check.rflo)
**CA Gen origin:** `AB-CHECK-ELIGIBILITY.ACB`

| Node | Type | Ruleset | CA Gen Procedure |
|---|---|---|---|
| `ComputeCandidateAge` | Rule Task | `eligibility/compute-candidate-age` | Inline calculation line 60 |
| `ValidateLicenseType` | Rule Task | `eligibility/validate-license-type` | Line 63 `IF LICENSE-TYPE` |
| `CheckMinimumAge` | Rule Task | `eligibility/check-minimum-age` | Lines 63–87 |
| `CheckUpgradePath` | Rule Task | `eligibility/check-upgrade-path` | Lines 89–108 |
| `EligibilityResultGateway` | Decision | `context.hasViolations()` | Line 112 `IF WA-FAIL-FLAG` |
| `RecordEligibilityPass` | Rule Task | `eligibility/record-eligibility-pass` | Lines 123–130 |
| `RecordEligibilityFail` | Rule Task | `eligibility/record-eligibility-fail` | Lines 112–117 |

**Status transitions:**
- Pass → `ELIGIBILITY-CHECK-STATUS = "P"`, `APPLICATION-STATUS = "EC"`
- Fail → `ELIGIBILITY-CHECK-STATUS = "F"`, `APPLICATION-STATUS = "RE"`

---

## DLIS-History-Check

**File:** [`ruleflows/DLIS-History-Check.rflo`](../ruleflows/DLIS-History-Check.rflo)
**CA Gen origin:** `AB-CHECK-HISTORY.ACB`

| Node | Type | Ruleset | CA Gen Procedure |
|---|---|---|---|
| `CheckEligibilityGate` | Rule Task | `history/check-eligibility-gate` | Lines 53–58 |
| `EligibilityGateDecision` | Decision | `context.returnCode == 2` | Lines 53–58 |
| `CheckActiveSuspension` | Rule Task | `history/check-active-suspension` | Lines 65–73 |
| `AccumulateDemeritPoints` | Rule Task | `history/accumulate-demerit-points` | Line 76 |
| `CheckUnpaidFines` | Rule Task | `history/check-unpaid-fines` | Lines 78–86 |
| `HistoryResultGateway` | Decision | `context.hasViolations()` | Lines 96–97 |
| `RecordHistoryPass` | Rule Task | `history/record-history-pass` | Lines 108–113 |
| `RecordHistoryFail` | Rule Task | `history/record-history-fail` | Lines 104–116 |

**Status transitions:**
- Pass → `HISTORY-CHECK-STATUS = "P"`, `APPLICATION-STATUS = "HC"`
- Fail → `HISTORY-CHECK-STATUS = "F"`, `APPLICATION-STATUS = "RE"`

---

## DLIS-Payment-Processing

**File:** [`ruleflows/DLIS-Payment-Processing.rflo`](../ruleflows/DLIS-Payment-Processing.rflo)
**CA Gen origin:** `AB-PROCESS-PAYMENT.ACB`

| Node | Type | CA Gen Procedure |
|---|---|---|
| `CheckHistoryGate` | Rule Task | Lines 49–54 |
| `CheckDuplicatePayment` | Rule Task | Lines 56–60 |
| `ValidateFeeSchedule` | Rule Task | Lines 63–74 |
| `GenerateReceiptAndApprove` | Rule Task | Lines 78–109 |

**Transactional note:** After `GenerateReceiptAndApprove` fires successfully, the Java service layer calls `CICS LINK DLISPAY` to INSERT the PAYMENT record and UPDATE the application status in DB2.

**Status transitions:**
- Pass → `PAYMENT-STATUS = "P"`, `APPLICATION-STATUS = "PA"`

---

## DLIS-Approval-Level1

**File:** [`ruleflows/DLIS-Approval-Level1.rflo`](../ruleflows/DLIS-Approval-Level1.rflo)
**CA Gen origin:** `AB-RECORD-APPROVAL-1.ACB`

| Node | Type | Rules Fired | CA Gen Lines |
|---|---|---|---|
| `ValidateDecisionValue` | Rule Task | APR-001 | 35–40 |
| `CheckPaymentGate` | Rule Task | BR-15 | 51–56 |
| `ValidateAuthorityUser` | Rule Task | BR-18, BR-19, BR-20 | 58–74 |
| `RecordApproval1Decision` | Rule Task | APR-REC-001/002 | 76–90 |

**Status transitions (Approve):** `APPROVAL-1-STATUS = "A"`, `APPLICATION-STATUS = "A2"`
**Status transitions (Reject):** `APPROVAL-1-STATUS = "R"`, `APPLICATION-STATUS = "RE"`

---

## DLIS-Approval-Level2

**File:** [`ruleflows/DLIS-Approval-Level2.rflo`](../ruleflows/DLIS-Approval-Level2.rflo)
**CA Gen origin:** `AB-RECORD-APPROVAL-2.ACB`

| Node | Type | Rules Fired | CA Gen Lines |
|---|---|---|---|
| `ValidateDecisionValue` | Rule Task | APR-001 | 35–40 |
| `CheckApproval1Gate` | Rule Task | BR-16 | 51–56 |
| `ValidateAuthorityUser2` | Rule Task | BR-18, BR-19b | 58–68 |
| `CheckDifferentAuthority` | Rule Task | BR-20, BR-21 | 69–82 |
| `RecordApproval2Decision` | Rule Task | APR-REC-003/004 | 84–99 |

**Key rule — BR-21 Segregation of Duties:** The second approver's `authorityName` must differ from `application.approval1Authority`. This enforces true dual-control.

**Status transitions (Approve):** `APPROVAL-2-STATUS = "A"`, `APPLICATION-STATUS = "AP"`
**Status transitions (Reject):** `APPROVAL-2-STATUS = "R"`, `APPLICATION-STATUS = "RE"`

---

## DLIS-License-Issuance

**File:** [`ruleflows/DLIS-License-Issuance.rflo`](../ruleflows/DLIS-License-Issuance.rflo)
**CA Gen origin:** `AB-ISSUE-LICENSE.ACB`

| Node | Type | Rules Fired | CA Gen Lines |
|---|---|---|---|
| `ValidateVehicleClass` | Rule Task | BR-25 | 46–50 |
| `RecheckAllGates` | Rule Task | BR-17a–f | 62–97 |
| `CalculateExpiryDate` | Rule Task | BR-24 (3 variants) | 102–115 |
| `GenerateLicenseNumber` | Rule Task | ISS-001 (3 variants) | 117–122 |
| `SetDemeritBalance` | Rule Task | BR-26 | 135 |

**Transactional note:** After all rules pass, the Java service layer calls `CICS LINK DLISLICS` to INSERT the ISSUED_LICENSE record and UPDATE application status to `IS`.

**License number format:** `{PREFIX}{VEHICLE-CLASS}{CANDIDATE-ID}{APPLICATION-ID}`

| License Type | Prefix |
|---|---|
| L — Learner | `LRN` |
| P — Probation | `PRB` |
| O — Open | `OPN` |
