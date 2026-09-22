# DLIS — Microservices Architecture

## 1. Overview

The modernised DLIS consists of four independent microservices plus a shared domain library:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       External API Clients                                   │
│   (Angular/React front-end, integration partners, API gateway)               │
└────────┬───────────────┬──────────────────┬──────────────────┬──────────────┘
         │               │                  │                  │
         ▼               ▼                  ▼                  ▼
┌──────────────┐ ┌────────────────┐ ┌─────────────┐ ┌───────────────────┐
│  candidate   │ │  application   │ │  payment    │ │     approval      │
│     svc      │ │     svc        │ │    svc      │ │      svc          │
│  :8080       │ │  :8080         │ │  :8080      │ │  :8080            │
│              │ │                │ │             │ │                   │
│ POST /cands  │ │ POST /apps     │ │POST /apps/  │ │POST /apps/{id}/   │
│ GET  /cands/ │ │ GET  /apps/{id}│ │ {id}/pmt   │ │  approval1        │
│ {id}         │ │ POST /apps/{id}│ │             │ │POST /apps/{id}/   │
│ GET  /cands  │ │  /elig-check   │ │             │ │  approval2        │
│ ?idNumber=   │ │ POST /apps/{id}│ │             │ │POST /apps/{id}/   │
│              │ │  /hist-check   │ │             │ │  issue            │
└──────┬───────┘ └───────┬────────┘ └──────┬──────┘ └──────────┬────────┘
       │                 │   ▲ REST call     │  ▲ REST call      │ ▲ REST call
       │                 └───┘               └──┘                └─┘
       │                                                  (to application-svc)
       ▼               ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     DB2 — Schema: DLIS (shared)                              │
│  CANDIDATE  LICENSE_APPLICATION  DRIVING_HISTORY  ISSUED_LICENSE             │
│  PAYMENT    LICENSE_FEE_SCHEDULE  AUTHORITY_USER                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

All services run in the `dlis` OpenShift namespace on IBM Z / LinuxONE (s390x).
Internal communication uses Kubernetes Service DNS (`<svc-name>:8080`).
External access via OpenShift Routes with TLS edge termination.

---

## 2. Service Catalogue

### 2.1 dlis-candidate-svc

| Attribute | Value |
|---|---|
| Maven artifact | `com.dlis:dlis-candidate-svc:2.0.0-SNAPSHOT` |
| Port | 8080 |
| Base path | `/api/v1/candidates` |
| DB2 tables (owned) | `DLIS.CANDIDATE` |
| DB2 tables (read) | — |
| Depends on | — (no upstream service calls) |
| Monolith origin | `CandidateServiceEjb`, `CandidateResource`, `CandidateDaoImpl` |

#### API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/candidates` | Create a new candidate (AB-CREATE-CANDIDATE) |
| GET | `/api/v1/candidates/{id}` | Retrieve candidate by primary key |
| GET | `/api/v1/candidates?idNumber={n}` | Find candidate by national ID number |

#### Request / Response Examples

**POST /api/v1/candidates**
```json
// Request
{
  "firstName":    "Jane",
  "lastName":     "Smith",
  "dateOfBirth":  "1990-05-14",
  "idNumber":     "123456789",
  "addressLine1": "12 Main St",
  "city":         "Brisbane",
  "stateProvince":"QLD",
  "postalCode":   "4000",
  "country":      "Australia",
  "emailAddress": "jane.smith@example.com",
  "createdBy":    "OFFICER1"
}
// Response 201 Created
{ "id": 42, "message": "CANDIDATE CREATED SUCCESSFULLY" }
```

---

### 2.2 dlis-application-svc

| Attribute | Value |
|---|---|
| Maven artifact | `com.dlis:dlis-application-svc:2.0.0-SNAPSHOT` |
| Port | 8080 |
| Base path | `/api/v1/applications` |
| DB2 tables (owned) | `DLIS.LICENSE_APPLICATION` |
| DB2 tables (read) | `DLIS.DRIVING_HISTORY`, `DLIS.ISSUED_LICENSE` |
| Depends on | `dlis-candidate-svc` (REST client for candidate validation) |
| Monolith origin | `ApplicationServiceEjb`, `ApplicationResource`, `ApplicationDaoImpl` |

#### API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/applications` | Create application (AB-CREATE-APPLICATION) |
| GET | `/api/v1/applications/{id}` | Inquire application status (AB-INQUIRE-APPLICATION-STATUS) |
| POST | `/api/v1/applications/{id}/eligibility-check` | Run eligibility check (AB-CHECK-ELIGIBILITY) |
| POST | `/api/v1/applications/{id}/history-check` | Run history check (AB-CHECK-HISTORY) |
| POST | `/api/v1/applications/{id}/payment-status` | Internal: called by payment-svc to advance status |
| POST | `/api/v1/applications/{id}/approval1-status` | Internal: called by approval-svc |
| POST | `/api/v1/applications/{id}/approval2-status` | Internal: called by approval-svc |
| POST | `/api/v1/applications/{id}/status` | Internal: called by approval-svc on license issue |

#### Application Status Lifecycle

```
PE (Pending)
  → EC (Eligibility Checked — passed)
    → HC (History Checked — passed)
      → PP (Payment Approved)
        → A1 (Awaiting 2nd Approval — 1st granted)
          → A2 (Awaiting Issue — both approved)
            → AP (Approved — ready to issue)
              → IS (Issued)
  At any step → RE (Rejected)
```

---

### 2.3 dlis-payment-svc

| Attribute | Value |
|---|---|
| Maven artifact | `com.dlis:dlis-payment-svc:2.0.0-SNAPSHOT` |
| Port | 8080 |
| Base path | `/api/v1/applications` |
| DB2 tables (owned) | `DLIS.PAYMENT` |
| DB2 tables (read) | `DLIS.LICENSE_FEE_SCHEDULE` |
| Depends on | `dlis-application-svc` (read app state + advance payment status) |
| Monolith origin | `PaymentServiceEjb`, `PaymentApprovalResource` (payment portion) |

#### API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/applications/{id}/payment` | Process payment (AB-PROCESS-PAYMENT) |

**Receipt number format:** `RCP<yyyyMMdd><applicationId>` (matches monolith)

---

### 2.4 dlis-approval-svc

| Attribute | Value |
|---|---|
| Maven artifact | `com.dlis:dlis-approval-svc:2.0.0-SNAPSHOT` |
| Port | 8080 |
| Base path | `/api/v1/applications` |
| DB2 tables (owned) | `DLIS.ISSUED_LICENSE` |
| DB2 tables (read) | `DLIS.AUTHORITY_USER` |
| Depends on | `dlis-application-svc` (read app state + advance approval/issue status) |
| Monolith origin | `ApprovalServiceEjb`, `PaymentApprovalResource` (approval portion) |

#### API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/applications/{id}/approval1` | First authority approval (AB-RECORD-APPROVAL-1) |
| POST | `/api/v1/applications/{id}/approval2` | Second authority approval (AB-RECORD-APPROVAL-2) |
| POST | `/api/v1/applications/{id}/issue` | Issue the license (AB-ISSUE-LICENSE) |

**License number format:** `<prefix><vehicleClass><candidateId><applicationId>`
- Prefix: `LRN` (Learner), `PRB` (Probation), `OPN` (Open)
- Validity: L=1yr, P=2yr, O=5yr — matches monolith

---

## 3. dlis-common Module

Shared library included by all four services:

| Package | Contents |
|---|---|
| `com.dlis.common.domain` | `Candidate`, `LicenseApplication` — POJO domain objects |
| `com.dlis.common.api` | `ServiceResult<T>` — return type wrapper |

Additional domain objects (`Payment`, `IssuedLicense`, `DrivingHistory`, `AuthorityUser`, `LicenseFeeSchedule`) are used as local value objects within each service to avoid coupling. The common module is intentionally minimal.

---

## 4. Inter-Service Communication

Services communicate synchronously over HTTP using **MicroProfile RestClient 3.0**:

```
application-svc ←─── candidate-svc       (candidate validation)
payment-svc     ←─── application-svc     (state validation + status update)
approval-svc    ←─── application-svc     (state validation + status update)
```

**Circuit breaking**: Add `quarkus-smallrye-fault-tolerance` in Phase 2 to wrap RestClient calls with `@CircuitBreaker` and `@Retry`.

**Service URLs** (configured via environment variables / ConfigMap):

| Client | Config key | Default |
|---|---|---|
| `CandidateSvcClient` | `quarkus.rest-client.candidate-svc.url` | `http://dlis-candidate-svc:8080` |
| `ApplicationSvcClient` | `quarkus.rest-client.application-svc.url` | `http://dlis-application-svc:8080` |

---

## 5. Health and Observability

Each service exposes:

| Endpoint | Purpose |
|---|---|
| `GET /health/live` | Liveness probe — JVM alive |
| `GET /health/ready` | Readiness probe — DB2 connection pool OK |
| `GET /openapi` | OpenAPI 3.0 specification |
| `GET /swagger-ui` | Interactive Swagger UI |

OpenShift probes are configured in `deploy/ocp/dlis-all.yaml`.

---

## 6. Technology Stack Summary

| Layer | Technology | Version |
|---|---|---|
| Runtime | Quarkus | 3.8.4 LTS |
| Java | IBM Semeru OpenJ9 JRE 17 | UBI 9 |
| Jakarta EE | Jakarta REST 3.1, CDI 4.0 | via Quarkus BOM |
| MicroProfile | Health 4.0, OpenAPI 3.1, RestClient 3.0 | via Quarkus BOM |
| JDBC | Agroal connection pool + IBM DB2 JCC 11.5.9 | |
| Database | DB2 LUW 11.5 / DB2 for z/OS v12+ | Shared schema DLIS |
| Container | OCI / Docker, UBI 9 base | linux/s390x primary |
| Orchestration | Red Hat OpenShift 4.14+ | IBM Z / LinuxONE |
| Build | Apache Maven 3.9, Quarkus Maven Plugin 3.8.4 | |
