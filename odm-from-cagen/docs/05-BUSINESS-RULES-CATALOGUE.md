# 05 — Business Rules Catalogue
## All 29 Rules — BAL Source, CA Gen Origin, ODM Location

---

## Rule Packages Summary

| Package | File | Rules |
|---|---|---|
| `candidate` | [`rules/candidate/candidate-rules.brl`](../rules/candidate/candidate-rules.brl) | BR-07, BR-08, BR-09, BR-27 |
| `eligibility` | [`rules/eligibility/eligibility-rules.brl`](../rules/eligibility/eligibility-rules.brl) | BR-01 to BR-06 |
| `history` | [`rules/history/history-rules.brl`](../rules/history/history-rules.brl) | BR-10 to BR-13 |
| `payment` | [`rules/payment/payment-rules.brl`](../rules/payment/payment-rules.brl) | BR-14, BR-22, BR-23, BR-28 |
| `approval` | [`rules/approval/approval-rules.brl`](../rules/approval/approval-rules.brl) | BR-15, BR-16, BR-18 to BR-21 |
| `issuance` | [`rules/issuance/issuance-rules.brl`](../rules/issuance/issuance-rules.brl) | BR-17, BR-24 to BR-26, BR-29 |

---

## Candidate Rules

### BR-01 — License Type Must Be L, P, or O
| | |
|---|---|
| **Package** | `eligibility` |
| **CA Gen source** | `AB-CREATE-APPLICATION.ACB` lines 46–49 |
| **Original code** | `IF WS-LICENSE-TYPE NOT IN ('L', 'P', 'O')` |
| **BAL rule** | `"BR-01 License Type Must Be L P Or O"` |
| **When** | `the licenseType of the application of context is not "L" and not "P" and not "O"` |
| **Then** | Add violation: `"INVALID LICENSE TYPE. MUST BE L, P OR O"` |
| **Return code** | 6 |

---

### BR-07 — Unique National ID
| | |
|---|---|
| **Package** | `candidate` |
| **CA Gen source** | `AB-CREATE-CANDIDATE.ACB` lines 38–46 |
| **Original code** | `READ EACH LV-CANDIDATE-DUP WHERE ID-NUMBER = WS-ID-NUMBER AND RECORD-STATUS = 'A'` |
| **BAL rule** | `"BR-07 Candidate ID Number Must Be Unique"` |
| **Note** | Service layer pre-queries DB2 and sets `returnCode=99` if duplicate found |
| **Return code** | 1 |

---

### BR-08 — Date of Birth Must Be in the Past
| | |
|---|---|
| **Package** | `candidate` |
| **CA Gen source** | `AB-CREATE-CANDIDATE.ACB` lines 52–57 |
| **Original code** | `IF WS-DATE-OF-BIRTH >= CURRENT-DATE` |
| **BAL rule** | `"BR-08 Date Of Birth Must Be In The Past"` |
| **When** | `the dateOfBirth of the candidate of context >= context.today` |
| **Then** | Add violation: `"DATE OF BIRTH MUST BE IN THE PAST"` |

---

### BR-09 — No Duplicate Active Application
| | |
|---|---|
| **Package** | `candidate` |
| **CA Gen source** | `AB-CREATE-APPLICATION.ACB` lines 52–66 |
| **Original code** | `READ EACH LV-APP-EXISTING WHERE APPLICATION-STATUS NOT IN ('RE', 'IS')` |
| **Note** | Service layer pre-queries DB2; sets `returnCode=98` if active application found |
| **Return code** | 3 |

---

### BR-27 — Candidate Must Be Active
| | |
|---|---|
| **Package** | `candidate` |
| **CA Gen source** | `AB-CREATE-APPLICATION.ACB` lines 30–43 |
| **Original code** | `IF LV-CANDIDATE.RECORD-STATUS <> 'A'` |
| **BAL rule** | `"BR-27 Candidate Must Be Active To Submit Application"` |
| **When** | `the recordStatus of the candidate of context is not "A"` |

---

## Eligibility Rules

### BR-02 — Learner Minimum Age 16
| | |
|---|---|
| **Package** | `eligibility` |
| **CA Gen source** | `AB-CHECK-ELIGIBILITY.ACB` lines 63–69 |
| **Original code** | `IF WA-CANDIDATE-AGE < 16` |
| **BAL when** | `the licenseType of the application of context is "L"` + `the candidateAge of context < 16` |
| **Error message** | `CANDIDATE MUST BE AT LEAST 16 YEARS OLD FOR LEARNER LICENSE` |
| **Decision Table** | Also represented in `LicenseAgeAndValidityTable` row `row-learner` |

---

### BR-03 — Probation Minimum Age 17
| | |
|---|---|
| **Package** | `eligibility` |
| **CA Gen source** | `AB-CHECK-ELIGIBILITY.ACB` lines 71–77 |
| **BAL when** | `licenseType is "P"` + `candidateAge < 17` |
| **Error message** | `CANDIDATE MUST BE AT LEAST 17 YEARS OLD FOR PROBATION LICENSE` |

---

### BR-04 — Open Minimum Age 18
| | |
|---|---|
| **Package** | `eligibility` |
| **CA Gen source** | `AB-CHECK-ELIGIBILITY.ACB` lines 79–86 |
| **BAL when** | `licenseType is "O"` + `candidateAge < 18` |
| **Error message** | `CANDIDATE MUST BE AT LEAST 18 YEARS OLD FOR OPEN LICENSE` |

---

### BR-05 — Probation Requires Active Learner License
| | |
|---|---|
| **Package** | `eligibility` |
| **CA Gen source** | `AB-CHECK-ELIGIBILITY.ACB` lines 89–108 |
| **Original code** | `READ EACH LV-EXISTING-LICENSE WHERE LICENSE-TYPE = 'L' AND LICENSE-STATUS = 'A'` |
| **BAL when** | `licenseType is "P"` + no `IssuedLicense` with `licenseType="L"` and `licenseStatus="A"` |
| **Error message** | `CANDIDATE MUST HOLD AN ACTIVE LEARNER LICENSE TO APPLY FOR PROBATION` |

---

### BR-06 — Open Requires Active Probation License
| | |
|---|---|
| **Package** | `eligibility` |
| **CA Gen source** | `AB-CHECK-ELIGIBILITY.ACB` lines 89–108 |
| **BAL when** | `licenseType is "O"` + no `IssuedLicense` with `licenseType="P"` and `licenseStatus="A"` |
| **Error message** | `CANDIDATE MUST HOLD AN ACTIVE PROBATION LICENSE TO APPLY FOR OPEN` |

---

## History Rules

### BR-10 — Active Suspension Disqualifies
| | |
|---|---|
| **Package** | `history` |
| **CA Gen source** | `AB-CHECK-HISTORY.ACB` lines 65–73 |
| **Original code** | `IF INCIDENT-TYPE IN ('SU', 'DQ') AND (SUSPENSION-END-DATE IS NULL OR >= TODAY)` |
| **BAL when** | History record with `incidentType in (SU, DQ)` and `suspensionEndDate is null or >= today` |
| **Error message** | `CANDIDATE IS UNDER AN ACTIVE SUSPENSION OR DISQUALIFICATION` |

---

### BR-11 — Total Demerit Points ≤ 12
| | |
|---|---|
| **Package** | `history` |
| **CA Gen source** | `AB-CHECK-HISTORY.ACB` lines 90–94 |
| **Original code** | `IF WA-TOTAL-DEMERIT > 12` |
| **BAL when** | `totalDemeritPoints of context > 12` |
| **Error message** | `TOTAL DEMERIT POINTS EXCEED ALLOWABLE THRESHOLD OF 12` |
| **Note** | Demerit accumulation computed by helper rule `HIST-001` prior to this check |

---

### BR-12 — Unpaid Fines Disqualify
| | |
|---|---|
| **Package** | `history` |
| **CA Gen source** | `AB-CHECK-HISTORY.ACB` lines 78–86 |
| **Original code** | `IF FINE-AMOUNT > 0 AND FINE-PAID-STATUS = 'N'` |
| **BAL when** | Any active history record with `fineAmount > 0` and `finePaidStatus = "N"` |
| **Error message** | `CANDIDATE HAS OUTSTANDING UNPAID FINES` |

---

### BR-13 — Eligibility Before History (Gate Guard)
| | |
|---|---|
| **Package** | `history` |
| **CA Gen source** | `AB-CHECK-HISTORY.ACB` lines 53–58 |
| **Original code** | `IF LV-APPLICATION.ELIGIBILITY-CHECK-STATUS <> 'P'` |
| **BAL when** | `eligibilityCheckStatus of application is not "P"` |
| **Action** | Sets `returnCode=2` — short-circuits history ruleflow |

---

## Sequential Gate Rules

### BR-14 — History Before Payment
| | |
|---|---|
| **Package** | `payment` |
| **CA Gen source** | `AB-PROCESS-PAYMENT.ACB` lines 49–54 |
| **BAL when** | `historyCheckStatus is not "P"` |
| **Return code** | 4 |

---

### BR-15 — Payment Before First Approval
| | |
|---|---|
| **Package** | `approval` |
| **CA Gen source** | `AB-RECORD-APPROVAL-1.ACB` lines 51–56 |
| **BAL when** | `paymentStatus is not "P"` |
| **Return code** | 4 |

---

### BR-16 — First Approval Before Second Approval
| | |
|---|---|
| **Package** | `approval` |
| **CA Gen source** | `AB-RECORD-APPROVAL-2.ACB` lines 51–56 |
| **Agenda group** | `approval-level2-gate` |
| **BAL when** | `approval1Status is not "A"` |
| **Return code** | 4 |

---

### BR-17 — All Gates Before Issuance (Final Safety Re-check)
| | |
|---|---|
| **Package** | `issuance` |
| **CA Gen source** | `AB-ISSUE-LICENSE.ACB` lines 62–97 |
| **Sub-rules** | BR-17a through BR-17f — one per gate |
| **Note** | Re-checks: APPLICATION-STATUS=AP, eligibility=P, history=P, payment in (P,W), approval1=A, approval2=A |

---

## Approval Rules

### BR-18 — Approver Must Be Active
| | |
|---|---|
| **Package** | `approval` |
| **CA Gen source** | `AB-RECORD-APPROVAL-1/2.ACB` |
| **BAL when** | `activeStatus of approvalUser is not "A"` |
| **Return code** | 2 |

---

### BR-19 — Approver Level Enforcement
| | |
|---|---|
| **Package** | `approval` |
| **CA Gen source** | `AB-RECORD-APPROVAL-1.ACB` lines 59–67 |
| **Level 1 rule** | `authorityLevel is not "1"` → error |
| **Level 2 rule** | `authorityLevel is not "2"` → error (BR-19b in agenda-group `approval-level2-validate`) |

---

### BR-20 — License Type Authorisation
| | |
|---|---|
| **Package** | `approval` |
| **CA Gen source** | `AB-RECORD-APPROVAL-1.ACB` lines 69–74 |
| **Original code** | `IF LV-APPLICATION.LICENSE-TYPE NOT IN LV-AUTHORITY.LICENSE-TYPES-AUTHORISED` |
| **BAL when** | `licenseTypesAuthorised does not contain licenseType` |
| **Error message** | `AUTHORITY USER IS NOT AUTHORISED FOR THIS LICENSE TYPE` |

---

### BR-21 — Segregation of Duties: Different Approving Authorities
| | |
|---|---|
| **Package** | `approval` |
| **CA Gen source** | `AB-RECORD-APPROVAL-2.ACB` lines 76–82 |
| **Original code** | `IF LV-APPLICATION.APPROVAL-1-AUTHORITY = LV-AUTHORITY.AUTHORITY-NAME` |
| **BAL when** | `approval1Authority of application is authorityName of approvalUser` |
| **Error message** | `SECOND APPROVER MUST BE FROM A DIFFERENT AUTHORITY THAN FIRST APPROVER` |
| **Return code** | 5 |

---

## Payment Rules

### BR-22 — No Duplicate Payment
| | |
|---|---|
| **Package** | `payment` |
| **CA Gen source** | `AB-PROCESS-PAYMENT.ACB` lines 56–60 |
| **BAL when** | `paymentStatus of application is "P"` |
| **Error message** | `PAYMENT HAS ALREADY BEEN RECORDED FOR THIS APPLICATION` |

---

### BR-23 — Active Fee Schedule Required
| | |
|---|---|
| **Package** | `payment` |
| **CA Gen source** | `AB-PROCESS-PAYMENT.ACB` lines 63–74 |
| **BAL when** | `applicableFeeSchedule of context is null` |
| **Error message** | `NO ACTIVE FEE SCHEDULE FOUND FOR THIS LICENSE TYPE` |
| **Note** | Fee schedule pre-loaded by DLISQLUP from `DLIS.LICENSE_FEE_SCHEDULE` where `FEE_TYPE='IF'`, `ACTIVE_STATUS='A'`, `EFFECTIVE_DATE <= today` |

---

### BR-28 — Fee Amount Must Be Positive
| | |
|---|---|
| **Package** | `payment` |
| **CA Gen source** | Entity attribute validation |
| **BAL when** | `feeAmount of applicableFeeSchedule <= 0` |

---

## License Issuance Rules

### BR-24 — License Validity Periods
| | |
|---|---|
| **Package** | `issuance` |
| **CA Gen source** | `AB-ISSUE-LICENSE.ACB` lines 102–115 |
| **Agenda group** | `calculate-expiry` |
| **Rules** | 3 separate rules, one per license type |
| **L** | `context.today plus 1 year` |
| **P** | `context.today plus 2 years` |
| **O** | `context.today plus 5 years` |
| **Decision Table** | Also represented in `LicenseAgeAndValidityTable` Validity column |

---

### BR-25 — Vehicle Class Must Be A, B, C, or D
| | |
|---|---|
| **Package** | `issuance` |
| **CA Gen source** | `AB-ISSUE-LICENSE.ACB` lines 46–50 |
| **BAL when** | `vehicleClass of context is not "A" and not "B" and not "C" and not "D"` |
| **Error message** | `VEHICLE CLASS MUST BE A, B, C OR D` |
| **Return code** | 3 |

---

### BR-26 — Starting Demerit Balance = 12
| | |
|---|---|
| **Package** | `issuance` |
| **CA Gen source** | `AB-ISSUE-LICENSE.ACB` line 135 |
| **Action** | `set totalDemeritPoints of context to 12` |
| **Note** | Java service uses this value when building the DLISLICS COMMAREA |

---

### BR-29 — Expiry Date Must Be After Issue Date
| | |
|---|---|
| **Package** | `issuance` |
| **CA Gen source** | Entity validation + `AB-ISSUE-LICENSE` |
| **Agenda group** | `validate-expiry` |
| **BAL when** | `calculatedExpiryDate <= context.today` |
| **Error message** | `EXPIRY DATE MUST BE AFTER ISSUE DATE` |
| **Note** | This fires only if BR-24 produced an anomalous result — acts as a safety net |
