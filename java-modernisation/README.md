# DLIS — Modernisation Project (Quarkus Microservices)

## Overview

This directory contains the **modernised** version of the DLIS (Driver License Issuance System).
The original monolith is preserved unchanged in [`../java/`](../java/).

The modernisation decomposes the WebSphere Liberty EAR monolith into **four independent
Quarkus microservices**, targeting deployment on **Red Hat OpenShift 4.x on IBM Z / LinuxONE
(s390x / zLinux)**, with full portability to any x86-64 cloud via a container rebuild.

---

## Documentation

| Document | Description |
|---|---|
| [docs/modernisation-strategy.md](docs/modernisation-strategy.md) | Why 4 services, framework selection, migration phases, CICS bridge strategy |
| [docs/microservices-architecture.md](docs/microservices-architecture.md) | Service catalogue, API endpoints, data ownership, inter-service communication |
| [docs/quarkus-migration-guide.md](docs/quarkus-migration-guide.md) | EJB→CDI, javax→jakarta, Liberty→Quarkus migration patterns with code examples |
| [docs/openshift-zlinux.md](docs/openshift-zlinux.md) | Step-by-step OpenShift on IBM Z deployment guide, CI/CD, HPA, Kustomize |
| [docs/lift-and-shift-x86.md](docs/lift-and-shift-x86.md) | Rebuilding for linux/amd64, EKS/AKS/GKE/ROSA deployment, DB2 on Cloud |

---

## Project Structure

```
java-modernisation/
├── pom.xml                          ← Parent POM (Quarkus BOM, dependency management)
│
├── dlis-common/                     ← Shared domain objects and ServiceResult<T>
│   └── src/main/java/com/dlis/common/
│       ├── domain/Candidate.java
│       ├── domain/LicenseApplication.java
│       └── api/ServiceResult.java
│
├── dlis-candidate-svc/              ← Candidate management microservice
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/candidate/
│       │   ├── CandidateResource.java   ← REST: POST/GET /api/v1/candidates
│       │   └── CandidateService.java    ← Business logic (was CandidateServiceEjb)
│       └── resources/application.properties
│
├── dlis-application-svc/            ← Application lifecycle microservice
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/application/
│       │   ├── ApplicationResource.java  ← REST: /api/v1/applications
│       │   ├── ApplicationService.java   ← Business logic (was ApplicationServiceEjb)
│       │   └── CandidateSvcClient.java   ← MicroProfile REST Client → candidate-svc
│       └── resources/application.properties
│
├── dlis-payment-svc/                ← Payment processing microservice
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/payment/
│       │   ├── PaymentResource.java      ← REST: POST /api/v1/applications/{id}/payment
│       │   ├── PaymentService.java       ← Business logic (was PaymentServiceEjb)
│       │   └── ApplicationSvcClient.java ← MicroProfile REST Client → application-svc
│       └── resources/application.properties
│
├── dlis-approval-svc/               ← Approval and license issuance microservice
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dlis/approval/
│       │   ├── ApprovalResource.java     ← REST: /api/v1/applications/{id}/approval1/2/issue
│       │   ├── ApprovalService.java      ← Business logic (was ApprovalServiceEjb)
│       │   └── ApplicationSvcClient.java ← MicroProfile REST Client → application-svc
│       └── resources/application.properties
│
├── deploy/
│   └── ocp/
│       └── dlis-all.yaml            ← All OCP manifests (Namespace, Deployment, Service, Route, ConfigMap, Secret)
│
└── docs/                            ← Comprehensive documentation (see table above)
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Quarkus 3.8.4 LTS |
| Java | IBM Semeru OpenJ9 JRE 17 (UBI 9) |
| Jakarta EE | Jakarta REST 3.1, CDI 4.0 |
| MicroProfile | Health 4.0, OpenAPI 3.1, RestClient 3.0 |
| Database | DB2 LUW 11.5 / DB2 for z/OS v12+ — unchanged schema `DLIS` |
| JDBC Pool | Agroal (bundled with Quarkus) |
| Container | OCI / UBI 9 openjdk-17-runtime (s390x primary, amd64 lift-and-shift) |
| Orchestration | Red Hat OpenShift 4.14+ on IBM Z / LinuxONE |
| Build | Maven 3.9, Quarkus Maven Plugin 3.8.4 |

---

## Microservice Decomposition

| Service | Port | Owns Tables | Monolith EJB |
|---|---|---|---|
| `dlis-candidate-svc` | 8080 | CANDIDATE | CandidateServiceEjb |
| `dlis-application-svc` | 8080 | LICENSE_APPLICATION | ApplicationServiceEjb |
| `dlis-payment-svc` | 8080 | PAYMENT | PaymentServiceEjb |
| `dlis-approval-svc` | 8080 | ISSUED_LICENSE, AUTHORITY_USER (read) | ApprovalServiceEjb |

---

## Quick Start

### Prerequisites

- JDK 17 (IBM Semeru recommended)
- Maven 3.9+
- Docker or Podman (s390x or x86)
- DB2 instance (same as monolith — schema already exists if monolith was deployed)

### Build all services

```bash
cd java-modernisation
mvn clean package -DskipTests
```

### Run in dev mode (hot reload)

```bash
# Start candidate service in Quarkus dev mode
mvn -pl dlis-common,dlis-candidate-svc -am quarkus:dev

# Visit: http://localhost:8080/swagger-ui
# Health: http://localhost:8080/health
```

### Run tests

```bash
mvn test
# Uses H2 in-memory DB in DB2-compat mode — no DB2 required
```

### Build container images (s390x)

```bash
export IMAGE_REGISTRY=quay.io/yourorg

docker build -f dlis-candidate-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-candidate-svc:2.0.0 .
# Repeat for other services
```

### Deploy to OpenShift

```bash
# Create DB2 password secret
oc create secret generic dlis-db2-secret \
  --from-literal=DB2_PASSWORD='your_password' -n dlis

# Deploy everything
oc apply -f deploy/ocp/dlis-all.yaml

# Watch rollout
oc rollout status deployment/dlis-candidate-svc -n dlis
```

See [docs/openshift-zlinux.md](docs/openshift-zlinux.md) for the full deployment guide.

---

## Relationship to Original Application

The original DLIS application in `../java/` is **not modified** by this project.
Both can coexist:

- The monolith continues to serve traffic during the migration period.
- Microservices connect to the **same DB2 schema** (`DLIS`).
- When ready, traffic is cut over to the microservices routes and the monolith is decommissioned.

See [docs/modernisation-strategy.md](docs/modernisation-strategy.md) for the phased migration plan.
