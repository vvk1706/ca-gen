# DLIS — Driver License Issuance System

## Overview

DLIS (Driver License Issuance System) is an enterprise Java application that automates the end-to-end workflow for issuing and managing driver licenses for citizens of Queensland, Australia. It is derived from a CA Gen encyclopedia and targets the IBM Z / LinuxONE (zLinux, s390x) platform.

Candidates are Queensland residents. Identity numbers follow the Queensland driver licence number format. Addresses use Australian states and territories, 4-digit postcodes, and the currency for all fees is Australian Dollars (AUD).

The system exposes a JAX-RS REST API and a CICS transaction bridge, both backed by a shared business logic layer (EJB) and a DB2 relational database.

---

## Documentation Index

| Document | Description |
|---|---|
| [README.md](README.md) | This file — project overview and quick-start |
| [architecture.md](architecture.md) | System architecture, module structure, component interactions |
| [business-logic.md](business-logic.md) | Business rules, workflow lifecycle, eligibility rules, approval logic |
| [api-specification.md](api-specification.md) | REST API reference and CICS transaction interface |
| [data-model.md](data-model.md) | DB2 schema, entity descriptions, reference codes |
| [deployment-guide-zlinux.md](deployment-guide-zlinux.md) | Step-by-step deployment guide for zLinux (IBM Z / LinuxONE) |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Runtime | IBM WebSphere Application Server Liberty 23.0.0.9 |
| Java | IBM Semeru OpenJ9 JDK 11 |
| Platform | zLinux (IBM Z s390x) — z/OS Container Extensions or LinuxONE |
| Jakarta EE | Jakarta EE 8 (webProfile-8.0) |
| REST | JAX-RS 2.1 |
| Business Logic | EJB Lite 3.2 (Stateless Session Beans) |
| DI / CDI | CDI 2.0 |
| Database | DB2 for z/OS v12+ or DB2 LUW 11.5, schema `DLIS` |
| CICS | CICS TS 6.x with JCICS 2.0.0 (Liberty JVM Server) |
| Build | Apache Maven 3.x, multi-module POM |
| Container | Docker / OCI image for s390x |
| Transactions | JTA/XA over DB2 JCC Type 4 driver |

---

## Maven Modules

```
java/
├── pom.xml               ← Parent POM (dependency management)
├── dlis-core/            ← Domain model, service interfaces, EJB implementations, DAOs
├── dlis-cics-bridge/     ← CICS JCICS programs and COMMAREA definitions
├── dlis-web/             ← JAX-RS REST resources (WAR)
└── dlis-ear/             ← EAR assembly + Liberty server.xml
```

---

## Quick Start

### Prerequisites

- JDK 11 (IBM Semeru recommended)
- Maven 3.6+
- Docker with s390x support (or `buildx` for cross-platform)
- DB2 instance (LUW 11.5+ or z/OS v12+)
- IBM WebSphere Liberty 23.0.0.9 (for non-container deployment)

### Build

```bash
cd java
./build.sh            # Maven clean package → produces dlis-ear/target/dlis.ear
./build.sh test       # Run unit tests
./build.sh docker     # Build s390x Docker image: dlis:1.0.0
```

### Database Setup

```bash
export DB2_HOST=<host> DB2_PORT=50000 DB2_DBNAME=DLISDB DB2_USER=dlisapp
./build.sh db2-deploy
```

This executes in order:
1. `DLIS-CREATE-TABLES.sql` — creates the `DLIS` schema and all tables
2. `DLIS-CREATE-INDEXES.sql` — creates performance indexes
3. `DLIS-SEED-DATA.sql` — inserts license fee schedule reference data

### CICS Installation (if using CICS bridge)

```bash
./build.sh cics-install
# Then submit DLISCSD.csd via DFHCSDUP on your z/OS LPAR
```

See [deployment-guide-zlinux.md](deployment-guide-zlinux.md) for full details.

---

## Key Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB2_HOST` | `db2.dlis.internal` | DB2 server hostname |
| `DB2_PORT` | `50000` | DB2 TCP port |
| `DB2_DBNAME` | `DLISDB` | Database name |
| `DB2_USER` | `dlisapp` | DB2 user |
| `DB2_PASSWORD` | _(required)_ | DB2 password — supply via Kubernetes Secret |
| `DLIS_KEYSTORE_PASSWORD` | _(required)_ | Liberty keystore password |
| `WLP_LOGGING_CONSOLE_FORMAT` | `JSON` | Liberty log format |
| `WLP_LOGGING_CONSOLE_LOGLEVEL` | `INFO` | Liberty log level |

---

## Security Roles

| Role | Purpose |
|---|---|
| `DLIS_OFFICER` | Licensing officers — create candidates, submit applications, process payments |
| `DLIS_AUTHORITY` | Approving authorities — record first and second-level approvals |
| `DLIS_ADMIN` | System administrators — full access, user management |

---

## Health Check

```
GET http://localhost:9080/dlis/api/v1/health
```

Docker/Kubernetes readiness probe interval: 30 s, timeout: 10 s, start period: 60 s.
