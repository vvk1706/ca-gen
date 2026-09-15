# 06 — Mainframe Integration
## CICS Adapter, EXCI Bridge, DB2 DDL, COMMAREA Layouts

---

## 1. Integration Architecture

The ODM DLIS application preserves the existing mainframe infrastructure. IBM Db2 for z/OS remains the system of record. CICS programs handle all DB2 I/O. The Java ODM layer connects to CICS via the **External CICS Interface (EXCI)** through IBM CICS Transaction Gateway.

```
Java ODM REST Layer
        │
        │  ExciCicsAdapter.java
        │  EXEC CICS LINK (via CTG TCP/IP)
        ▼
z/OS CICS Region
        │
        ├── DLISQLUP  (read: load rule context)
        ├── DLISPAY   (write: record payment)
        ├── DLISLICS  (write: issue license)
        └── DLISUPD   (write: status updates)
        │
        ▼
IBM Db2 for z/OS — Schema DLIS
```

---

## 2. CICS Programs

### DLISQLUP — Data Lookup
**File:** [`mainframe/cics/DLISQLUP.cbl`](../mainframe/cics/DLISQLUP.cbl)

**Purpose:** Retrieves all data required by the ODM rule engine before any ruleset is called. This is the pre-rule data loader.

**Queries executed:**
1. `DLIS.LICENSE_APPLICATION` — by `APPLICATION_ID`
2. `DLIS.CANDIDATE` — by `CANDIDATE_ID` from application
3. `DLIS.LICENSE_FEE_SCHEDULE` — active `IF` fee for license type

**Called by:** Every REST endpoint before ODM execution
**Direction:** Java → CICS → DB2 → CICS → Java

---

### DLISPAY — Payment Write
**File:** [`mainframe/cics/DLISPAY.cbl`](../mainframe/cics/DLISPAY.cbl)

**Purpose:** Executes DB2 transactional writes for payment processing AFTER ODM rules pass.

**DB2 operations:**
1. `INSERT INTO DLIS.PAYMENT` — creates payment record
2. `UPDATE DLIS.LICENSE_APPLICATION SET PAYMENT_STATUS='P', APPLICATION_STATUS='PA'`

**Triggered by:** `POST /applications/{id}/payment` — only on successful ODM evaluation
**Returns:** `CA-PAYMENT-ID`, `CA-RECEIPT-NUMBER`, `CA-RETURN-CODE`

---

### DLISLICS — License Issuance Write
**File:** [`mainframe/cics/DLISLICS.cbl`](../mainframe/cics/DLISLICS.cbl)

**Purpose:** Executes DB2 transactional writes for license issuance AFTER ODM rules pass.

**DB2 operations:**
1. `INSERT INTO DLIS.ISSUED_LICENSE` — creates issued license record with `DEMERIT_BALANCE=12`
2. `UPDATE DLIS.LICENSE_APPLICATION SET APPLICATION_STATUS='IS'`

**Triggered by:** `POST /applications/{id}/issue` — only on successful ODM evaluation
**Returns:** `CA-LICENSE-ID`, `CA-RETURN-CODE`

---

## 3. COMMAREA Layouts

All COMMAREA structures use a 4-byte version marker `'0001'` as the first field. The Java `ExciCicsAdapter` validates this before processing.

### Payment COMMAREA (DLISPAY)

| Field | PIC | Position | Direction |
|---|---|---|---|
| `CA-VERSION` | `X(4)` | 0–3 | Input |
| `CA-APPLICATION-ID` | `9(10)` | 4–8 | Input |
| `CA-CANDIDATE-ID` | `9(10)` | 9–13 | Input |
| `CA-PAYMENT-METHOD` | `X(2)` | 14–15 | Input |
| `CA-PAYMENT-REFERENCE` | `X(30)` | 16–45 | Input |
| `CA-PROCESSED-BY` | `X(20)` | 46–65 | Input |
| `CA-LICENSE-TYPE` | `X(1)` | 66 | Input |
| `CA-FEE-AMOUNT` | `9(10)V99 COMP-3` | 67–72 | Input |
| `CA-PAYMENT-ID` | `9(10)` | 73–77 | **Output** |
| `CA-RECEIPT-NUMBER` | `X(20)` | 78–97 | **Output** |
| `CA-RETURN-CODE` | `9(4) COMP` | 98–99 | **Output** |
| `CA-RETURN-MSG` | `X(100)` | 100–199 | **Output** |

### License Issuance COMMAREA (DLISLICS)

| Field | PIC | Direction |
|---|---|---|
| `CA-VERSION` | `X(4)` | Input |
| `CA-APPLICATION-ID` | `9(10)` | Input |
| `CA-CANDIDATE-ID` | `9(10)` | Input |
| `CA-LICENSE-NUMBER` | `X(20)` | Input |
| `CA-LICENSE-TYPE` | `X(1)` | Input |
| `CA-ISSUE-DATE` | `X(10)` | Input |
| `CA-EXPIRY-DATE` | `X(10)` | Input |
| `CA-VEHICLE-CLASS` | `X(2)` | Input |
| `CA-RESTRICTIONS` | `X(200)` | Input |
| `CA-DEMERIT-BALANCE` | `9(3) COMP` | Input (always 12) |
| `CA-ISSUED-BY-AUTHORITY` | `X(50)` | Input |
| `CA-ISSUED-BY-OFFICER` | `X(50)` | Input |
| `CA-LICENSE-ID` | `9(10)` | **Output** |
| `CA-RETURN-CODE` | `9(4) COMP` | **Output** |
| `CA-RETURN-MSG` | `X(100)` | **Output** |

---

## 4. DB2 DDL

**File:** [`mainframe/db2/DLIS-DDL.sql`](../mainframe/db2/DLIS-DDL.sql)

All 7 tables converted from CA Gen entity definitions. Schema: `DLIS`. Platform: IBM Db2 for z/OS.

| Table | Origin | Key Constraints |
|---|---|---|
| `DLIS.CANDIDATE` | `CANDIDATE.ENT` | PK auto-identity; UQ on `ID_NUMBER` |
| `DLIS.LICENSE_APPLICATION` | `LICENSE-APPLICATION.ENT` | FK to CANDIDATE; CHECK on all status codes |
| `DLIS.DRIVING_HISTORY` | `DRIVING-HISTORY.ENT` | FK to CANDIDATE; CHECK on incident type |
| `DLIS.PAYMENT` | `PAYMENT.ENT` | FK to APPLICATION + CANDIDATE; CHECK `PAYMENT_AMOUNT > 0` |
| `DLIS.ISSUED_LICENSE` | `ISSUED-LICENSE.ENT` | UQ on `LICENSE_NUMBER`; CHECK `EXPIRY_DATE > ISSUE_DATE` |
| `DLIS.AUTHORITY_USER` | `AUTHORITY-USER.ENT` | UQ on `USER_CODE`; CHECK on level (`1` or `2`) |
| `DLIS.LICENSE_FEE_SCHEDULE` | `LICENSE-FEE-SCHEDULE.ENT` | CHECK `FEE_AMOUNT > 0`; index on type+feeType+date |

---

## 5. ExciCicsAdapter Configuration

**File:** [`service/src/main/java/com/dlis/odm/service/ExciCicsAdapter.java`](../service/src/main/java/com/dlis/odm/service/ExciCicsAdapter.java)

**Environment variables:**

| Variable | Default | Description |
|---|---|---|
| `CICS_APPLID` | `CICSPROD` | VTAM applid of the target CICS region |
| `CICS_USERID` | `DLISUSR` | RACF user ID for EXCI link |
| `CICS_COMMAREA_MAX` | `32767` | Maximum COMMAREA size in bytes |

**EBCDIC handling:**
- `toEbcdic(String, int)` — converts Java String to IBM-037 EBCDIC padded field
- `fromEbcdic(byte[], int, int)` — decodes IBM-037 field from COMMAREA
- `writeComp3` / `readComp3` — packed decimal (COMP-3) conversion

**Prerequisites:**
- IBM CICS Transaction Gateway (CTG) client library on classpath
- Network connectivity from Java server to z/OS TCP port (default 2006 for EXCI)
- RACF security grant for `DLISUSR` to LINK DLISPAY, DLISLICS, DLISQLUP

---

## 6. RACF Security Requirements

| Resource | Type | Required Access |
|---|---|---|
| `DLISQLUP` | CICS PROGRAM | READ + EXECUTE |
| `DLISPAY` | CICS PROGRAM | READ + EXECUTE |
| `DLISLICS` | CICS PROGRAM | READ + EXECUTE |
| `DLIS.CANDIDATE` | DB2 TABLE | SELECT |
| `DLIS.LICENSE_APPLICATION` | DB2 TABLE | SELECT + UPDATE |
| `DLIS.DRIVING_HISTORY` | DB2 TABLE | SELECT |
| `DLIS.PAYMENT` | DB2 TABLE | SELECT + INSERT |
| `DLIS.ISSUED_LICENSE` | DB2 TABLE | SELECT + INSERT |
| `DLIS.LICENSE_FEE_SCHEDULE` | DB2 TABLE | SELECT |

---

## 7. Error Handling

CICS adapter errors are surfaced as `ExciCicsAdapter.CicsLinkException` and mapped to HTTP 503 (Service Unavailable) at the REST layer. The ODM rule evaluation is not affected — rules only fail if a business rule violation occurs.

DB2 SQLCODE errors from CICS programs are returned in `CA-RETURN-CODE = 99` with `CA-RETURN-MSG` containing `"DB2 INSERT/UPDATE FAILED SQLCODE=xxxx"`.
