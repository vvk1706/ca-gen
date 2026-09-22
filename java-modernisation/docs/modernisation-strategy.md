# DLIS — Modernisation Strategy

## 1. Executive Summary

This document defines the strategy for modernising the DLIS (Driver License Issuance System) monolith from a WebSphere Liberty EAR application into a cloud-native microservices architecture. The target runtime is **Red Hat OpenShift 4.x on IBM Z / LinuxONE (s390x / zLinux)**, with full portability to any x86-64 cloud environment.

The modernisation is a **refactor-and-decompose** approach: the existing Java source code in `java/` is preserved intact and unchanged. New microservices in `java-modernisation/` re-implement the same business logic using modern frameworks, matching the original behaviour 1:1.

---

## 2. Current State Assessment

### 2.1 Monolith Profile

| Dimension          | Current State |
|---|---|
| Packaging          | Java EE EAR (dlis.ear) |
| Runtime            | IBM WebSphere Application Server Liberty 23.0.0.9 |
| Java               | JDK 11 (IBM Semeru OpenJ9) |
| Business Logic     | EJB Stateless Session Beans (EJB Lite 3.2) |
| REST               | JAX-RS 2.1 via dlis-web WAR |
| Database           | DB2 LUW 11.5 / z/OS v12+ — JDBC/JTA XA |
| CICS bridge        | JCICS programs on Liberty JVM Server |
| Build              | Maven multi-module: dlis-core, dlis-cics-bridge, dlis-web, dlis-ear |
| Deployment         | Docker on s390x or direct Liberty deployment on zLinux |

### 2.2 Pain Points

| Problem | Impact |
|---|---|
| Monolithic EAR packaging | Full re-deploy for any change; no independent scaling |
| EJB Lite dependency | Requires full Jakarta EE Web Profile container; slow startup (60–90 s) |
| Single deployment unit | No fault isolation; one bug can crash all functionality |
| CICS tight coupling | Prevents containerised deployment without z/OS co-location |
| Heavy runtime | Liberty EAR image > 400 MB |

---

## 3. Decomposition Strategy

### 3.1 Bounded Context Analysis

The DLIS domain maps directly to four bounded contexts, each with clear data ownership, service interfaces, and CA Gen action block sets:

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Candidate Context                                                        │
│  Data:     CANDIDATE table                                                │
│  Logic:    AB-CREATE-CANDIDATE                                            │
│  Service:  CandidateServiceEjb → dlis-candidate-svc                      │
├──────────────────────────────────────────────────────────────────────────┤
│  Application Lifecycle Context                                            │
│  Data:     LICENSE_APPLICATION, DRIVING_HISTORY, ISSUED_LICENSE (read)   │
│  Logic:    AB-CREATE-APPLICATION, AB-CHECK-ELIGIBILITY, AB-CHECK-HISTORY │
│            AB-INQUIRE-APPLICATION-STATUS                                  │
│  Service:  ApplicationServiceEjb → dlis-application-svc                  │
├──────────────────────────────────────────────────────────────────────────┤
│  Payment Context                                                          │
│  Data:     PAYMENT, LICENSE_FEE_SCHEDULE                                  │
│  Logic:    AB-PROCESS-PAYMENT                                             │
│  Service:  PaymentServiceEjb → dlis-payment-svc                          │
├──────────────────────────────────────────────────────────────────────────┤
│  Approval & Issuance Context                                              │
│  Data:     ISSUED_LICENSE, AUTHORITY_USER                                 │
│  Logic:    AB-RECORD-APPROVAL-1, AB-RECORD-APPROVAL-2, AB-ISSUE-LICENSE  │
│  Service:  ApprovalServiceEjb → dlis-approval-svc                        │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Why 4 Microservices (not more or fewer)?

**Why not 1 service (keep as monolith)?**
The whole point is independent deployment, scaling, and fault isolation. A single service provides no improvement.

**Why not 2–3 services?**
Grouping Payment with Application would create an oversized service with conflicting scaling needs. Payment processing is I/O-bound (payment gateway calls); eligibility checks are compute-bound. Separating them allows independent resource tuning.

**Why not 5+ services (e.g., split Approval from Issuance)?**
Approval and Issuance are tightly coupled in the same workflow step — splitting them would require synchronous inter-service calls for a single user action with no independent scaling benefit. The dual-approval + issue sequence is a single atomic workflow.

**Conclusion: 4 services is optimal** — matches the natural domain boundaries from the original CA Gen encyclopedia, the existing EJB service layer, and the business workflow stages.

### 3.3 Shared Database Strategy

All four services share the same `DLIS` DB2 schema. This is the **shared database pattern** — a pragmatic choice for this modernisation because:

1. The existing DB2 schema is well-designed with clean foreign keys.
2. Each service only writes to its own primary tables (Candidate, LicenseApplication, Payment, IssuedLicense).
3. Cross-service reads are done via REST API calls, not direct SQL joins across service boundaries.
4. Splitting into 4 separate databases in phase 1 would add operational complexity without business benefit.

**Phase 2 path**: Each service can be migrated to a dedicated schema/database as team and operational maturity grows.

---

## 4. Technology Platform Selection

### 4.1 Framework Evaluation

| Framework | s390x support | Startup | Memory | OCP certified | Decision |
|---|---|---|---|---|---|
| **Quarkus 3.x** | ✅ Red Hat certified | ~0.8 s | ~100 MB | ✅ OCP 4.x | **SELECTED** |
| Spring Boot 3.x | ✅ (community) | ~3 s | ~200 MB | ✅ via RHOCP | Viable backup |
| WAS Liberty (current) | ✅ IBM native | ~60 s | ~400 MB | ✅ OCP | Too heavy |
| Micronaut 4 | ✅ (community) | ~0.5 s | ~80 MB | ❌ no RHOCP cert | Risk |
| Helidon 4 | Limited | ~1 s | ~120 MB | ❌ no RHOCP cert | Risk |

**Quarkus** was selected because:
- Red Hat OpenShift Quarkus operator provides SLA-backed support on OCP.
- UBI 9 base images certified for s390x/zLinux from Red Hat registry.
- Uses Jakarta EE 10 APIs (JAX-RS, CDI) — minimal code change from existing javax.ws.rs/CDI.
- MicroProfile 6 provides RestClient, Health, OpenAPI out of the box.
- `quarkus-jdbc-db2` extension wraps IBM DB2 JCC driver natively.
- Same JVM (IBM Semeru OpenJ9 JRE 17 in UBI 9) available as base image.
- Native compilation (GraalVM) available as future optimisation step.

### 4.2 Java Version

**Java 17 LTS** — the current LTS from IBM Semeru on s390x, supported by UBI 9 OpenJDK 17 runtime images. This is a jump from Java 11 (current) but maintains IBM Semeru OpenJ9 JVM continuity.

### 4.3 Base Container Image

```
registry.access.redhat.com/ubi9/openjdk-17-runtime:latest
```

- Universal Base Image 9 — certified s390x.
- Layered on RHEL 9 libraries — matches OCP host kernel compatibility.
- Non-root user (UID 185) — compatible with OpenShift restricted Security Context Constraints (SCC).
- Available in both s390x and amd64 variants — same Dockerfile builds for both platforms.

---

## 5. Migration Approach

### 5.1 Phase 1 — Microservices Refactor (this project)

| Activity | Status |
|---|---|
| Domain analysis and service boundary definition | ✅ Complete |
| Quarkus project scaffolding (4 services + common module) | ✅ Complete |
| Business logic migration (EJB → CDI, javax → jakarta) | ✅ Complete |
| JDBC DAO migration (EJB @Stateless → @ApplicationScoped CDI) | ✅ Complete |
| REST resource migration (JAX-RS 2.1 → Jakarta REST 3.1) | ✅ Complete |
| Inter-service communication (EJB injection → MicroProfile REST Client) | ✅ Complete |
| OpenShift manifests (Deployment, Service, Route, ConfigMap, Secret) | ✅ Complete |
| Multi-stage Dockerfiles (s390x primary, x86 lift-and-shift) | ✅ Complete |
| DB2 schema unchanged — zero migration risk | ✅ No change |

### 5.2 Phase 2 — Hardening (recommended next steps)

- Add JWT/OIDC security via `quarkus-oidc` integrated with Red Hat SSO (Keycloak).
- Replace JDBC direct calls with Panache (Quarkus ActiveRecord ORM) for cleaner repository layer.
- Add distributed tracing via `quarkus-opentelemetry`.
- Add per-service Prometheus metrics via `quarkus-micrometer`.
- Migrate from shared DB schema to per-service schema with cross-service data via events (Kafka on MQ).

### 5.3 Phase 3 — Native Compilation (optional)

Quarkus GraalVM native compilation on s390x requires IBM Semeru Native Image (available from IBM). Native images reduce startup to <50 ms and memory to <50 MB per pod, enabling aggressive horizontal scaling.

---

## 6. CICS Bridge Strategy

The existing `dlis-cics-bridge` module (JCICS programs on Liberty JVM Server) is **not migrated in this phase** for the following reasons:

1. CICS bridge depends on CICS TS + Liberty JVM Server on z/OS — not a container pattern.
2. The REST API microservices provide an equivalent HTTP interface that is accessible from modern CICS LINK-over-IP or CICS Web Services channels.
3. If CICS integration is still required, a thin CICS gateway service can be added that proxies COMMAREA calls to the appropriate microservice REST endpoint.

The existing `java/dlis-cics-bridge` source is preserved and continues to compile against the monolith. It is out of scope for the microservices modernisation.

---

## 7. Documentation Structure (java-modernisation/)

| Document | Purpose |
|---|---|
| `docs/modernisation-strategy.md` | This document |
| `docs/microservices-architecture.md` | Per-service technical specs, API catalogue, data ownership, service mesh topology |
| `docs/quarkus-migration-guide.md` | Detailed EJB→CDI, javax→jakarta, Liberty→Quarkus migration patterns with code examples |
| `docs/openshift-zlinux.md` | Step-by-step OpenShift on zLinux deployment guide |
| `docs/lift-and-shift-x86.md` | Guide for deploying the same containers on x86 cloud (AWS, Azure, GCP, ROKS) |
| `README.md` | Project overview and quick start |
