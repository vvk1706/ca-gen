# DLIS — Business Rules

All rules extracted directly from COBOL PROCEDURE DIVISION logic.
Source programs: DLISCAND, DLISAPPL, DLISELIG, DLISHIST, DLISPAY, DLISAP1, DLISAP2, DLISISSU.

---

## BR-CAND-01 — Candidate Mandatory Fields
**Source:** `DLISCAND` / `CREATE-CANDIDATE`  
First name, last name, and national ID number are all mandatory for candidate creation.

---

## BR-CAND-02 — Unique National ID
**Source:** `DLISCAND` / `CREATE-CANDIDATE`  
A candidate with the same `ID_NUMBER` and `RECORD_STATUS = 'A'` may not be created twice.  
Check: `SELECT CANDIDATE_ID FROM DLIS.CANDIDATE WHERE ID_NUMBER = :id AND RECORD_STATUS = 'A'`  
→ If SQLCODE = 0 (row found), creation is rejected.

---

## BR-CAND-03 — Inactive Candidate Cannot Apply
**Source:** `DLISAPPL` / `LOOKUP-CANDIDATE`  
A candidate with `RECORD_STATUS = 'I'` cannot have a new licence application submitted against them.

---

## BR-APPL-01 — Valid Licence Types
**Source:** `DLISAPPL` / `SUBMIT-APPLICATION`  
Licence type must be one of: `L` (Learner), `P` (Probation), `O` (Open). Any other value is rejected.  
DDL constraint: `CHECK (LICENSE_TYPE IN ('L','P','O'))`

---

## BR-APPL-02 — No Duplicate Active Application
**Source:** `DLISAPPL` / `SUBMIT-APPLICATION`  
Only one active application per candidate per licence type is permitted.  
Active means `APPLICATION_STATUS NOT IN ('RE','IS')`.  
If any such application already exists, the new submission is rejected.

---

## BR-APPL-03 — Application Initial Status
**Source:** `DLISAPPL` / `SUBMIT-APPLICATION`  
All newly submitted applications are created with `APPLICATION_STATUS = 'PE'` (Pending).

---

## BR-ELIG-01 — Minimum Age: Learner
**Source:** `DLISELIG` / `RUN-ELIGIBILITY-CHECK`  
Candidate must be at least **16 years old** (year-level calculation: `CURRENT YEAR - BIRTH YEAR`) to apply for a Learner licence.  
Failure message: `AGE BELOW 16 - LEARNER LICENSE REQUIRES MIN AGE 16`

---

## BR-ELIG-02 — Minimum Age: Probation
**Source:** `DLISELIG` / `RUN-ELIGIBILITY-CHECK`  
Candidate must be at least **17 years old** for a Probation licence.  
Failure message: `AGE BELOW 17 - PROBATION LICENSE REQUIRES MIN AGE 17`

---

## BR-ELIG-03 — Minimum Age: Open
**Source:** `DLISELIG` / `RUN-ELIGIBILITY-CHECK`  
Candidate must be at least **18 years old** for an Open licence.  
Failure message: `AGE BELOW 18 - OPEN LICENSE REQUIRES MIN AGE 18`

---

## BR-ELIG-04 — Upgrade Path: Probation Requires Active Learner
**Source:** `DLISELIG` / `RUN-ELIGIBILITY-CHECK`  
To be eligible for a Probation licence, the candidate must hold at least one issued licence with `LICENSE_TYPE = 'L'` and `LICENSE_STATUS = 'A'` in `DLIS.ISSUED_LICENSE`.  
Failure message: `MUST HOLD ACTIVE LEARNER LICENSE FOR PROBATION`

---

## BR-ELIG-05 — Upgrade Path: Open Requires Active Probation
**Source:** `DLISELIG` / `RUN-ELIGIBILITY-CHECK`  
To be eligible for an Open licence, the candidate must hold at least one issued licence with `LICENSE_TYPE = 'P'` and `LICENSE_STATUS = 'A'` in `DLIS.ISSUED_LICENSE`.  
Failure message: `MUST HOLD ACTIVE PROBATION LICENSE FOR OPEN`

---

## BR-ELIG-06 — Eligibility Check Sequence
**Source:** `DLISELIG` / `RUN-ELIGIBILITY-CHECK`  
The eligibility check operates on the application data. Age calculation uses year-level precision only (current year minus birth year); month/day are not considered.

---

## BR-HIST-01 — History Check Requires Eligibility Pass
**Source:** `DLISHIST` / `RUN-HISTORY-CHECK`  
The driving history check may only be run after the eligibility check has passed (`ELIG_CHECK_STATUS = 'P'`).  
Message: `ELIGIBILITY CHECK MUST BE PASSED FIRST`

---

## BR-HIST-02 — Active Suspension Bars Application
**Source:** `DLISHIST` / `LOAD-HISTORY-TABLE`  
Any driving history record with `INCIDENT_TYPE IN ('SU','DQ')` and `SUSP_END_DATE >= CURRENT DATE` (or `SUSP_END_DATE` is null/spaces, meaning open-ended) constitutes an active suspension or disqualification.  
→ History check **FAIL**.  
Message: `ACTIVE SUSPENSION OR DISQUALIFICATION ON FILE`

---

## BR-HIST-03 — Demerit Point Threshold
**Source:** `DLISHIST` / `RUN-HISTORY-CHECK`  
The sum of all `DEMERIT_POINTS` across active history records (`RECORD_STATUS = 'A'`) must not exceed **12**.  
→ History check **FAIL**.  
Message: `TOTAL DEMERIT POINTS EXCEED THRESHOLD OF 12`

---

## BR-HIST-04 — Unpaid Fines Bar Application
**Source:** `DLISHIST` / `LOAD-HISTORY-TABLE`  
Any active history record with `FINE_AMOUNT > 0` and `FINE_PAID_STATUS = 'N'` represents an outstanding unpaid fine.  
→ History check **FAIL**.  
Message: `OUTSTANDING UNPAID FINES ON FILE`

---

## BR-HIST-05 — History Failure Order
**Source:** `DLISHIST` / `RUN-HISTORY-CHECK`  
The three history failure checks are evaluated in priority order:
1. Active suspension/disqualification (checked first)
2. Demerit point threshold
3. Unpaid fines  
Only the first failing condition is recorded as the failure reason.

---

## BR-PAY-01 — Payment Requires History Pass
**Source:** `DLISPAY` / `PROCESS-PAYMENT`  
Payment may only be processed after the history check has passed (`HIST_CHECK_STATUS = 'P'`).  
Message: `HISTORY CHECK MUST BE PASSED BEFORE PAYMENT`

---

## BR-PAY-02 — No Duplicate Payment
**Source:** `DLISPAY` / `PROCESS-PAYMENT`  
If `PAYMENT_STATUS = 'P'` already exists on the application, a second payment attempt is rejected.  
Message: `PAYMENT ALREADY RECORDED FOR THIS APPLICATION`

---

## BR-PAY-03 — Valid Payment Methods
**Source:** `DLISPAY` / `PROCESS-PAYMENT`  
Accepted payment methods: `CC` (Credit Card), `DC` (Debit Card), `EF` (EFT/Bank Transfer), `CS` (Cash), `CH` (Cheque).  
DDL constraint: `CHECK (PAYMENT_METHOD IN ('CC','DC','EF','CS','CH'))`

---

## BR-PAY-04 — Receipt Number Format
**Source:** `DLISPAY` / `PROCESS-PAYMENT`  
Receipt number is system-generated as: `'RCP'` + `YYYYMMDD` (current date, 8 chars) + `applicationId`.

---

## BR-PAY-05 — Fee Amount Source
**Source:** `DLISPAY` / `LOAD-APPLICATION`  
The payment amount is always taken from the current active fee schedule (`DLIS.LICENSE_FEE_SCHEDULE`) at the time of processing, not entered manually.

---

## BR-APP1-01 — Payment Must Be Complete Before First Approval
**Source:** `DLISAP1` / `SUBMIT-APPROVAL1`  
The first authority approval may only be submitted after payment has been recorded (`PAYMENT_STATUS = 'P'`).  
Message: `PAYMENT MUST BE COMPLETED BEFORE FIRST APPROVAL`

---

## BR-APP1-02 — First Approver Must Be Level 1
**Source:** `DLISAP1` / `VALIDATE-AUTHORITY`  
The authority user must exist in `DLIS.AUTHORITY_USER` with `ACTIVE_STATUS = 'A'` and `AUTHORITY_LEVEL = '1'`.

---

## BR-APP1-03 — First Approver Licence Type Authorisation
**Source:** `DLISAP1` / `SUBMIT-APPROVAL1`  
The first approver's `LIC_TYPES_AUTH` field must contain the character matching the application's `LICENSE_TYPE`.  
E.g. if `LIC_TYPES_AUTH = 'LP'`, the user may approve Learner and Probation applications but not Open (`O`).  
Message: `AUTHORITY NOT AUTHORISED FOR THIS LICENSE TYPE`

---

## BR-APP2-01 — Second Approval Requires First Approval
**Source:** `DLISAP2` / `SUBMIT-APPROVAL2`  
The second authority approval may only be submitted after the first approval has been granted (`APPROVAL_1_STATUS = 'A'`).  
Message: `FIRST APPROVAL MUST BE GRANTED BEFORE SECOND`

---

## BR-APP2-02 — Second Approver Must Be Level 2
**Source:** `DLISAP2` / `VALIDATE-AUTHORITY`  
The authority user must exist in `DLIS.AUTHORITY_USER` with `ACTIVE_STATUS = 'A'` and `AUTHORITY_LEVEL = '2'`.

---

## BR-APP2-03 — Segregation of Duties: Different Authorities Required
**Source:** `DLISAP2` / `SUBMIT-APPROVAL2`  
The `AUTHORITY_NAME` of the second approver must differ from the `APPROVAL_1_AUTHORITY` stored on the application.  
The same authority organisation cannot provide both the first and second approval.  
Message: `SECOND APPROVER MUST BE FROM DIFFERENT AUTHORITY`

---

## BR-APP2-04 — Second Approver Licence Type Authorisation
**Source:** `DLISAP2` / `SUBMIT-APPROVAL2`  
Same restriction as BR-APP1-03 — the second approver's `LIC_TYPES_AUTH` must also cover the application's `LICENSE_TYPE`.

---

## BR-ISSU-01 — Application Must Be Fully Approved Before Issue
**Source:** `DLISISSU` / `ISSUE-LICENSE`  
A licence may only be issued when `APPLICATION_STATUS = 'AP'`. This is the terminal gate that confirms all five checks (eligibility, history, payment, approval 1, approval 2) are satisfied.  
Message: `APPLICATION STATUS MUST BE AP BEFORE ISSUE`

---

## BR-ISSU-02 — Mandatory Issue Fields
**Source:** `DLISISSU` / `ISSUE-LICENSE`  
Vehicle class (`A`, `B`, `C`, or `D`), issuing officer, and issuing authority are all mandatory at issuance.

---

## BR-ISSU-03 — Expiry Date Calculation
**Source:** `DLISISSU` / `ISSUE-LICENSE`  
Expiry date is calculated from the issue date (current date):

| Licence Type | Validity Period |
|-------------|----------------|
| L (Learner) | + 1 year        |
| P (Probation)| + 2 years      |
| O (Open)    | + 5 years       |

---

## BR-ISSU-04 — Licence Number Generation
**Source:** `DLISISSU` / `ISSUE-LICENSE`  
Licence number is system-generated by concatenating:
`{licPrefix}` + `{vehicleClass}` + `{candidateId}` + `{applicationId}`

Where `licPrefix` is:
- `LRN` for Learner
- `PRB` for Probation
- `OPN` for Open

---

## BR-ISSU-05 — Initial Demerit Balance
**Source:** `DLISISSU` / `ISSUE-LICENSE`  
All newly issued licences are created with a `DEMERIT_BALANCE` of **12**.  
DDL: `DEMERIT_BALANCE DECIMAL(3,0) WITH DEFAULT 12`

---

## BR-ISSU-06 — Application Closed After Issue
**Source:** `DLISISSU` / `ISSUE-LICENSE`  
After successful licence issuance, `APPLICATION_STATUS` is updated to `'IS'`. No further processing is possible on that application.

---

## Summary Table

| Rule ID      | Category    | Type        | Description (short)                              |
|-------------|------------|------------|--------------------------------------------------|
| BR-CAND-01  | Candidate   | Validation  | Mandatory fields for candidate creation          |
| BR-CAND-02  | Candidate   | Uniqueness  | National ID must be unique across active records |
| BR-CAND-03  | Candidate   | Gate        | Inactive candidate cannot apply                 |
| BR-APPL-01  | Application | Validation  | Licence type must be L, P, or O                 |
| BR-APPL-02  | Application | Uniqueness  | No duplicate active application per type        |
| BR-APPL-03  | Application | Default     | New applications start as PE                    |
| BR-ELIG-01  | Eligibility | Age         | Learner min age 16                              |
| BR-ELIG-02  | Eligibility | Age         | Probation min age 17                            |
| BR-ELIG-03  | Eligibility | Age         | Open min age 18                                 |
| BR-ELIG-04  | Eligibility | Upgrade     | Probation requires active Learner licence        |
| BR-ELIG-05  | Eligibility | Upgrade     | Open requires active Probation licence           |
| BR-ELIG-06  | Eligibility | Calculation | Age calculated at year level only               |
| BR-HIST-01  | History     | Sequence    | History check requires eligibility pass         |
| BR-HIST-02  | History     | Disqualify  | Active suspension/disqualification bars app      |
| BR-HIST-03  | History     | Threshold   | Demerit total > 12 bars application             |
| BR-HIST-04  | History     | Compliance  | Unpaid fines bar application                    |
| BR-HIST-05  | History     | Priority    | Failure reasons evaluated in defined order      |
| BR-PAY-01   | Payment     | Sequence    | Payment requires history pass                   |
| BR-PAY-02   | Payment     | Duplicate   | Only one payment per application                |
| BR-PAY-03   | Payment     | Validation  | Valid payment method codes only                 |
| BR-PAY-04   | Payment     | Format      | Receipt number format: RCP + YYYYMMDD + appId   |
| BR-PAY-05   | Payment     | Amount      | Fee from schedule, not manual entry             |
| BR-APP1-01  | Approval    | Sequence    | Payment must precede first approval             |
| BR-APP1-02  | Approval    | Authority   | Level 1 authority required for first approval   |
| BR-APP1-03  | Approval    | Authorisation| Approver must cover licence type               |
| BR-APP2-01  | Approval    | Sequence    | First approval must precede second              |
| BR-APP2-02  | Approval    | Authority   | Level 2 authority required for second approval  |
| BR-APP2-03  | Approval    | Segregation | Second approver from different authority        |
| BR-APP2-04  | Approval    | Authorisation| Second approver must cover licence type        |
| BR-ISSU-01  | Issuance    | Gate        | Application must be AP before issue             |
| BR-ISSU-02  | Issuance    | Validation  | Vehicle class, officer, authority mandatory     |
| BR-ISSU-03  | Issuance    | Calculation | Expiry = issue date + type-specific years       |
| BR-ISSU-04  | Issuance    | Format      | Licence number format: prefix+class+cand+app    |
| BR-ISSU-05  | Issuance    | Default     | Demerit balance initialised to 12               |
| BR-ISSU-06  | Issuance    | Terminal    | Application closed (IS) after issue             |
