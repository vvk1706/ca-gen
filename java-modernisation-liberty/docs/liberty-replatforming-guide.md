# DLIS — Quarkus to Open Liberty Replatforming Guide

## 1. Overview

This guide details the systematic replatforming of the DLIS microservices from Quarkus 3.x to **Open Liberty (Jakarta EE 10 / MicroProfile 6.1)**.

Both runtimes are modern, cloud-native frameworks capable of running on Red Hat OpenShift on IBM Z (s390x) and x86-64. This migration demonstrates how the application code cleanly transitions between runtimes thanks to Jakarta EE and MicroProfile standardisation.

---

## 2. Key Differences and Mapping

| Area | Quarkus 3.x | Open Liberty 24.x |
|---|---|---|
| **Packaging** | Quarkus Fast-JAR (`target/quarkus-app/`) | Standard Web Archive WAR (`target/*.war`) |
| **Server Configuration** | `application.properties` | `server.xml` + MicroProfile Config |
| **Feature Configuration** | Build-time extensions in `pom.xml` | Dynamic `<featureManager>` in `server.xml` |
| **REST Client Injection** | `@Inject RestClient` | `@Inject @RestClient` (MicroProfile standard) |
| **DataSource Access** | `@Inject DataSource` (Agroal pool) | `@Resource(lookup="jdbc/dlisDS") DataSource` |
| **MicroProfile Client Config** | `quarkus.rest-client.<key>.url=...` | `<key>/mp-rest/url=...` (MP Config standard) |
| **Container Base Image** | `ubi9/openjdk-17-runtime` | `icr.io/appcafe/open-liberty:full-java17-openj9-ubi` |
| **Health Check Provider** | SmallRye Health | Liberty MicroProfile Health feature (`mpHealth-4.0`) |
| **Port Conventions** | `8080` (HTTP) | `9080` (HTTP) / `9443` (HTTPS) |

---

## 3. Code Migration Patterns

### 3.1 REST Client Declaration and Injection

**Quarkus:**
```java
@Inject
CandidateSvcClient candidateSvcClient;
```

**Open Liberty:**
```java
@Inject
@RestClient
CandidateSvcClient candidateSvcClient;
```

### 3.2 DataSource Lookup

**Quarkus (`application.properties`):**
```properties
quarkus.datasource.db-kind=db2
quarkus.datasource.jdbc.url=jdbc:db2://...
```
```java
@Inject
DataSource dataSource;
```

**Open Liberty (`server.xml`):**
```xml
<dataSource id="dlisDS" jndiName="jdbc/dlisDS">
    <jdbcDriver libraryRef="DB2JccLib"/>
    <properties.db2.jcc .../>
</dataSource>
```
```java
@Resource(lookup = "jdbc/dlisDS")
DataSource dataSource;
```

### 3.3 MicroProfile Config for REST Clients

**Quarkus (`application.properties`):**
```properties
quarkus.rest-client.candidate-svc.url=http://dlis-candidate-svc:8080
```

**Open Liberty (`microprofile-config.properties`):**
```properties
candidate-svc/mp-rest/url=http://dlis-candidate-svc:9080
candidate-svc/mp-rest/connectTimeout=5000
candidate-svc/mp-rest/readTimeout=10000
```
