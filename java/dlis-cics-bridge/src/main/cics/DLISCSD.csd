* ================================================================
* DLIS CICS CSD Resource Definitions — Java Programs
* Install via: DFHCSDUP INSTALL GROUP(DLISGRP)
*
* These definitions register the JCICS Java programs as CICS
* programs within the CICS TS Liberty JVM Server (DLISLRTY).
* ================================================================

DEFINE GROUP(DLISGRP)

* ── JVM Server ────────────────────────────────────────────────────────────
DEFINE JVMSERVER(DLISLRTY) GROUP(DLISGRP)
  DESCRIPTION(DLIS Liberty JVM Server)
  STATUS(ENABLED)
  JVMPROFILE(DFHOSGI)
  THREADLIMIT(20)
  LERUNOPTS(DFHCL2LO)

* ── Programs ────────────────────────────────────────────────────────────────
DEFINE PROGRAM(DLISCAND) GROUP(DLISGRP)
  DESCRIPTION(DLIS Candidate Maintenance - Java)
  LANGUAGE(JAVA)
  JVMCLASS(com.dlis.cics.program.DlisCandProgram)
  JVMSERVER(DLISLRTY)
  STATUS(ENABLED)
  DYNAMIC(YES)

DEFINE PROGRAM(DLISAPPL) GROUP(DLISGRP)
  DESCRIPTION(DLIS Application Entry and Status - Java)
  LANGUAGE(JAVA)
  JVMCLASS(com.dlis.cics.program.DlisApplProgram)
  JVMSERVER(DLISLRTY)
  STATUS(ENABLED)
  DYNAMIC(YES)

DEFINE PROGRAM(DLISPAAP) GROUP(DLISGRP)
  DESCRIPTION(DLIS Payment and Approval - Java)
  LANGUAGE(JAVA)
  JVMCLASS(com.dlis.cics.program.DlisPayApprProgram)
  JVMSERVER(DLISLRTY)
  STATUS(ENABLED)
  DYNAMIC(YES)

* ── Transactions ─────────────────────────────────────────────────────────────
DEFINE TRANSACTION(CAND) GROUP(DLISGRP)
  DESCRIPTION(DLIS Candidate Maintenance)
  PROGRAM(DLISCAND)
  TWASIZE(0)
  PROFILE(DFHCICST)
  STATUS(ENABLED)
  TASKDATALOC(ANY)
  ISOLATEST(YES)

DEFINE TRANSACTION(APPL) GROUP(DLISGRP)
  DESCRIPTION(DLIS Application Entry and Status)
  PROGRAM(DLISAPPL)
  TWASIZE(0)
  PROFILE(DFHCICST)
  STATUS(ENABLED)
  TASKDATALOC(ANY)
  ISOLATEST(YES)

DEFINE TRANSACTION(PAAP) GROUP(DLISGRP)
  DESCRIPTION(DLIS Payment and Approval)
  PROGRAM(DLISPAAP)
  TWASIZE(0)
  PROFILE(DFHCICST)
  STATUS(ENABLED)
  TASKDATALOC(ANY)
  ISOLATEST(YES)

* ── DB2 Entry (CICS-DB2 attachment) ──────────────────────────────────────────
DEFINE DB2ENTRY(DLISDB2) GROUP(DLISGRP)
  DESCRIPTION(DLIS DB2 attachment entry)
  DB2ID(DLISDB)
  ACCOUNTREC(TASK)
  AUTHTYPE(USERID)
  DROLLBACK(YES)
  PRIORITY(HIGH)

* ── DB2TRAN (transaction-to-DB2 mapping) ─────────────────────────────────────
DEFINE DB2TRAN(CAND) GROUP(DLISGRP)
  TRANSACTION(CAND)
  ENTRY(DLISDB2)

DEFINE DB2TRAN(APPL) GROUP(DLISGRP)
  TRANSACTION(APPL)
  ENTRY(DLISDB2)

DEFINE DB2TRAN(PAAP) GROUP(DLISGRP)
  TRANSACTION(PAAP)
  ENTRY(DLISDB2)

* ── Install command ───────────────────────────────────────────────────────────
INSTALL GROUP(DLISGRP)
