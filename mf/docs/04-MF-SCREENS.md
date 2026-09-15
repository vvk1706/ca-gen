# 04 — BMS Screens

All screens are 24×80 3270 terminals.  
Field naming convention: `<map-prefix><field-abbrev>I` for input (UNPROT), `<map-prefix><field-abbrev>O` for output (PROT).

---

## Screen: DLISMM01 — Main Menu

**Mapset:** DLISMM  **Transaction:** DL01  
**Source:** [`bms/DLISMM.bms`](../bms/DLISMM.bms)

### Layout

```
 Row  Col  Content
  1   1    ***** DRIVER LICENSE ISSUANCE SYSTEM (DLIS)          [PROT BRT]
  4  66    Date (DMMDATE)                                        [PROT 10]
  6   5    --- CANDIDATE ---
  7   7    1.  CANDIDATE MAINTENANCE
  8   7    2.  CANDIDATE INQUIRY
 10   5    --- APPLICATION ---
 11   7    3.  NEW LICENSE APPLICATION
 12   7    4.  APPLICATION STATUS INQUIRY
 14   5    --- PROCESSING ---
 15   7    5.  ELIGIBILITY CHECK
 16   7    6.  DRIVING HISTORY CHECK
 17   7    7.  PAYMENT ENTRY
 19   5    --- APPROVALS ---
 20   7    8.  FIRST AUTHORITY APPROVAL
 21   7    9.  SECOND AUTHORITY APPROVAL
 22   5    10. ISSUE LICENSE
 15  45    SELECT OPTION:                                        [PROT]
 15  61    DMMOPT (2)                                           [UNPROT IC]
 23   2    DMMMSG (76) message area                             [PROT]
 24   2    ENTER=SELECT  F12=EXIT                               [PROT]
```

### Fields

| Map field | Len | Attr | Description |
|---|---|---|---|
| DMMDATE | 10 | PROT NORM | Current date displayed on init |
| DMMOPT | 2 | UNPROT IC | Option selection (1–10) |
| DMMMSG | 76 | PROT NORM | Error / status messages |

### PF keys

| Key | Action |
|---|---|
| ENTER | Process option selection |
| F12 | Exit CICS |

---

## Screen: DLISCM01 — Candidate Maintenance

**Mapset:** DLISCM  **Transaction:** DL02  
**Source:** [`bms/DLISCM.bms`](../bms/DLISCM.bms)

### Layout

```
 Row  Col  Content
  1   1    DLIS            CANDIDATE MAINTENANCE                 [PROT BRT]
  1  72    DCMDATE (10)                                          [PROT]
  2   1    ─── separator line ───────────────────────────────
  4   2    CANDIDATE ID    :   DCMCID  (10)  [NUM UNPROT]
  5   2    FIRST NAME      :   DCMFNAM (30)  [UNPROT IC]
  6   2    LAST NAME       :   DCMLNAM (30)  [UNPROT]
  7   2    DATE OF BIRTH   :   DCMDOB  (10)  [UNPROT]
  8   2    NATIONAL ID NO  :   DCMIDNO (20)  [UNPROT]
  9   2    ADDRESS LINE 1  :   DCMADR1 (50)  [UNPROT]
 10   2    ADDRESS LINE 2  :   DCMADR2 (50)  [UNPROT]
 11   2    CITY            :   DCMCITY (30)  [UNPROT]
 12   2    STATE/PROVINCE  :   DCMSTAT (30)  [UNPROT]
 13   2    POSTAL CODE     :   DCMPOST (10)  [UNPROT]
 14   2    COUNTRY         :   DCMCTRY (30)  [UNPROT]
 15   2    PHONE NUMBER    :   DCMPHN  (15)  [UNPROT]
 16   2    EMAIL ADDRESS   :   DCMEML  (60)  [UNPROT]
 17   2    STATUS          :   DCMSTS  ( 1)  [UNPROT]
 22   2    DCMMSG (76) message area
 24   2    F1=CREATE  F2=UPDATE  F3=INQUIRE  F4=DEACTIVATE  F12=EXIT
```

### Fields

| Map field | Len | Attr | I/O | Description |
|---|---|---|---|---|
| DCMDATE | 10 | PROT | O | Screen date |
| DCMCID | 10 | NUM UNPROT | I/O | Candidate ID |
| DCMFNAM | 30 | UNPROT IC | I/O | First name |
| DCMLNAM | 30 | UNPROT | I/O | Last name |
| DCMDOB | 10 | UNPROT | I/O | Date of birth |
| DCMIDNO | 20 | UNPROT | I/O | National ID number |
| DCMADR1 | 50 | UNPROT | I/O | Address line 1 |
| DCMADR2 | 50 | UNPROT | I/O | Address line 2 |
| DCMCITY | 30 | UNPROT | I/O | City |
| DCMSTAT | 30 | UNPROT | I/O | State / Province |
| DCMPOST | 10 | UNPROT | I/O | Postal code |
| DCMCTRY | 30 | UNPROT | I/O | Country |
| DCMPHN | 15 | UNPROT | I/O | Phone number |
| DCMEML | 60 | UNPROT | I/O | Email address |
| DCMSTS | 1 | UNPROT | I/O | Record status (A/I) |
| DCMMSG | 76 | PROT | O | Message area |

### PF keys

| Key | Function |
|---|---|
| F1 | Create new candidate |
| F2 | Update existing candidate (requires CANDIDATE ID) |
| F3 | Inquire — search by CANDIDATE ID or NATIONAL ID |
| F4 | Deactivate candidate (sets status to I) |
| F12 | Exit |

---

## Screen: DLISAE01 — Licence Application Entry

**Mapset:** DLISAE  **Transaction:** DL03  
**Source:** [`bms/DLISAE.bms`](../bms/DLISAE.bms)

### Layout

```
 Row  Col  Content
  1   1    DLIS            LICENSE APPLICATION ENTRY             [PROT BRT]
  1  72    DAEDATE (10)                                          [PROT]
  2   1    ─── separator ────────────────────────────────────────
  4  20    DAEAID  (10) Application ID                          [PROT]
  5  20    DAECID  (10) Candidate ID                            [NUM UNPROT IC]
  6  20    DAECNAM (60) Candidate Name                          [PROT]
  8  20    DAELTYP ( 1) License Type                            [UNPROT]
  8  22    (L=LEARNER  P=PROBATION  O=OPEN)
 10  20    DAEADAT (10) Application Date                        [PROT]
 11  20    DAEASTT ( 2) Status                                  [PROT]
 13  20    DAEFEE  (14) Fee Applicable                          [PROT]
 14  20    DAECURR ( 3) Currency                                [PROT]
 22   2    DAEMSG  (76) message
 24   2    F1=SUBMIT  F3=LOOKUP CANDIDATE  F5=CALC FEE  F12=EXIT
```

### Fields

| Map field | Len | Attr | I/O | Description |
|---|---|---|---|---|
| DAEDATE | 10 | PROT | O | Screen date |
| DAEAID | 10 | PROT | O | Generated application ID |
| DAECID | 10 | NUM UNPROT IC | I | Candidate ID to look up |
| DAECNAM | 60 | PROT | O | Candidate full name |
| DAELTYP | 1 | UNPROT | I | L / P / O |
| DAEADAT | 10 | PROT | O | Application date |
| DAEASTT | 2 | PROT | O | Application status code |
| DAEFEE | 14 | PROT | O | Fee amount (formatted ZZZ,ZZ9.99) |
| DAECURR | 3 | PROT | O | Currency code |
| DAEMSG | 76 | PROT | O | Message area |

### PF keys

| Key | Function |
|---|---|
| F1 | Submit application |
| F3 | Look up candidate by ID |
| F5 | Calculate applicable fee |
| F12 | Exit |

---

## Screen: DLISEC01 — Eligibility Check

**Mapset:** DLISEC  **Transaction:** DL05  
**Source:** [`bms/DLISECHPE.bms`](../bms/DLISECHPE.bms) (combined mapset file)

### Layout

```
 Row  Content
  1   DLIS                  ELIGIBILITY CHECK
  4   APPLICATION ID   :  DECAID  (10) [NUM UNPROT IC]
  5   CANDIDATE NAME   :  DECCNAM (60) [PROT]
  6   DATE OF BIRTH    :  DECDOB  (10) [PROT]
  7   CALCULATED AGE   :  DECAGE  ( 3) [PROT]
  8   LICENSE TYPE     :  DECLTYP ( 1) [PROT]   DECLDSC (20) [PROT]
  9   MIN AGE REQUIRED :  DECMAGE ( 3) [PROT]
 10   PRIOR LICENSE    :  DECPRIOR(30) [PROT]
 12   CHECK STATUS     :  DECESTS ( 1) [PROT BRT]  DECEDSC (30) [PROT]
 13   CHECK NOTES      :  DECENTS (60) [PROT]
 15   CHECKED BY       :  DECCHKBY(20) [UNPROT]
 22   DECMSG (76) message
 24   F1=RUN CHECK  F3=LOOKUP APP  F12=EXIT
```

### PF keys

| Key | Function |
|---|---|
| F1 | Run eligibility check |
| F3 | Load application and candidate data |
| F12 | Exit |

---

## Screen: DLISHC01 — Driving History Check

**Mapset:** DLISHC  **Transaction:** DL06  
**Source:** [`bms/DLISECHPE.bms`](../bms/DLISECHPE.bms)

### Layout

```
 Row  Content
  1   DLIS              DRIVING HISTORY CHECK
  4   APPLICATION ID   :  DHCAID  (10) [NUM UNPROT IC]
  5   CANDIDATE NAME   :  DHCCNAM (60) [PROT]
  6   LICENSE TYPE     :  DHCLTYP ( 1) [PROT]
  8   HISTORY RECORDS (ACTIVE):
  9   DATE       TYPE  DESCRIPTION                     DEMERIT
 10   ───────────────────────────────────────────────────────
 11   DHCHL1 (76) [PROT]  ← scrollable rows
 12   DHCHL2 (76) [PROT]
 13   DHCHL3 (76) [PROT]
 14   DHCHL4 (76) [PROT]
 15   DHCHL5 (76) [PROT]
 16   DHCHL6 (76) [PROT]
 17   DHCHL7 (76) [PROT]
 18   ───────────────────────────────────────────────────────
 19   TOTAL DEMERITS  :  DHCDMRT ( 3) [PROT BRT]
 20   ACTIVE SUSPENSN :  DHCSUSP ( 3) [PROT BRT]
 21   UNPAID FINES    :  DHCFINE ( 3) [PROT BRT]
 22   HISTORY STATUS  :  DHCHSTS ( 1) [PROT BRT]  DHCHSDSC (30)
 23   DHCMSG (76) message
 24   F1=RUN CHECK  F3=LOOKUP  F7=UP  F8=DOWN  F12=EXIT
```

The scroll window shows 7 rows at a time from the internal 50-row history table.  
F7 scrolls backward, F8 scrolls forward.

### PF keys

| Key | Function |
|---|---|
| F1 | Run history check |
| F3 | Load application |
| F7 | Scroll up |
| F8 | Scroll down |
| F12 | Exit |

---

## Screen: DLISPE01 — Payment Entry

**Mapset:** DLISPE  **Transaction:** DL07  
**Source:** [`bms/DLISECHPE.bms`](../bms/DLISECHPE.bms)

### Layout

```
 Row  Content
  1   DLIS                   PAYMENT ENTRY
  4   APPLICATION ID   :  DPEAID  (10) [NUM UNPROT IC]
  5   CANDIDATE NAME   :  DPECNAM (60) [PROT]
  6   LICENSE TYPE     :  DPELTYP ( 1) [PROT]  DPELDSC (20) [PROT]
  8   FEE APPLICABLE   :  DPEFEE  (14) [PROT BRT]
  9   CURRENCY         :  DPECURR ( 3) [PROT]
 11   PAYMENT METHOD   :  DPEMETH ( 2) [UNPROT]  (CC/DC/EF/CS/CH)
 12   PAYMENT REF      :  DPEREF  (30) [UNPROT]
 13   PROCESSED BY     :  DPEPRCBY(20) [UNPROT]
 15   PAYMENT ID       :  DPEPID  (10) [PROT]
 16   RECEIPT NUMBER   :  DPERCT  (20) [PROT BRT]
 17   PAYMENT STATUS   :  DPEPSTS ( 1) [PROT]
 22   DPEMSG (76) message
 24   F1=PROCESS PAYMENT  F3=LOOKUP APP  F5=PRINT RECEIPT  F12=EXIT
```

### PF keys

| Key | Function |
|---|---|
| F1 | Process payment |
| F3 | Load application details |
| F5 | Print receipt (LINK to DLISRPRT) |
| F12 | Exit |

---

## Screen: DLISA1M01 — First Authority Approval

**Mapset:** DLISA1  **Transaction:** DL08  
*(BMS source not included in source tree — defined in CSD only)*

Screen presents full application status checklist plus authority code and decision fields.

### Key fields (from program source)

| Map field | I/O | Description |
|---|---|---|
| DA1AID | I | Application ID |
| DA1CNAM | O | Candidate name |
| DA1LTY | O | Licence type |
| DA1ASTT | O | Application status |
| DA1ELIG | O | Eligibility check status |
| DA1HIST | O | History check status |
| DA1PAY | O | Payment status |
| DA1PYRF | O | Payment reference |
| DA1ELNT | O | Eligibility check notes |
| DA1HSNT | O | History check notes |
| DA1AUCO | I | Authority user code |
| DA1AUNM | O | Authority user name |
| DA1DEC | I | Decision (A=Approve / R=Reject) |
| DA1DCNT | I | Decision notes |
| DA1MSG | O | Message area |

### PF keys

| Key | Function |
|---|---|
| F1 | Submit first approval decision |
| F3 | Load application |
| F4 | Validate authority user |
| F12 | Exit |

---

## Screen: DLISA2M01 — Second Authority Approval

**Mapset:** DLISA2  **Transaction:** DL09  
*(BMS source not included in source tree — defined in CSD only)*

Mirrors DLISA1M01 with additions:

| Additional fields | Description |
|---|---|
| DA2A1ST | First approval status (from application) |
| DA2A1NM | First approver authority name (for segregation display) |

### PF keys — identical to DLISAP1

---

## Screen: DLISLIM01 — Licence Issuance

**Mapset:** DLISLI  **Transaction:** DL10  
*(BMS source not included in source tree — defined in CSD only)*

### Key fields (from program source)

| Map field | I/O | Description |
|---|---|---|
| DLIAID | I | Application ID |
| DLICNAM | O | Candidate name |
| DLILTYP | O | Licence type |
| DLIELIG0 | O | Eligibility status |
| DLIHIST | O | History status |
| DLIPAY | O | Payment status |
| DLIAU1 | O | First approval status |
| DLIAU2 | O | Second approval status |
| DLIVCLSI | I | Vehicle class (A/B/C/D) |
| DLIOFCRI | I | Issuing officer name |
| DLIAUTHI | I | Issuing authority name |
| DLIRSTI | I | Restrictions (free text) |
| DLIILNUM | O | Generated licence number |
| DLIEXP | O | Expiry date |
| DLIMSG | O | Message area |

### PF keys

| Key | Function |
|---|---|
| F1 | Issue licence |
| F3 | Load application |
| F6 | Print licence (LINK to DLISLPRT) |
| F12 | Exit |

---

## Screen: DLISST01 — Application Status Inquiry

**Mapset:** DLISST  **Transaction:** DL04  
*(BMS source not included in source tree — defined in CSD only)*

### Key fields (from program source)

| Map field | I/O | Description |
|---|---|---|
| DSTAID | I | Application ID |
| DSTLTYP | O | Licence type |
| DSTASTT | O | Application status code |
| DSTADS | O | Status description (human-readable) |
| DST1STS | O | Eligibility check status |
| DST1DAT | O | Eligibility check date |
| DST1NTS | O | Eligibility check notes |
| DST2STS | O | History check status |
| DST2DAT | O | History check date |
| DST2NTS | O | History check notes |
| DST3STS | O | Payment status |
| DST3REF | O | Payment reference |
| DST4STS | O | First approval status |
| DST4DAT | O | First approval date |
| DST4AUT | O | First approval authority |
| DST5STS | O | Second approval status |
| DST5DAT | O | Second approval date |
| DST5AUT | O | Second approval authority |
| DST6NUM | O | Issued licence number (if IS) |
| DST6EXP | O | Licence expiry date |
| DST6FLG | O | YES / NO — licence issued flag |
| DSTMSG | O | Message area |

### PF keys

| Key | Function |
|---|---|
| ENTER | Perform inquiry |
| F12 | Exit |

---

## BMS field attribute summary

| Attribute | Meaning |
|---|---|
| PROT | Protected — user cannot type |
| UNPROT | Unprotected — user can type |
| NUM | Numeric shift |
| BRT | Bright (highlighted) |
| NORM | Normal intensity |
| IC | Initial cursor position on this field |
| FREEKB | Keyboard unlocked after send |
| SIZE=(24,80) | Standard 3270 screen size |
| MODE=INOUT | Bidirectional map (send and receive) |
| TERM=3270-2 | IBM 3270 Model 2 terminal |
