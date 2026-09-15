# 05 — Build & Deploy

## Prerequisites

| Component | Description |
|---|---|
| IBM Enterprise COBOL (IGYCRCTL) | COBOL compiler |
| DB2 for z/OS Precompiler (DSNHPC) | SQL precompiler, produces DBRM |
| CICS Translator | Translates EXEC CICS into COBOL calls (run inside IGYCRCTL with LIB option) |
| ASMA90 | High-Level Assembler for BMS maps |
| IEWL | Binder/Linkeditor |
| DFHCSDUP | CICS CSD utility |
| DSNTEP2 | DB2 dynamic SQL utility |

---

## Dataset naming conventions

| Dataset | Purpose |
|---|---|
| `DLIS.COBOL` | COBOL source library (partitioned) |
| `DLIS.COPYBOOK` | Copybook library |
| `DLIS.BMS` | BMS mapset source |
| `DLIS.DBRM` | DB2 database request modules (output from precompile) |
| `DLIS.LOADLIB` | Load module library (output from link-edit) |
| `DLIS.DDL` | DB2 DDL members |
| `CICSTS.DFHCSD` | CICS CSD file |

---

## Build sequence overview

```
Step 1: DB2 DDL
         DLISDB2.jcl
             STEP010 → tables (DLIS0001)
             STEP020 → indexes (DLIS0002)
             STEP030 → grants (DLIS0003)

Step 2: BMS Assembly
         DLISBMS.jcl
             each mapset → DLISBASM PROC
                 ASM1 (MAP pass)  → load library
                 ASM2 (DSECT pass) → DLIS.COPYBOOK (symbolic map)
                 LKED             → DLIS.LOADLIB

Step 3: COBOL Compile
         DLISCOMP.jcl
             each program → DLISCLNK PROC
                 PC    (DB2 precompile) → DLIS.DBRM + temp output
                 COBOL (IGYCRCTL)       → object module
                 LKED  (IEWL)           → DLIS.LOADLIB

Step 4: DB2 Bind
         DLISBIND.jcl
             BIND PACKAGE for each program → DLISPKG
             BIND PLAN(DLISPLAN) PKLIST(DLISPKG.*)

Step 5: CICS CSD Install
         DLISCSD.jcl
             STEP010 → DFHCSDUP batch install from DLIS.CSD(DLISCSD)
             STEP020 → INSTALL GROUP(DLISGRP)
```

---

## JCL: DLISDB2 — DB2 DDL execution

**Source:** [`jcl/DLISDB2.jcl`](../jcl/DLISDB2.jcl)

Three sequential steps using DSNTEP2 (DB2 interactive SQL utility):

| Step | DDL member | COND |
|---|---|---|
| STEP010 | DLIS.DDL(DLIS0001) — CREATE TABLESPACES + TABLES | unconditional |
| STEP020 | DLIS.DDL(DLIS0002) — CREATE INDEXES | `COND=(4,LT,STEP010)` |
| STEP030 | DLIS.DDL(DLIS0003) — GRANTS | `COND=(4,LT,STEP020)` |

`COND=(4,LT,stepname)` means "skip this step if a previous step returned RC > 4" — i.e., only run if the previous step was successful.

---

## PROC: DLISBASM — BMS Assembly

**Source:** [`proc/DLISBASM.proc`](../proc/DLISBASM.proc)  
**Parameters:** `MEMBER=`, `SRCLIB='DLIS.BMS'`, `LOADLIB='DLIS.LOADLIB'`

### Steps

| Step | Program | PARM | Output |
|---|---|---|---|
| ASM1 | ASMA90 | `SYSPARM(MAP),DECK,NOOBJECT` | `&&MAPOUT` (physical map copy for CICS) |
| ASM2 | ASMA90 | `SYSPARM(DSECT),DECK,NOOBJECT` | `DLIS.COPYBOOK(&MEMBER)` (symbolic DSECT for COBOL COPY) |
| LKED | IEWL | `RENT,REFR,REUS` | `DLIS.LOADLIB(&MEMBER)` |

ASM2 runs only if ASM1 RC ≤ 7 (`COND=(8,LT,ASM1)`).  
LKED runs only if ASM2 RC ≤ 7 (`COND=(8,LT,ASM2)`).

Syslibs include `CICSTS.SDFHMAC` (CICS macro library) and `SYS1.MACLIB`.

---

## JCL: DLISBMS — BMS Assembly job

**Source:** [`jcl/DLISBMS.jcl`](../jcl/DLISBMS.jcl)

Invokes DLISBASM PROC for each mapset in dependency order:

```
DLISMM → DLISCM → DLISAE → DLISEC → DLISHC → DLISPE → DLISA1 → DLISA2 → DLISLI → DLISST
```

Each step has `COND=(4,LT)` — stops the job if any previous step fails with RC > 4.

---

## PROC: DLISCLNK — COBOL Compile + Link

**Source:** [`proc/DLISCLNK.proc`](../proc/DLISCLNK.proc)  
**Parameters:** `MEMBER=`, `SRCLIB='DLIS.COBOL'`, `COPYLIB='DLIS.COPYBOOK'`, `LOADLIB='DLIS.LOADLIB'`, `DBRMLIB='DLIS.DBRM'`

### Steps

#### Step PC — DB2 Precompile (DSNHPC)

```
Input:  DLIS.COBOL(&MEMBER)
Output: &&DSNHOUT  (COBOL source with SQL replaced by CALL stubs)
        DLIS.DBRM(&MEMBER)  (DB2 Request Module for bind)
PARM:   HOST(COBOL),SOURCE,XREF
```

#### Step COBOL — Enterprise COBOL Compile (IGYCRCTL)

```
Input:   &&DSNHOUT  (from precompile)
SYSLIB:  DLIS.COPYBOOK  (for COPY statements)
Output:  &&LOADSET  (object module)
PARM:    OBJECT,RENT,APOST,LIB,NOSEQ,NOTERM
```
- `RENT` = reentrant code generation (required by CICS).
- `LIB` = enables COPY statements.
- `APOST` = use apostrophe as string delimiter.
- Runs only if PC RC ≤ 7 (`COND=(8,LT,PC)`).

#### Step LKED — Link-Edit (IEWL)

```
Input:   &&LOADSET
Output:  DLIS.LOADLIB(&MEMBER)
PARM:    RENT,REFR,REUS,AMODE(31),RMODE(ANY)
Syslibs: CEE.SCEELKED  (Language Environment)
         DSN.SDSNLOAD  (DB2 runtime)
         CICSTS.SDFHLOAD (CICS stubs)
```
- `AMODE(31)` + `RMODE(ANY)` = 31-bit addressing, relocatable to any storage.
- Runs only if COBOL RC ≤ 7 (`COND=(8,LT,COBOL)`).

---

## JCL: DLISCOMP — COBOL Compile job

**Source:** [`jcl/DLISCOMP.jcl`](../jcl/DLISCOMP.jcl)

Calls DLISCLNK for each COBOL program:

```
DLISMENU → DLISCAND → DLISAPPL → DLISELIG → DLISHIST → DLISPAY
         → DLISAP1 → DLISAP2 → DLISISSU → DLISSTAT
```

All steps after the first use `COND=(4,LT)`.

---

## JCL: DLISBIND — DB2 Bind

**Source:** [`jcl/DLISBIND.jcl`](../jcl/DLISBIND.jcl)

Runs DSNTEP2 via `IKJEFT01` with TSO DSN commands:

### BIND PACKAGE for each program

```
BIND PACKAGE(DLISPKG)
     MEMBER(<program>)
     LIBRARY('DLIS.DBRM')
     ISOLATION(CS)         ← Cursor Stability
     VALIDATE(BIND)        ← validate at bind time
     RELEASE(DEALLOCATE)   ← release resources at deallocate
```

Programs bound: DLISCAND, DLISAPPL, DLISELIG, DLISHIST, DLISPAY, DLISAP1, DLISAP2, DLISISSU, DLISSTAT.

### BIND PLAN

```
BIND PLAN(DLISPLAN)
     PKLIST(DLISPKG.*)    ← all packages in DLISPKG collection
     ISOLATION(CS)
     VALIDATE(BIND)
     RELEASE(DEALLOCATE)
     CURRENTDATA(NO)
```

`DLISPLAN` is referenced by CICS DB2ENTRY `DLISDB2E`.

---

## JCL: DLISCSD — CICS CSD Install

**Source:** [`jcl/DLISCSD.jcl`](../jcl/DLISCSD.jcl)

Two steps:

| Step | Action |
|---|---|
| STEP010 | DFHCSDUP reads `DLIS.CSD(DLISCSD)` and processes DEFINE statements |
| STEP020 | DFHCSDUP runs `INSTALL GROUP(DLISGRP)` to activate all resources |

---

## CSD resource definitions (DLISCSD.csd)

**Source:** [`csd/DLISCSD.csd`](../csd/DLISCSD.csd)

All resources belong to group **DLISGRP** and are listed in transaction list **DLISLIST**.

### Programs defined

| Program | Description | Concurrency |
|---|---|---|
| DLISMENU | Main menu | QUASIRENT |
| DLISCAND | Candidate maintenance | QUASIRENT |
| DLISAPPL | Application entry | QUASIRENT |
| DLISSTAT | Status inquiry | QUASIRENT |
| DLISELIG | Eligibility check | QUASIRENT |
| DLISHIST | History check | QUASIRENT |
| DLISPAY | Payment entry | QUASIRENT |
| DLISAP1 | First approval | QUASIRENT |
| DLISAP2 | Second approval | QUASIRENT |
| DLISISSU | Licence issuance | QUASIRENT |
| DLISRPRT | Receipt print stub | QUASIRENT |
| DLISLPRT | Licence print stub | QUASIRENT |

All programs: `EXECKEY(USER)`, `STATUS(ENABLED)`, `RELOAD(NO)`.

### Mapsets defined

DLISMM · DLISCM · DLISAE · DLISEC · DLISHC · DLISPE · DLISA1 · DLISA2 · DLISLI · DLISST  
All: `STATUS(ENABLED)`, `RESIDENT(NO)`.

### Transactions defined

| Transaction | Program | Timeout | Notes |
|---|---|---|---|
| DL01 | DLISMENU | 10 s | SPURGE(YES) |
| DL02 | DLISCAND | 10 s | SPURGE(YES) |
| DL03 | DLISAPPL | 10 s | SPURGE(YES) |
| DL04 | DLISSTAT | 10 s | SPURGE(YES) |
| DL05 | DLISELIG | 10 s | SPURGE(YES) |
| DL06 | DLISHIST | 10 s | SPURGE(YES) |
| DL07 | DLISPAY  | 10 s | SPURGE(YES) |
| DL08 | DLISAP1  | 30 s | Approval transactions have longer timeout |
| DL09 | DLISAP2  | 30 s | Approval transactions have longer timeout |
| DL10 | DLISISSU | 30 s | Approval transactions have longer timeout |

### DB2ENTRY

```
DEFINE DB2ENTRY(DLISDB2E)
       PLAN(DLISPLAN)
       AUTHTYPE(USERID)
       THREADLIMIT(10)
       THREADWAIT(YES)
       PRIORITY(HIGH)
       ACCOUNTREC(TASK)
       DROLLBACK(NO)
```

- `THREADLIMIT(10)` — maximum 10 concurrent DB2 threads.
- `THREADWAIT(YES)` — tasks wait rather than abend if all threads are busy.
- `DROLLBACK(NO)` — CICS does not automatically roll back on DB2 error (programs handle their own error paths).

---

## Full build and deploy checklist

```
□ 1. Allocate datasets:
       DLIS.COBOL, DLIS.COPYBOOK, DLIS.BMS, DLIS.DBRM,
       DLIS.LOADLIB, DLIS.DDL

□ 2. Upload source members to respective PDSes

□ 3. Run DLISDB2.jcl
       → creates DLISDB database, tablespaces, tables, indexes, grants
       → verify all steps RC=0

□ 4. Run DLISBMS.jcl
       → assembles all 10 BMS mapsets
       → symbolic DSECTs written to DLIS.COPYBOOK
       → verify all steps RC=0

□ 5. Run DLISCOMP.jcl
       → precompiles, compiles, and link-edits all 10 programs
       → DBRMs written to DLIS.DBRM
       → load modules written to DLIS.LOADLIB
       → verify all steps RC ≤ 4 (COBOL informational=4 is acceptable)

□ 6. Run DLISBIND.jcl
       → binds 9 DBRM packages into DLISPKG collection
       → binds DLISPLAN application plan
       → verify bind RC=0

□ 7. Run DLISCSD.jcl
       → installs CICS group DLISGRP (programs, mapsets, transactions, DB2ENTRY)
       → verify DFHCSDUP messages show INSTALL complete with RC=0

□ 8. Add DLIS.LOADLIB to CICS DFHRPL DD concatenation
       (or NEWCOPY all programs if already in DFHRPL)

□ 9. Populate reference data:
       DLIS.AUTHORITY_USER — add level-1 and level-2 approvers
       DLIS.LICENSE_FEE_SCHEDULE — add initial fee rows per licence type

□ 10. Test with transaction DL01 on a 3270 terminal
```

---

## Re-compile and refresh procedure

To recompile a single program (e.g., DLISCAND) after a code change:

```
1. Submit a single-step DLISCOMP job for that member only:
   //DLISCAND EXEC DLISCLNK, MEMBER=DLISCAND

2. If SQL was changed, re-run DLISBIND.jcl for that DBRM member and
   rebind the plan:
   BIND PACKAGE(DLISPKG) MEMBER(DLISCAND) ...
   BIND PLAN(DLISPLAN) PKLIST(DLISPKG.*) ...

3. CICS NEWCOPY or PHASEIN the program:
   CEMT SET PROGRAM(DLISCAND) NEWCOPY
```

To reassemble a BMS map:

```
1. Submit a single-step DLISBMS job for that mapset:
   //DLISCM EXEC DLISBASM, MEMBER=DLISCM

2. CICS NEWCOPY the mapset:
   CEMT SET MAPSET(DLISCM) NEWCOPY
```
