# DLIS — Application Lifecycle and Workflow

## End-to-End Process

A licence application passes through a mandatory sequence of five gates before a licence can be issued. Each gate must be completed in order. A failure at any gate terminates the application with status `RE` (Rejected).

```
Candidate Registration
        │
        ▼
  Submit Application
  (DLISAPPL / DL03)
  STATUS: PE
        │
        ▼
  Eligibility Check ──── FAIL ──────────────────────┐
  (DLISELIG / DL05)                                 │
  STATUS: EC                                        │
        │                                           │
        ▼                                           │
  Driving History Check ── FAIL ────────────────────┤
  (DLISHIST / DL06)                                 │
  STATUS: HC                                        │
        │                                           │
        ▼                                           │
  Payment Processing ────── FAIL (not yet paid) ────┤
  (DLISPAY / DL07)          [blocks approval]       │
  STATUS: PA                                        │
        │                                           │
        ▼                                           │
  1st Authority Approval ── REJECT ─────────────────┤
  (DLISAP1 / DL08)                                  │
  STATUS: A2                                        │
        │                                           │
        ▼                                           │
  2nd Authority Approval ── REJECT ─────────────────┤
  (DLISAP2 / DL09)                                  │
  STATUS: AP                                        │
        │                                           │
        ▼                                           ▼
  Licence Issuance                            STATUS: RE
  (DLISISSU / DL10)                           (Rejected)
  STATUS: IS
```

---

## Status Transition Table

| From Status | Action                        | Program   | Pass → New Status | Fail → Status |
|------------|-------------------------------|----------|------------------|--------------|
| *(none)*    | Submit Application            | DLISAPPL | `PE`             | —            |
| `PE`        | Run Eligibility Check         | DLISELIG | `EC`             | `RE`         |
| `EC`        | Run History Check             | DLISHIST | `HC`             | `RE`         |
| `HC`        | Process Payment               | DLISPAY  | `PA`             | —            |
| `PA`        | Submit 1st Approval (Approve) | DLISAP1  | `A2`             | `RE`         |
| `A2`        | Submit 2nd Approval (Approve) | DLISAP2  | `AP`             | `RE`         |
| `AP`        | Issue Licence                 | DLISISSU | `IS`             | —            |

---

## Gate Prerequisites (Sequential Dependencies)

| Gate              | Prerequisite field/value                      | Enforced in  |
|------------------|----------------------------------------------|-------------|
| History Check     | `ELIG_CHECK_STATUS = 'P'`                    | DLISHIST    |
| Payment           | `HIST_CHECK_STATUS = 'P'`                    | DLISPAY     |
| 1st Approval      | `PAYMENT_STATUS = 'P'`                       | DLISAP1     |
| 2nd Approval      | `APPROVAL_1_STATUS = 'A'`                    | DLISAP2     |
| 2nd Approval      | Second approver's authority ≠ first approver | DLISAP2     |
| Licence Issuance  | `APPLICATION_STATUS = 'AP'`                  | DLISISSU    |

---

## Sequence Diagram — Happy Path

```
Clerk        DLISCAND     DLISAPPL     DLISELIG     DLISHIST     DLISPAY      DLISAP1      DLISAP2      DLISISSU
  │               │            │            │            │            │            │            │            │
  │─ createCand ─►│            │            │            │            │            │            │            │
  │◄─ candidateId ┤            │            │            │            │            │            │            │
  │               │            │            │            │            │            │            │            │
  │──── submitApplication ────►│            │            │            │            │            │            │
  │◄────────── applicationId ──┤            │            │            │            │            │            │
  │               │            │  STATUS=PE │            │            │            │            │            │
  │               │            │            │            │            │            │            │            │
  │──────────── runEligibilityCheck ───────►│            │            │            │            │            │
  │◄──────────────────────── PASS ──────────┤            │            │            │            │            │
  │               │            │  STATUS=EC │            │            │            │            │            │
  │               │            │            │            │            │            │            │            │
  │────────────────── runHistoryCheck ──────────────────►│            │            │            │            │
  │◄────────────────────────────── PASS ────────────────┤            │            │            │            │
  │               │            │  STATUS=HC │            │            │            │            │            │
  │               │            │            │            │            │            │            │            │
  │─────────────────────── processPayment ──────────────────────────►│            │            │            │
  │◄──────────────────────────────── receiptNumber ─────────────────┤            │            │            │
  │               │            │  STATUS=PA │            │            │            │            │            │
  │               │            │            │            │            │            │            │            │
  │────────────────────────── submitApproval1 ──────────────────────────────────►│            │            │
  │◄─────────────────────────────────────── APPROVED ───────────────────────────┤            │            │
  │               │            │  STATUS=A2 │            │            │            │            │            │
  │               │            │            │            │            │            │            │            │
  │──────────────────────────────── submitApproval2 ────────────────────────────────────────►│            │
  │◄───────────────────────────────────────────── APPROVED ─────────────────────────────────┤            │
  │               │            │  STATUS=AP │            │            │            │            │            │
  │               │            │            │            │            │            │            │            │
  │──────────────────────────────────── issueLicence ───────────────────────────────────────────────────►│
  │◄──────────────────────────────────────────────── licenceNumber + expiryDate ───────────────────────────┤
  │               │            │  STATUS=IS │            │            │            │            │            │
```

---

## Licence Type Upgrade Path

The system enforces a strict licensing ladder:

```
Learner (L) ──► Probation (P) ──► Open (O)
```

- To apply for **Probation**, the candidate must hold an **Active** Learner licence (`DLIS.ISSUED_LICENSE WHERE LICENSE_TYPE='L' AND LICENSE_STATUS='A'`).
- To apply for **Open**, the candidate must hold an **Active** Probation licence (`DLIS.ISSUED_LICENSE WHERE LICENSE_TYPE='P' AND LICENSE_STATUS='A'`).
- Learner may be applied for with no prior licence — only the minimum age (16) applies.

---

## Dual-Authority Approval — Segregation of Duties

The approval workflow enforces segregation at two levels:

1. **Level separation:** First approval requires `AUTHORITY_LEVEL = '1'`; second requires `AUTHORITY_LEVEL = '2'`. These are distinct user populations in `DLIS.AUTHORITY_USER`.

2. **Organisational separation:** The second approver's `AUTHORITY_NAME` (organisation) must differ from `APPROVAL_1_AUTHORITY` stored on the application. One organisation cannot hold both approvals.

3. **Type restriction:** Each authority user has a `LIC_TYPES_AUTH` field (up to 3 chars, e.g. `LPO`). The application's `LICENSE_TYPE` character must appear in this string — a user not authorised for that licence type is rejected.

---

## Key Business Events and Database Changes

| Event                   | Tables Written                          | Status Transitions              |
|------------------------|----------------------------------------|---------------------------------|
| Candidate created       | CANDIDATE (INSERT)                     | —                               |
| Application submitted   | LICENSE_APPLICATION (INSERT)           | → PE                            |
| Eligibility passed      | LICENSE_APPLICATION (UPDATE)           | → EC                            |
| Eligibility failed      | LICENSE_APPLICATION (UPDATE)           | → RE                            |
| History passed          | LICENSE_APPLICATION (UPDATE)           | → HC                            |
| History failed          | LICENSE_APPLICATION (UPDATE)           | → RE                            |
| Payment recorded        | PAYMENT (INSERT), LICENSE_APPLICATION  | → PA                            |
| 1st approval granted    | LICENSE_APPLICATION (UPDATE)           | → A2                            |
| 1st approval rejected   | LICENSE_APPLICATION (UPDATE)           | → RE                            |
| 2nd approval granted    | LICENSE_APPLICATION (UPDATE)           | → AP                            |
| 2nd approval rejected   | LICENSE_APPLICATION (UPDATE)           | → RE                            |
| Licence issued          | ISSUED_LICENSE (INSERT), LICENSE_APPLICATION | → IS                       |
