# DLIS — Modernisation Project (Open Liberty Microservices)

## Overview

This directory contains the **re-platformed microservices version** of the DLIS (Driver License Issuance System) built on **Open Liberty** (Jakarta EE 10 / MicroProfile 6.1).
The original monolith is preserved unchanged in [`../java/`](../java/) and the Quarkus modernisation is in [`../java-modernisation/`](../java-modernisation/).

This implementation decomposes the application into **four independent Open Liberty microservices**, packaged as lean WAR containers targeting **Red Hat OpenShift 4.x on IBM Z / LinuxONE (s390x / zLinux)**, with full lift-and-shift portability to any standard x86-64 Kubernetes cloud environment.

---

## Documentation

| Document | Description |
|---|---|
| [docs/modernisation-strategy.md](docs/modernisation-strategy.md) | Modernisation strategy, bounded contexts, Liberty microservices architecture |
| [docs/microservices-architecture.md](docs/microservices-architecture.md) | Service catalogue, endpoints, MicroProfile REST Client topology, data ownership |
| [docs/liberty-replatforming-guide.md](docs/liberty-replatforming-guide.md) | Step-by-step re-platforming guide from Quarkus to Open Liberty (MicroProfile 6.1 / Jakarta EE 10) |
| [docs/openshift-zlinux.md](docs/openshift-zlinux.md) | Step-by-step OpenShift deployment on IBM Z (s390x) and Open Liberty Operator integration |
| [docs/lift-and-shift-x86.md](docs/lift-and-shift-x86.md) | Multi-arch container rebuilds and lift-and-shift to x86 cloud Kubernetes (ROSA, EKS, AKS, GKE) |

---

## Project Structure

```
java-modernisation-liberty/
├── pom.xml                                   ← Parent POM (Liberty Maven Plugin, MicroProfile 6.1 BOM)
├── README.md                                 ← Project overview and quick start
│
├── dlis-common/                              ← Shared domain model & ServiceResult
│   ├── pom.xml
│   └── src/main/java/com/dlis/common/
│       ├── domain/Candidate.java
│       ├── domain/LicenseApplication.java
│       └── api/ServiceResult.java
│
├── dlis-candidate-svc/                       ← Candidate management microservice
│   ├── Dockerfile                            ← Multi-stage Open Liberty UBI image
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/candidate/
│       │   ├── CandidateApplication.java     ← JAX-RS Application root
│       │   ├── CandidateResource.java        ← REST endpoint (/api/v1/candidates)
│       │   ├── CandidateService.java         ← CDI Business bean with JNDI DataSource
│       │   ├── CandidateLivenessCheck.java   ← MP Health @Liveness
│       │   └── CandidateReadinessCheck.java  ← MP Health @Readiness (DB ping)
│       ├── liberty/config/server.xml         ← Liberty server config (DB2 DS, features)
│       └── resources/META-INF/microprofile-config.properties
│
├── dlis-application-svc/                     ← Application lifecycle microservice
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/application/
│       │   ├── ApplicationApp.java           ← JAX-RS Application root
│       │   ├── ApplicationResource.java      ← REST API & callback endpoints
│       │   ├── ApplicationService.java       ← CDI Business bean & JDBC operations
│       │   ├── CandidateSvcClient.java       ← MP RestClient → candidate-svc
│       │   ├── ApplicationLivenessCheck.java ← MP Health @Liveness
│       │   └── ApplicationReadinessCheck.java← MP Health @Readiness
│       ├── liberty/config/server.xml
│       └── resources/META-INF/microprofile-config.properties
│
├── dlis-payment-svc/                         ← Payment processing microservice
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/payment/
│       │   ├── PaymentApplication.java       ← JAX-RS Application root
│       │   ├── PaymentResource.java          ← REST endpoint (/api/v1/applications/{id}/payment)
│       │   ├── PaymentService.java           ← CDI Business bean
│       │   ├── ApplicationSvcClient.java     ← MP RestClient → application-svc
│       │   ├── PaymentLivenessCheck.java     ← MP Health @Liveness
│       │   └── PaymentReadinessCheck.java    ← MP Health @Readiness
│       ├── liberty/config/server.xml
│       └── resources/META-INF/microprofile-config.properties
│
├── dlis-approval-svc/                        ← Approval & issuance microservice
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/approval/
│       │   ├── ApprovalApplication.java      ← JAX-RS Application root
│       │   ├── ApprovalResource.java         ← REST endpoint (/approval1, /approval2, /issue)
│       │   ├── ApprovalService.java          ← CDI Business bean
│       │   ├── ApplicationSvcClient.java     ← MP RestClient → application-svc
│       │   ├── ApprovalLivenessCheck.java    ← MP Health @Liveness
│       │   └── ApprovalReadinessCheck.java   ← MP Health @Readiness
│       ├── liberty/config/server.xml
│       └── resources/META-INF/microprofile-config.properties
│
├── deploy/
│   └── ocp/
│       └── dlis-all.yaml                     ← OpenShift manifests (Deployments, Services, Routes, ConfigMap, Secret)
│
└── docs/                                     ← Comprehensive documentation
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Open Liberty 24.0.0.3 |
| Java | IBM Semeru Runtime OpenJ9 17 (UBI 9) |
| Standards | Jakarta EE 10 Web Profile, MicroProfile 6.1 |
| CDI / DI | Jakarta CDI 4.0 (`@ApplicationScoped`, `@Inject`) |
| REST API | Jakarta REST 3.1 (`@Path`, `@Produces`, `@Consumes`) |
| Inter-Service | Eclipse MicroProfile REST Client 3.0 (`@RegisterRestClient`, `@RestClient`) |
| Health & Metrics | Eclipse MicroProfile Health 4.0 (`/health/live`, `/health/ready`) |
| API Docs | Eclipse MicroProfile OpenAPI 3.1 (`/openapi`, `/api/docs`) |
| Database | IBM DB2 LUW 11.5 / DB2 for z/OS v12+ (unchanged schema `DLIS`) |
| Connection Pool | Liberty native JDBC connection pooling (`jdbc/dlisDS`) |
| Packaging | Lean WAR deployed on Open Liberty container image |
| Base Container | `icr.io/appcafe/open-liberty:full-java17-openj9-ubi` (multi-arch s390x & amd64) |
| Target Platform | Red Hat OpenShift 4.14+ on IBM Z / LinuxONE (s390x) & x86-64 |

---

## Microservice Decomposition

| Microservice | Port | Tables Owned | Primary Business Logic |
|---|---|---|---|
| `dlis-candidate-svc` | 9080 | `DLIS.CANDIDATE` | Candidate registration & ID lookup |
| `dlis-application-svc` | 9080 | `DLIS.LICENSE_APPLICATION`, `DLIS.DRIVING_HISTORY` | Application lifecycle, eligibility & history checks |
| `dlis-payment-svc` | 9080 | `DLIS.PAYMENT`, `DLIS.LICENSE_FEE_SCHEDULE` | Fee computation & payment settlement |
| `dlis-approval-svc` | 9080 | `DLIS.ISSUED_LICENSE`, `DLIS.AUTHORITY_USER` | Dual-level approval & driver license issuance |

---

## Quick Start

### 1. Build all services

```bash
cd java-modernisation-liberty
mvn clean package -DskipTests
```

### 2. Run locally in Liberty dev mode

```bash
mvn -pl dlis-candidate-svc liberty:dev
```

### 3. Build container images

```bash
export IMAGE_REGISTRY=quay.io/yourorg

docker build -f dlis-candidate-svc/Dockerfile -t ${IMAGE_REGISTRY}/dlis/dlis-candidate-svc-liberty:2.0.0 .
docker build -f dlis-application-svc/Dockerfile -t ${IMAGE_REGISTRY}/dlis/dlis-application-svc-liberty:2.0.0 .
docker build -f dlis-payment-svc/Dockerfile -t ${IMAGE_REGISTRY}/dlis/dlis-payment-svc-liberty:2.0.0 .
docker build -f dlis-approval-svc/Dockerfile -t ${IMAGE_REGISTRY}/dlis/dlis-approval-svc-liberty:2.0.0 .
```

### 4. Deploy to OpenShift on IBM Z

```bash
oc apply -f deploy/ocp/dlis-all.yaml
```
