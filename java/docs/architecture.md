# DLIS — System Architecture

## 1. Overview

DLIS follows a layered, multi-module Java EE architecture deployed inside an IBM WebSphere Liberty EAR on zLinux (IBM Z s390x). It provides two entry points into the same shared business-logic layer:

- **REST API** — JAX-RS 2.1 JSON interface consumed by external front-ends or integration clients.
- **CICS Transaction Bridge** — JCICS programs invoked as CICS transactions, providing terminal/3270 or inter-system access on z/OS.

```
┌───────────────────────────────────────────────────────────────────────┐
│                        External Clients                                │
│        REST Client (HTTP/HTTPS)      CICS Caller (commarea)           │
└────────────────┬─────────────────────────────┬────────────────────────┘
                 │                             │
                 ▼                             ▼
┌────────────────────────────┐   ┌─────────────────────────────────────┐
│   dlis-web (WAR)           │   │   dlis-cics-bridge (JAR)            │
│   JAX-RS REST Resources    │   │   JCICS Programs                    │
│   ─────────────────────    │   │   ─────────────────────             │
│   CandidateResource        │   │   DlisCandProgram  (CAND txn)       │
│   ApplicationResource      │   │   DlisApplProgram  (APPL txn)       │
│   PaymentApprovalResource  │   │   DlisPayApprProgram (PAAP txn)     │
│   DlisApplication          │   │   CandidateCommarea (396 bytes)     │
│   (base path /api/v1)      │   │   ApplicationCommarea (180 bytes)   │
└────────────┬───────────────┘   └──────────────┬──────────────────────┘
             │                                  │
             └──────────────┬───────────────────┘
                            ▼
┌───────────────────────────────────────────────────────────────────────┐
│   dlis-core (JAR)                                                     │
│   Service Layer (EJB Stateless Session Beans)                         │
│   ─────────────────────────────────────────────────────────────       │
│   ApplicationServiceEjb   — application lifecycle (create/check/inquire)│
│   CandidateServiceEjb     — candidate creation and lookup            │
│   PaymentServiceEjb       — fee calculation and payment recording     │
│   ApprovalServiceEjb      — dual-authority approval and license issue │
│                                                                       │
│   DAO Layer (JDBC / DB2 JCC)                                          │
│   ─────────────────────────────────────────────────────────────       │
│   ApplicationDaoImpl      CandidateDaoImpl                            │
│   SupportingDaoImpls (DrivingHistory, IssuedLicense, LicenseFeeSchedule)│
└────────────────────────────────────┬──────────────────────────────────┘
                                     │
                                     ▼
                    ┌────────────────────────────────┐
                    │   DB2 (z/OS v12+ or LUW 11.5)  │
                    │   Schema: DLIS                  │
                    │   XA DataSource via JCC Type 4  │
                    │   JNDI: jdbc/dlisDS             │
                    └────────────────────────────────┘
```

---

## 2. Maven Module Breakdown

### `dlis-core` (JAR)

The heart of the application. Contains:

| Package | Contents |
|---|---|
| `com.dlis.core.domain` | Seven JPA-free POJO domain objects (Candidate, LicenseApplication, IssuedLicense, Payment, DrivingHistory, LicenseFeeSchedule, AuthorityUser) |
| `com.dlis.core.service` | Four service interfaces (ApplicationService, CandidateService, PaymentService, ApprovalService) plus `ServiceResult<T>` wrapper |
| `com.dlis.core.service.ejb` | EJB Stateless implementations of all four service interfaces |
| `com.dlis.core.dao` | Seven DAO interfaces (one per DB table) |
| `com.dlis.core.dao.jdbc` | JDBC implementations using DB2 JCC prepared statements |

### `dlis-cics-bridge` (JAR)

JCICS bridge programs compiled and loaded into the CICS Liberty JVM Server (`DLISLRTY`).

| Class | CICS Program | Transaction |
|---|---|---|
| `DlisCandProgram` | `DLISCAND` | `CAND` |
| `DlisApplProgram` | `DLISAPPL` | `APPL` |
| `DlisPayApprProgram` | `DLISPAAP` | `PAAP` |

Each program:
1. Obtains the CICS COMMAREA via `task.getCommarea()`.
2. Decodes fixed-length EBCDIC fields using `BaseCommarea` helpers.
3. Dispatches to the shared service EJB.
4. Writes results back to the COMMAREA.

### `dlis-web` (WAR)

Thin JAX-RS layer. Context root: `/dlis`. Base API path: `/api/v1`.

| Resource Class | Path prefix | Operations |
|---|---|---|
| `CandidateResource` | `/candidates` | POST, GET /{id}, GET ?idNumber= |
| `ApplicationResource` | `/applications` | POST, GET /{id}, POST /{id}/eligibility-check, POST /{id}/history-check |
| `PaymentApprovalResource` | `/applications` | POST /{id}/payment, POST /{id}/approval1, POST /{id}/approval2, POST /{id}/issue |

### `dlis-ear` (EAR)

Packages the WAR and both JARs into `dlis.ear`. Bundles `server.xml` for WAS Liberty. Context root `/dlis` is defined in `application.xml`.

---

## 3. Transaction and Session Management

- All service EJBs use `@TransactionAttribute(TransactionAttributeType.REQUIRED)`.
- Read-only inquiry operations use `SUPPORTS` to avoid unnecessary transaction overhead.
- The DataSource is configured as an **XA DataSource** (`javax.sql.XADataSource`) to support distributed transactions between CICS and Liberty.
- Transaction timeout: 120 seconds (configured in `server.xml`).

---

## 4. Security Architecture

Security is enforced at the EAR level through three roles defined in `application.xml` and mapped to LDAP/WIM groups in `server.xml` and `ibm-application-bnd.xml`.

```
Role             Group (WIM Realm)       Access
─────────────    ─────────────────────   ─────────────────────────────────────
DLIS_OFFICER     DlisOfficers            Create candidates, submit applications,
                                          check eligibility/history, process payments
DLIS_AUTHORITY   DlisAuthority           Record first and second approvals
DLIS_ADMIN       DlisAdmin               Full system access
```

SSL is required for the HTTPS endpoint (port 9443). The keystore path is `${server.config.dir}/resources/security/dlis-keystore.p12`. The password is injected via the `DLIS_KEYSTORE_PASSWORD` environment variable.

---

## 5. CICS Integration Architecture

```
z/OS LPAR
┌──────────────────────────────────────────────────────────────┐
│  CICS TS Region                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Liberty JVM Server (DLISLRTY)                          │ │
│  │  ┌────────────────────────────────────────────────────┐ │ │
│  │  │  dlis-cics-bridge.jar + dlis-core.jar              │ │ │
│  │  │                                                    │ │ │
│  │  │  CAND → DlisCandProgram → CandidateServiceEjb      │ │ │
│  │  │  APPL → DlisApplProgram → ApplicationServiceEjb   │ │ │
│  │  │  PAAP → DlisPayApprProgram → PaymentServiceEjb    │ │ │
│  │  │                           → ApprovalServiceEjb    │ │ │
│  │  └────────────────────────────────────────────────────┘ │ │
│  │                                                         │ │
│  │  DB2ENTRY: DLISDB2  (CICS-DB2 Attachment)              │ │
│  └─────────────────────────────────────────────────────────┘ │
│                        │                                     │
│                        ▼                                     │
│             DB2 for z/OS Subsystem (DLISDB)                 │
└──────────────────────────────────────────────────────────────┘
```

CICS resource definitions (`DLISCSD.csd`) define:
- **JVMSERVER** `DLISLRTY` — Liberty JVM server, thread limit 20.
- **PROGRAMs** `DLISCAND`, `DLISAPPL`, `DLISPAAP` — Java JCICS programs.
- **TRANSACTIONs** `CAND`, `APPL`, `PAAP` — linked to above programs.
- **DB2ENTRY** `DLISDB2` — CICS–DB2 attachment (AUTHTYPE=USERID, DROLLBACK=YES).
- **DB2TRAN** entries — map each transaction to the DB2ENTRY.

---

## 6. COMMAREA Layout Summary

### CandidateCommarea — 396 bytes

```
Offset  Len   Field
  0      2    Function code  (CR=Create, UPD=Update, INQ=Inquire)
  2      2    Return code
  4     10    Candidate ID (numeric string)
 14     30    First name
 44     30    Last name
 74      8    Date of birth (YYYYMMDD)
 94     20    ID number
114     50    Address line 1
164     50    Address line 2
214     30    City
244     30    State/Province
274     10    Postal code
284     30    Country
314     15    Phone number
329     60    Email address
389      1    Record status
390      6    (reserved)
```

### ApplicationCommarea — 180 bytes

```
Offset  Len   Field
  0      2    Function code  (CRT, ELG, HST, INQ)
  2      2    Return code
  4     10    Application ID
 14     10    Candidate ID
 24      1    License type  (L/P/O)
 25      8    Application date (YYYYMMDD)
 33      2    Application status
 35      1    Eligibility check status
 36      8    Eligibility check date
 44    200    Eligibility check notes
(and so on for history, payment, approval fields)
```

### PayApprCommarea — 544 bytes

Covers: function code, application/candidate IDs, payment method/reference/amount/receipt, approval decision/notes, vehicle class, license number, expiry date, return code.

---

## 7. Build and CI Pipeline

```
Source → mvn clean package → dlis.ear
                         ↓
              docker buildx build --platform linux/s390x
                         ↓
              Push to registry (registry.dlis.internal)
                         ↓
              Deploy to Kubernetes on OCP/s390x
              or deploy EAR to Liberty standalone on zLinux
```

Unit tests are executed via `mvn test` (JUnit 5 + Mockito) and should run in a CI stage before the Docker build. See [deployment-guide-zlinux.md](deployment-guide-zlinux.md) for the full deployment procedure.
