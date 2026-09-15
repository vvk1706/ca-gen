# 07 — Deployment Guide
## Step-by-Step Deployment: Rule Designer → RES → z/OS

---

## Overview

The DLIS ODM application consists of three deployment targets:

| Component | Deployment Target | Tool |
|---|---|---|
| BOM + Ruleflows + Rules | IBM ODM Rule Execution Server | Rule Designer / Decision Center |
| REST API + EXCI adapter | Java EE application server (e.g., WLP/WebSphere) | Maven / WAR deployment |
| CICS programs | z/OS CICS region | Standard CICS compile + install |
| DB2 schema | IBM Db2 for z/OS | JCL / DB2 Admin |

---

## 1. Prerequisites

### ODM
- IBM ODM 8.10 or later (Rule Designer + Decision Center + RES)
- Java 11+
- Maven 3.6+

### Mainframe
- z/OS 2.3+
- CICS TS 5.4+
- IBM Db2 for z/OS 12+
- IBM CICS Transaction Gateway 9.2+ (for EXCI)
- Enterprise COBOL 6.3+

---

## 2. DB2 Schema Deployment

1. Transfer [`mainframe/db2/DLIS-DDL.sql`](../mainframe/db2/DLIS-DDL.sql) to z/OS
2. Edit the file to replace `DLIS` schema name if required by your installation standards
3. Run via DSNTEP2 batch job or DB2 Admin Tool:
   ```jcl
   //DLISINST JOB CLASS=A,MSGCLASS=X
   //STEP1    EXEC PGM=DSNTEP2
   //SYSPRINT DD SYSOUT=*
   //SYSTSIN  DD *
     DSN SYSTEM(DBXX)
     RUN PROGRAM(DSNTEP2) PLAN(DSNTEP2) -
         LIB('SYS1.DB2.RUNLIB.LOAD')
   //SYSIN    DD DSN=HLQ.DLIS.DDL(DLISDDL),DISP=SHR
   /*
   ```
4. Grant permissions (see Section 6 of [`06-MAINFRAME-INTEGRATION.md`](06-MAINFRAME-INTEGRATION.md))

---

## 3. CICS Program Deployment

For each of `DLISQLUP.cbl`, `DLISPAY.cbl`, `DLISLICS.cbl`:

### Step 3a — Compile
```jcl
//COBCOMP  EXEC PGM=IGYCRCTL,PARM='RENT,APOST,CICS,SQL'
//SYSLIB   DD DSN=CICS.SDFHCOB,DISP=SHR
//         DD DSN=DSN.SDSNMACS,DISP=SHR   (Db2 macros)
//SYSIN    DD DSN=HLQ.DLIS.SRC(DLISPAY),DISP=SHR
//SYSLIN   DD DSN=&&OBJ,DISP=(MOD,PASS)
//SYSPRINT DD SYSOUT=*
```

### Step 3b — Link-Edit
```jcl
//LKED     EXEC PGM=IEWL,PARM='RENT,REUS'
//SYSLIB   DD DSN=CICS.SDFHLOAD,DISP=SHR
//SYSLIN   DD DSN=&&OBJ,DISP=(OLD,DELETE)
//SYSLMOD  DD DSN=HLQ.DLIS.LOADLIB(DLISPAY),DISP=SHR
//SYSPRINT DD SYSOUT=*
```

### Step 3c — Install in CICS
```cics
CEDA DEFINE PROGRAM(DLISPAY) GROUP(DLIS) LANGUAGE(COBOL) DATALOCATION(ANY)
CEDA INSTALL PROGRAM(DLISPAY) GROUP(DLIS)
```

Repeat for `DLISQLUP` and `DLISLICS`.

---

## 4. ODM Rule Project Deployment

### Step 4a — Import BOM into Rule Designer
1. Open IBM Rule Designer
2. File → Import → General → Existing Projects into Workspace
3. Navigate to `odm-from-cagen/bom/`
4. Build the BOM project (Maven: `mvn clean install`)

### Step 4b — Create Rule Project
1. File → New → Rule Project → Name: `dlis-rules`
2. Set BOM dependency to `dlis-bom`
3. Import rule files:
   - `rules/candidate/candidate-rules.brl`
   - `rules/eligibility/eligibility-rules.brl`
   - `rules/history/history-rules.brl`
   - `rules/payment/payment-rules.brl`
   - `rules/approval/approval-rules.brl`
   - `rules/issuance/issuance-rules.brl`
4. Import decision tables:
   - `decision-tables/LicenseAgeAndValidityTable.dtt`
   - `decision-tables/LicenseUpgradePathTable.dtt`
5. Import ruleflows from `ruleflows/`

### Step 4c — Deploy to Decision Center
1. In Rule Designer: RuleApp → Deploy to Decision Center
2. Target URL: `http://<dc-host>:9060/teamserver`
3. Create RuleApp: `DLIS-Rules`
4. Create Ruleset for each package:
   - `DLIS-Rules/candidate`
   - `DLIS-Rules/eligibility`
   - `DLIS-Rules/history`
   - `DLIS-Rules/payment`
   - `DLIS-Rules/approval`
   - `DLIS-Rules/issuance`

### Step 4d — Deploy to RES
1. In Decision Center: Deployments → Deploy
2. Target: Rule Execution Server
3. RES URL: `http://<res-host>:9080/res`
4. Confirm all rulesets are in status **Running**
5. Test via RES Console → Ruleset Test

---

## 5. Java REST Layer Deployment

### Step 5a — Configure Environment
Set environment variables on the application server:

```bash
export CICS_APPLID=CICSPROD
export CICS_USERID=DLISUSR
export CICS_COMMAREA_MAX=32767
export ODM_RES_URL=http://res-host:9080/res
export ODM_RULESET_PATH=/DLIS-Rules
```

### Step 5b — Build
```bash
cd odm-from-cagen/service
mvn clean package
```

### Step 5c — Deploy WAR
```bash
# WebSphere Liberty
cp target/dlis-odm.war $WLP_HOME/usr/servers/defaultServer/dropins/

# Or: configure server.xml
<application location="dlis-odm.war" type="war" contextRoot="/api"/>
```

### Step 5d — Verify
```bash
# Health check
curl http://localhost:9080/api/dlis/applications/12345/status

# Eligibility check
curl -X POST http://localhost:9080/api/dlis/applications/12345/eligibility-check \
  -H "Content-Type: application/json" \
  -d '{"checkedBy":"OFFICER1"}'
```

---

## 6. End-to-End Test Sequence

Run the following sequence to verify the complete workflow:

```bash
BASE=http://localhost:9080/api/dlis

# Step 1: Create candidate
curl -X POST $BASE/candidates \
  -d '{"firstName":"John","lastName":"Smith","dateOfBirth":"2000-01-15",
       "idNumber":"ID12345678","addressLine1":"1 Main St",
       "city":"Sydney","stateProvince":"NSW","country":"Australia"}'

# Step 2: Create application (Learner)
curl -X POST $BASE/applications \
  -d '{"candidateId":1001,"licenseType":"L"}'

# Step 3: Eligibility check
curl -X POST $BASE/applications/2001/eligibility-check \
  -d '{"checkedBy":"OFFICER1"}'

# Step 4: History check
curl -X POST $BASE/applications/2001/history-check \
  -d '{"checkedBy":"OFFICER1"}'

# Step 5: Payment
curl -X POST $BASE/applications/2001/payment \
  -d '{"paymentMethod":"CC","paymentReference":"CC-REF-001","processedBy":"OFFICER1"}'

# Step 6: First approval
curl -X POST $BASE/applications/2001/approvals/1 \
  -d '{"authorityUserCode":"AUTH001","decision":"A","notes":"Approved - all checks passed"}'

# Step 7: Second approval (different authority)
curl -X POST $BASE/applications/2001/approvals/2 \
  -d '{"authorityUserCode":"AUTH002","decision":"A","notes":"Confirmed approval"}'

# Step 8: Issue license
curl -X POST $BASE/applications/2001/issue \
  -d '{"vehicleClass":"B","restrictions":"","issuedByOfficer":"OFFICER2","issuedByAuthority":"RTA"}'

# Check final status
curl $BASE/applications/2001/status
```

Expected final `applicationStatus`: `"IS"` (License Issued)
Expected `generatedLicenseNumber`: `"LRNB10012001"` (prefix + class + candidateId + applicationId)

---

## 7. Decision Center Governance

Once deployed, business users can manage rules via Decision Center without developer involvement:

| Change Type | Decision Center Action | No Code Required |
|---|---|---|
| Change minimum age | Edit `LicenseAgeAndValidityTable.dtt` row values | ✓ |
| Add new license type | Add row to both decision tables | ✓ |
| Change validity period | Edit `LicenseAgeAndValidityTable.dtt` Validity column | ✓ |
| Modify demerit threshold (BR-11) | Edit rule `BR-11` `when` condition | ✓ |
| Add new fee type | Extend payment BAL rules | Requires developer |
| Change error messages | Edit rule `then` action text | ✓ |

All changes in Decision Center are versioned. Rollback is possible at any time via version history.
