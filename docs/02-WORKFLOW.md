# 02 — Process Flow & Workflow
## Driver License Issuance System (DLIS)

---

## 1. End-to-End Process Overview

The DLIS workflow is a strictly sequential, gate-controlled pipeline. Each stage must pass before the next can begin. A failure at any gate results in the application being **Rejected (RE)** and no further processing occurs.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                  DRIVER LICENSE ISSUANCE WORKFLOW                        │
│                                                                          │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐              │
│  │  STEP 1 │    │  STEP 2 │    │  STEP 3 │    │  STEP 4 │              │
│  │Register │───▶│ Submit  │───▶│Eligible │───▶│ History │              │
│  │Candidate│    │   App   │    │  Check  │    │  Check  │              │
│  └─────────┘    └─────────┘    └────┬────┘    └────┬────┘              │
│                                     │ FAIL         │ FAIL              │
│                                     ▼              ▼                   │
│                                 ┌───────┐      ┌───────┐              │
│                                 │REJECT │      │REJECT │              │
│                                 └───────┘      └───────┘              │
│                                                                         │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐             │
│  │  STEP 5 │    │  STEP 6 │    │  STEP 7 │    │  STEP 8 │             │
│  │ Payment │───▶│  Auth 1 │───▶│  Auth 2 │───▶│  Issue  │             │
│  │         │    │Approval │    │Approval │    │License  │             │
│  └─────────┘    └────┬────┘    └────┬────┘    └─────────┘             │
│                      │ REJECT       │ REJECT                          │
│                      ▼              ▼                                  │
│                  ┌───────┐      ┌───────┐                             │
│                  │REJECT │      │REJECT │                             │
│                  └───────┘      └───────┘                             │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Detailed Process Flow Diagram

```mermaid
flowchart TD
    START([Start]) --> REG[Register Candidate\nAB-CREATE-CANDIDATE]
    REG --> REGOK{Candidate\nCreated OK?}
    REGOK -- No --> REGERR([Error: Duplicate ID\nor Invalid Data])
    REGOK -- Yes --> APP[Create Application\nAB-CREATE-APPLICATION]

    APP --> LICTYPE{Select\nLicense Type}
    LICTYPE -- L=Learner --> ELIG
    LICTYPE -- P=Probation --> ELIG
    LICTYPE -- O=Open --> ELIG

    ELIG[Eligibility Check\nAB-CHECK-ELIGIBILITY] --> ELIGOK{Pass?}
    ELIGOK -- Fail --> RE1([REJECTED\nRE - Age or Upgrade Path])
    ELIGOK -- Pass --> HIST

    HIST[History Check\nAB-CHECK-HISTORY] --> HISTOK{Pass?}
    HISTOK -- Fail - Suspension --> RE2([REJECTED\nRE - Active Suspension])
    HISTOK -- Fail - Demerits --> RE3([REJECTED\nRE - Demerit Points GT 12])
    HISTOK -- Fail - Fines --> RE4([REJECTED\nRE - Unpaid Fines])
    HISTOK -- Pass --> PAY

    PAY[Process Payment\nAB-PROCESS-PAYMENT] --> PAYOK{Payment\nSuccessful?}
    PAYOK -- No --> RE5([ERROR - Payment Failed])
    PAYOK -- Yes --> AUTH1

    AUTH1[First Authority Approval\nAB-RECORD-APPROVAL-1] --> AUTH1OK{Level-1 Decision}
    AUTH1OK -- Reject --> RE6([REJECTED\nRE - Authority 1 Rejected])
    AUTH1OK -- Approve --> AUTH2

    AUTH2[Second Authority Approval\nAB-RECORD-APPROVAL-2] --> DIFFAUTH{Different\nAuthority?}
    DIFFAUTH -- No --> ERR7([ERROR - Same Authority\nNot Allowed])
    DIFFAUTH -- Yes --> AUTH2OK{Level-2 Decision}
    AUTH2OK -- Reject --> RE8([REJECTED\nRE - Authority 2 Rejected])
    AUTH2OK -- Approve --> ISSUE

    ISSUE[Issue License\nAB-ISSUE-LICENSE] --> ISSUED([LICENSE ISSUED\nStatus: IS])

    style ISSUED fill:#22c55e,color:#fff
    style RE1 fill:#ef4444,color:#fff
    style RE2 fill:#ef4444,color:#fff
    style RE3 fill:#ef4444,color:#fff
    style RE4 fill:#ef4444,color:#fff
    style RE5 fill:#f97316,color:#fff
    style RE6 fill:#ef4444,color:#fff
    style RE7 fill:#f97316,color:#fff
    style RE8 fill:#ef4444,color:#fff
    style ERR7 fill:#f97316,color:#fff
```

---

## 3. Application Status Lifecycle

Each application record tracks its position in the pipeline via the `APPLICATION-STATUS` field:

| Status Code | Description | Next Action |
|---|---|---|
| `PE` | Pending — application submitted | Run Eligibility Check |
| `EC` | Eligibility Checked and passed | Run History Check |
| `HC` | History Checked and passed | Process Payment |
| `PA` | Payment Approved | Submit First Approval |
| `A2` | Awaiting Second Authority Approval | Submit Second Approval |
| `AP` | Fully Approved — all gates passed | Issue License |
| `IS` | License Issued | Terminal state |
| `RE` | Rejected | Terminal state |

> **Note:** Status `A1` (Awaiting First Approval) is a display alias — the transition from `PA` to `A2` is recorded atomically when Auth 1 approves.

---

## 4. Eligibility Check Logic

```mermaid
flowchart TD
    E1[Read Application + Candidate] --> E2[Calculate Age\nCurrentDate - DateOfBirth]
    E2 --> E3{License Type?}

    E3 -- L=Learner --> E4{Age >= 16?}
    E4 -- No --> EFAIL
    E4 -- Yes --> EPASS

    E3 -- P=Probation --> E5{Age >= 17?}
    E5 -- No --> EFAIL
    E5 -- Yes --> E6{Active Learner\nLicense Exists?}
    E6 -- No --> EFAIL
    E6 -- Yes --> EPASS

    E3 -- O=Open --> E7{Age >= 18?}
    E7 -- No --> EFAIL
    E7 -- Yes --> E8{Active Probation\nLicense Exists?}
    E8 -- No --> EFAIL
    E8 -- Yes --> EPASS

    EPASS[Record PASS\nUpdate Status to EC] --> DONE([Done])
    EFAIL[Record FAIL + Reason\nUpdate Status to RE] --> DONE
```

---

## 5. History Check Logic

```mermaid
flowchart TD
    H1[Read All Active\nHistory Records] --> H2[For Each Record...]
    H2 --> H3{Incident Type\nSU or DQ?}
    H3 -- Yes --> H4{Suspension End\nDate in Future?}
    H4 -- Yes --> HFAIL1[FAIL: Active Suspension]
    H3 -- No --> H5[Accumulate\nDemerit Points]
    H5 --> H6{Fine Amount > 0\nAND Unpaid?}
    H6 -- Yes --> HFAIL2[FAIL: Unpaid Fine]
    H6 -- No --> H2

    H2 --> H7{Total Demerits\n> 12?}
    H7 -- Yes --> HFAIL3[FAIL: Demerit Threshold]
    H7 -- No --> HPASS[PASS: Record Result\nUpdate Status to HC]

    HFAIL1 & HFAIL2 & HFAIL3 --> HFAILREC[Record FAIL + Reason\nUpdate Status to RE]
```

---

## 6. Dual Approval Segregation

```mermaid
sequenceDiagram
    participant Clerk
    participant System
    participant Auth1 as First Authority Officer
    participant Auth2 as Second Authority Officer

    Clerk->>System: Submit Application (Payment Complete)
    System-->>Auth1: Application Available for Level-1 Review

    Auth1->>System: Submit Decision (Approve/Reject)\nwith Authority Code
    System->>System: Validate: Level-1 authorised for license type
    System-->>Auth1: Decision Recorded

    alt First Approval Approved
        System-->>Auth2: Application Available for Level-2 Review
        Auth2->>System: Submit Decision (Approve/Reject)\nwith Authority Code
        System->>System: Validate: Level-2 authorised for license type
        System->>System: Validate: Auth2 ≠ Auth1 authority
        System-->>Auth2: Decision Recorded

        alt Second Approval Approved
            System-->>Clerk: Application Fully Approved (AP)
            Clerk->>System: Issue License
            System-->>Clerk: License Number + Expiry Date
        else Second Approval Rejected
            System-->>Clerk: Application Rejected (RE)
        end
    else First Approval Rejected
        System-->>Clerk: Application Rejected (RE)
    end
```

---

## 7. Payment Process Flow

```mermaid
flowchart LR
    P1[Officer Opens\nPayment Screen] --> P2[Enter Application ID]
    P2 --> P3[System Looks Up\nFee Schedule]
    P3 --> P4[Display Fee Amount\nand Currency]
    P4 --> P5[Officer Enters:\nMethod + Reference]
    P5 --> P6[AB-PROCESS-PAYMENT]
    P6 --> P7[Create PAYMENT Record\nGenerate Receipt Number]
    P7 --> P8[Update Application:\nPAYMENT-STATUS=P\nAPP-STATUS=PA]
    P8 --> P9[Print/Display Receipt]
```

---

## 8. License Issuance — Expiry Calculation

| License Type | Validity Period | Example: Issued 01/01/2025 |
|---|---|---|
| L — Learner | **1 year** | Expires 01/01/2026 |
| P — Probation | **2 years** | Expires 01/01/2027 |
| O — Open | **5 years** | Expires 01/01/2030 |

License number format: `{TYPE-PREFIX}{VEHICLE-CLASS}{CANDIDATE-ID}{APPLICATION-ID}`

| Prefix | License Type |
|---|---|
| `LRN` | Learner |
| `PRB` | Probation |
| `OPN` | Open |

Vehicle Classes: `A`=Motorcycle, `B`=Light Vehicle, `C`=Heavy Vehicle, `D`=Bus/Coach
