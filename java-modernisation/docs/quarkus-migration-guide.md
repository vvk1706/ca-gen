# DLIS — Quarkus Migration Guide

## 1. Overview

This guide documents every code-level change required to migrate the DLIS monolith
(`java/`) to Quarkus microservices (`java-modernisation/`). It is organised by
migration category, with side-by-side before/after examples.

The migration preserves all business logic exactly. Only the infrastructure
(annotations, transaction management, DI, configuration) changes.

---

## 2. Jakarta EE Namespace Migration (javax → jakarta)

Quarkus 3.x uses **Jakarta EE 10** which renamed all `javax.*` packages to `jakarta.*`.
This is a purely mechanical change — no logic is affected.

| Monolith import | Modernised import |
|---|---|
| `javax.inject.Inject` | `jakarta.inject.Inject` |
| `javax.ws.rs.*` | `jakarta.ws.rs.*` |
| `javax.enterprise.context.*` | `jakarta.enterprise.context.*` |
| `javax.ejb.Stateless` | _(removed — see Section 3)_ |
| `javax.ejb.TransactionAttribute` | _(removed — see Section 4)_ |
| `javax.annotation.Resource` | `jakarta.annotation.Resource` |
| `javax.sql.DataSource` | `javax.sql.DataSource` _(unchanged — java.sql not jakarta)_ |

---

## 3. EJB Stateless → CDI ApplicationScoped

EJB `@Stateless` session beans are replaced by CDI `@ApplicationScoped` beans.
Quarkus CDI is a full CDI 4.0 implementation — all injection points work identically.

**Before (monolith):**
```java
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class CandidateServiceEjb implements CandidateService {
    @Inject
    private CandidateDao candidateDao;
    ...
}
```

**After (Quarkus):**
```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CandidateService {
    @Inject
    DataSource dataSource;   // Direct Agroal DataSource, not DAO layer
    ...
}
```

**Key differences:**
- `@Stateless` → `@ApplicationScoped` (singleton CDI bean, thread-safe via pooling)
- Service interface removed — the CDI bean IS the service. No EJB remote/local interfaces needed.
- `@Inject CandidateDao` → `@Inject DataSource` (DAOs consolidated into service bean for simplicity; can be re-extracted in Phase 2)

---

## 4. Transaction Management

### 4.1 Monolith approach

The monolith uses JTA/XA transactions managed by WAS Liberty:

```java
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ApplicationServiceEjb implements ApplicationService {
    // All methods participate in a JTA transaction
}
```

### 4.2 Quarkus approach

Quarkus uses **Agroal** connection pool with JDBC auto-commit. For write operations:

```java
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CandidateService {

    @Inject
    DataSource dataSource;

    @Transactional   // Quarkus transaction — wraps the method in a JTA transaction
    public ServiceResult<Long> createCandidate(Candidate candidate, String createdBy) {
        ...
    }
}
```

**Note on XA transactions**: The monolith used XA DataSource for CICS–DB2 distributed transactions. In the microservices architecture:
- CICS bridge is removed (see strategy doc).
- Standard (non-XA) JDBC connections are sufficient for single-DB operations.
- If distributed transactions are needed in future (e.g., cross-service saga), use the Saga pattern with compensating transactions instead of XA.

### 4.3 DataSource configuration

**Monolith `server.xml`:**
```xml
<dataSource id="dlisDS" jndiName="jdbc/dlisDS">
  <jdbcDriver libraryRef="db2Lib"/>
  <properties.db2.jcc databaseName="DLISDB" serverName="db2.dlis.internal" portNumber="50000"/>
</dataSource>
```

**Quarkus `application.properties`:**
```properties
quarkus.datasource.db-kind=db2
quarkus.datasource.username=${DB2_USER}
quarkus.datasource.password=${DB2_PASSWORD}
quarkus.datasource.jdbc.url=jdbc:db2://${DB2_HOST}:${DB2_PORT}/${DB2_DBNAME}:currentSchema=DLIS;
```

The `@Resource(lookup = "jdbc/dlisDS")` injection in the monolith is replaced by `@Inject DataSource dataSource`.

---

## 5. DAO Layer Consolidation

The monolith has a separate DAO interface + JDBC implementation per entity. In the microservices, JDBC operations are consolidated into the service bean. This reduces class count while keeping all SQL visible in one place.

**Monolith pattern:**
```
ApplicationService (interface)
  → ApplicationServiceEjb (EJB)
    → ApplicationDao (interface)
      → ApplicationDaoImpl (JDBC impl)
```

**Quarkus pattern:**
```
ApplicationService (CDI bean, owns all SQL for its context)
```

The DAO interfaces (`CandidateDao`, `ApplicationDao`, etc.) from `dlis-common` are not carried forward. All SQL lives directly in the `ApplicationService`, `CandidateService`, etc. CDI beans.

**To restore the DAO layer** (recommended for Phase 2):
1. Extract a `@ApplicationScoped CandidateRepository` class.
2. Move SQL methods from `CandidateService` to `CandidateRepository`.
3. `@Inject CandidateRepository` in `CandidateService`.
4. Optionally migrate to Quarkus Panache for zero-boilerplate repository pattern.

---

## 6. REST Resource Migration

REST resources migrate from `javax.ws.rs` to `jakarta.ws.rs`. The annotations are identical — only the package changes.

**Monolith:**
```java
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.inject.Inject;

@Path("/applications")
public class ApplicationResource {
    @Inject
    private ApplicationService applicationService;
```

**Quarkus:**
```java
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;

@Path("/api/v1/applications")
public class ApplicationResource {
    @Inject
    ApplicationService applicationService;
```

**Path change**: The monolith uses a `DlisApplication` class to set `/api/v1` as the base path. In Quarkus, the full path is placed directly in the `@Path` annotation on each resource. `quarkus.http.root-path=/` is configured in `application.properties`.

---

## 7. Configuration Migration

**Monolith** uses `server.xml` and environment variables:
```xml
<!-- server.xml -->
<variable name="DB2_HOST" defaultValue="db2.dlis.internal"/>
<variable name="DB2_PORT" defaultValue="50000"/>
```

**Quarkus** uses `application.properties` with MicroProfile Config:
```properties
# Resolved from environment variable, falling back to default
quarkus.datasource.jdbc.url=jdbc:db2://${DB2_HOST:db2.dlis.internal}:${DB2_PORT:50000}/...
```

All environment variables remain the same names — no changes needed in OpenShift ConfigMaps or Secrets.

---

## 8. Inter-Service Communication (EJB → MicroProfile RestClient)

In the monolith, services call each other via direct EJB `@Inject`:
```java
// Monolith: ApplicationServiceEjb injects CandidateDao directly
@Inject private CandidateDao candidateDao;
Optional<Candidate> cand = candidateDao.findById(app.getCandidateId());
```

In the microservices, cross-service calls use MicroProfile RestClient:
```java
// Quarkus: application-svc calls candidate-svc over HTTP
@Inject
@RestClient
CandidateSvcClient candidateSvcClient;

Candidate cand = candidateSvcClient.findById(app.getCandidateId());
```

The RestClient interface mirrors the REST API of the target service:
```java
@RegisterRestClient(configKey = "candidate-svc")
@Path("/api/v1/candidates")
public interface CandidateSvcClient {
    @GET @Path("/{id}")
    Candidate findById(@PathParam("id") Long id);
}
```

---

## 9. Health Endpoint

**Monolith**: Custom `/api/v1/health` endpoint in `DlisApplication`.

**Quarkus**: Automatic via `quarkus-smallrye-health`:
- `/health/live` — liveness (JVM alive)
- `/health/ready` — readiness (DB connection pool OK)
- `/health` — combined

Configure in OpenShift readiness/liveness probes.

---

## 10. Maven Build Migration

**Monolith `pom.xml` parent:**
```xml
<packaging>pom</packaging>
<modules>dlis-core, dlis-cics-bridge, dlis-web, dlis-ear</modules>
```

**Quarkus `pom.xml` parent:**
```xml
<packaging>pom</packaging>
<modules>dlis-common, dlis-candidate-svc, dlis-application-svc, dlis-payment-svc, dlis-approval-svc</modules>
```

**Build command comparison:**

| Monolith | Quarkus |
|---|---|
| `mvn clean package` → `dlis.ear` | `mvn clean package` → 4 × `quarkus-run.jar` |
| `./build.sh docker` → 1 image | `mvn -pl dlis-candidate-svc package -Dquarkus.container-image.build=true` → 1 image per service |
| `./build.sh test` | `mvn test` |

**Quarkus dev mode** (hot reload):
```bash
cd java-modernisation
mvn -pl dlis-candidate-svc quarkus:dev
# Visit http://localhost:8080/swagger-ui for interactive API
```

---

## 11. Migration Checklist

| Item | Done |
|---|---|
| `javax.*` → `jakarta.*` imports | ✅ |
| `@Stateless` removed, `@ApplicationScoped` added | ✅ |
| `@TransactionAttribute` removed, `@Transactional` available | ✅ |
| `@Resource(lookup="jdbc/dlisDS")` → `@Inject DataSource` | ✅ |
| `implements ApplicationService` removed (no EJB interface) | ✅ |
| `@Path` includes full path `/api/v1/...` | ✅ |
| `DlisApplication` (JAX-RS Application class) removed | ✅ |
| Business logic preserved byte-for-byte | ✅ |
| DB2 SQL unchanged | ✅ |
| JSON field names unchanged | ✅ |
| HTTP status codes unchanged | ✅ |
| Error response format unchanged | ✅ |
