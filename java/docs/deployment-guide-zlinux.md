# DLIS — Deployment Guide for zLinux (IBM Z / LinuxONE)

This guide covers the complete deployment of DLIS on IBM Z (s390x architecture) using:

- **IBM WebSphere Application Server Liberty** on native zLinux (LinuxONE or z/OS Container Extensions)
- **Red Hat OpenShift on IBM Z** (OCP/s390x)
- **CICS TS Liberty JVM Server** on z/OS (for the CICS transaction bridge)

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Build the Application](#2-build-the-application)
3. [Database Setup (DB2)](#3-database-setup-db2)
4. [Option A — Standalone Liberty on zLinux](#4-option-a--standalone-liberty-on-zlinux)
5. [Option B — Docker / OCI Container on zLinux](#5-option-b--docker--oci-container-on-zlinux)
6. [Option C — Red Hat OpenShift on IBM Z](#6-option-c--red-hat-openshift-on-ibm-z)
7. [CICS Transaction Bridge Setup (z/OS)](#7-cics-transaction-bridge-setup-zos)
8. [Security Configuration](#8-security-configuration)
9. [Verification and Health Check](#9-verification-and-health-check)
10. [Logging and Monitoring](#10-logging-and-monitoring)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Prerequisites

### Development / Build Host

| Requirement | Version | Notes |
|---|---|---|
| JDK | 11 (IBM Semeru recommended) | `java -version` must report s390x or a cross-build host |
| Apache Maven | 3.6+ | `mvn -version` |
| Docker / Podman | 20.10+ | With `buildx` for cross-platform builds on x86 |
| Git | any | — |

### zLinux Target Host

| Requirement | Version | Notes |
|---|---|---|
| OS | RHEL 8/9, SLES 15, or Ubuntu 22 on s390x | LinuxONE or LPAR running Linux on IBM Z |
| IBM Semeru OpenJ9 JDK | 11 | Download from [IBM Developer](https://developer.ibm.com/languages/java/semeru-runtimes/) |
| IBM WebSphere Liberty | 23.0.0.9-full | Install from [WASdev](https://developer.ibm.com/wasdev/) |
| DB2 JCC driver | `db2jcc4.jar` | Obtain from the DB2 installation |
| OpenSSL | 1.1+ | For certificate generation |

### DB2

| Requirement | Notes |
|---|---|
| DB2 for z/OS v12+ or DB2 LUW 11.5 | TCP/IP connectivity on port 50000 (or as configured) |
| Database `DLISDB` | Must exist before running DDL |
| Schema `DLIS` | Created by the DDL script |
| DB2 user `dlisapp` | Must have CREATE TABLE, CREATE INDEX, SELECT, INSERT, UPDATE privileges on schema DLIS |

---

## 2. Build the Application

### 2.1 Build on any host (produces `dlis.ear`)

```bash
cd java
./build.sh          # or: mvn clean package -f pom.xml
```

Artifacts produced:
- `dlis-ear/target/dlis.ear` — enterprise application (deploy this)
- `dlis-cics-bridge/target/dlis-cics-bridge-*.jar` — CICS bridge (for z/OS CICS deployment)

### 2.2 Run unit tests

```bash
./build.sh test     # or: mvn test -f pom.xml
```

Tests use JUnit 5 and Mockito and do not require a running DB2 or Liberty server.

### 2.3 Build the Docker image for s390x

**On a native s390x (zLinux) host:**

```bash
cd java
./build.sh docker
# Produces: dlis:1.0.0
```

**Cross-platform build on x86 host (using Docker buildx):**

```bash
docker buildx create --use
docker buildx build \
  --platform linux/s390x \
  -t dlis:1.0.0 \
  --load \
  java/
```

---

## 3. Database Setup (DB2)

### 3.1 Create the database (if not already present)

```sql
-- Run as DB2 instance owner
CREATE DATABASE DLISDB USING CODESET UTF-8 TERRITORY US;
```

### 3.2 Create the DB2 user

```bash
# On DB2 LUW (Linux): create OS user
useradd -m dlisapp
passwd dlisapp

# Grant database access
db2 "CONNECT TO DLISDB"
db2 "GRANT CONNECT ON DATABASE TO USER dlisapp"
```

For DB2 for z/OS, create the RACF user ID `DLISAPP` and grant CONNECT authority via your site's standard procedure.

### 3.3 Execute the DDL

```bash
export DB2_HOST=db2.your.host
export DB2_PORT=50000
export DB2_DBNAME=DLISDB
export DB2_USER=dlisapp

cd java
./build.sh db2-deploy
```

This runs the three SQL files in order:

```
1. dlis-core/src/main/resources/db2/DLIS-CREATE-TABLES.sql
2. dlis-core/src/main/resources/db2/DLIS-CREATE-INDEXES.sql
3. dlis-core/src/main/resources/db2/DLIS-SEED-DATA.sql
```

Alternatively, run them individually:

```bash
db2 -tvf dlis-core/src/main/resources/db2/DLIS-CREATE-TABLES.sql
db2 -tvf dlis-core/src/main/resources/db2/DLIS-CREATE-INDEXES.sql
db2 -tvf dlis-core/src/main/resources/db2/DLIS-SEED-DATA.sql
```

### 3.4 Verify

```bash
db2 "CONNECT TO DLISDB USER dlisapp USING <password>"
db2 "SELECT TABNAME FROM SYSCAT.TABLES WHERE TABSCHEMA='DLIS'"
```

Expected output: CANDIDATE, LICENSE_APPLICATION, DRIVING_HISTORY, ISSUED_LICENSE, PAYMENT, LICENSE_FEE_SCHEDULE, AUTHORITY_USER.

---

## 4. Option A — Standalone Liberty on zLinux

This option runs Liberty directly on the zLinux host without a container.

### 4.1 Install Liberty

```bash
# Download and extract
cd /opt
unzip wlp-base-all-23.0.0.9.zip
export WLP_HOME=/opt/wlp

# Create the DLIS server
$WLP_HOME/bin/server create dlis
```

### 4.2 Install Liberty features

```bash
$WLP_HOME/bin/installUtility install webProfile-8.0 jaxrs-2.1 cdi-2.0 \
  ejbLite-3.2 transaction-1.2 jdbc-4.2 jsonb-1.0 jsonp-1.1 ssl-1.0 monitor-1.0
```

### 4.3 Configure the server

Copy the server configuration:

```bash
cp java/dlis-ear/src/main/liberty/config/server.xml \
   $WLP_HOME/usr/servers/dlis/server.xml
```

### 4.4 Install the DB2 JCC driver

```bash
mkdir -p $WLP_HOME/usr/shared/resources/db2jcc
cp /path/to/db2jcc4.jar $WLP_HOME/usr/shared/resources/db2jcc/
```

### 4.5 Create the TLS keystore

```bash
# Generate a self-signed certificate (replace with your CA-signed cert for production)
keytool -genkeypair \
  -alias dlis \
  -keyalg RSA -keysize 2048 \
  -validity 365 \
  -keystore $WLP_HOME/usr/servers/dlis/resources/security/dlis-keystore.p12 \
  -storetype PKCS12 \
  -storepass <yourKeystorePassword> \
  -dname "CN=dlis.your.domain, OU=DLIS, O=YourOrg, L=Brisbane, ST=Queensland, C=AU"
```

### 4.6 Set environment variables

Create `$WLP_HOME/usr/servers/dlis/server.env`:

```
DB2_HOST=db2.your.host
DB2_PORT=50000
DB2_DBNAME=DLISDB
DB2_USER=dlisapp
DB2_PASSWORD=<db2password>
DLIS_KEYSTORE_PASSWORD=<yourKeystorePassword>
WLP_LOGGING_CONSOLE_FORMAT=ENHANCED
WLP_LOGGING_CONSOLE_LOGLEVEL=INFO
```

**Important:** Protect this file with restrictive permissions:

```bash
chmod 600 $WLP_HOME/usr/servers/dlis/server.env
```

### 4.7 Deploy the EAR

```bash
cp java/dlis-ear/target/dlis.ear \
   $WLP_HOME/usr/servers/dlis/apps/dlis.ear
```

### 4.8 Start the server

```bash
$WLP_HOME/bin/server start dlis
# Verify startup
tail -f $WLP_HOME/usr/servers/dlis/logs/messages.log
```

Look for: `CWWKZ0001I: Application dlis started`

### 4.9 Create a systemd service (for auto-start)

Create `/etc/systemd/system/dlis-liberty.service`:

```ini
[Unit]
Description=DLIS Liberty Server
After=network.target

[Service]
Type=forking
User=liberty
ExecStart=/opt/wlp/bin/server start dlis
ExecStop=/opt/wlp/bin/server stop dlis
PIDFile=/opt/wlp/usr/servers/dlis/workarea/.pid
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable dlis-liberty
systemctl start dlis-liberty
```

---

## 5. Option B — Docker / OCI Container on zLinux

### 5.1 Transfer the Docker image

Either build directly on the zLinux host (Section 2.3) or push from a build host and pull on target:

```bash
# On build host
./build.sh push     # tags and pushes to registry.dlis.internal

# On zLinux target
docker pull registry.dlis.internal/dlis:1.0.0
```

### 5.2 Create the keystore file

```bash
mkdir -p /opt/dlis/security
keytool -genkeypair \
  -alias dlis \
  -keyalg RSA -keysize 2048 \
  -validity 365 \
  -keystore /opt/dlis/security/dlis-keystore.p12 \
  -storetype PKCS12 \
  -storepass <yourKeystorePassword> \
  -dname "CN=dlis.your.domain, OU=DLIS, O=YourOrg, L=Brisbane, ST=Queensland, C=AU"
```

### 5.3 Mount the DB2 JCC driver

```bash
mkdir -p /opt/dlis/db2jcc
cp /path/to/db2jcc4.jar /opt/dlis/db2jcc/
```

### 5.4 Run the container

```bash
docker run -d \
  --name dlis \
  --platform linux/s390x \
  -p 9080:9080 \
  -p 9443:9443 \
  -e DB2_HOST=db2.your.host \
  -e DB2_PORT=50000 \
  -e DB2_DBNAME=DLISDB \
  -e DB2_USER=dlisapp \
  -e DB2_PASSWORD=<db2password> \
  -e DLIS_KEYSTORE_PASSWORD=<keystorePassword> \
  -e WLP_LOGGING_CONSOLE_FORMAT=JSON \
  -v /opt/dlis/security:/config/resources/security:ro \
  -v /opt/dlis/db2jcc:/opt/ibm/wlp/usr/shared/resources/db2jcc:ro \
  registry.dlis.internal/dlis:1.0.0
```

### 5.5 Verify

```bash
docker logs -f dlis
# Wait for: CWWKZ0001I: Application dlis started
curl -k https://localhost:9443/dlis/api/v1/health
```

---

## 6. Option C — Red Hat OpenShift on IBM Z

### 6.1 Prerequisites

- OCP cluster running on IBM Z s390x workers
- `oc` CLI logged in with cluster-admin or appropriate project roles
- Image accessible from the OCP internal registry or an external registry reachable from the cluster

### 6.2 Create the project

```bash
oc new-project dlis
```

### 6.3 Create Kubernetes Secrets

```bash
# DB2 credentials
oc create secret generic dlis-db2-secret \
  --from-literal=DB2_USER=dlisapp \
  --from-literal=DB2_PASSWORD=<db2password>

# Keystore password
oc create secret generic dlis-keystore-secret \
  --from-literal=DLIS_KEYSTORE_PASSWORD=<keystorePassword>

# DB2 JCC driver (as a secret volume or ConfigMap)
oc create secret generic dlis-db2jcc \
  --from-file=db2jcc4.jar=/path/to/db2jcc4.jar

# TLS keystore
oc create secret generic dlis-keystore \
  --from-file=dlis-keystore.p12=/opt/dlis/security/dlis-keystore.p12
```

### 6.4 Create a Deployment

Save the following as `dlis-deployment.yaml` and apply it:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dlis
  namespace: dlis
spec:
  replicas: 2
  selector:
    matchLabels:
      app: dlis
  template:
    metadata:
      labels:
        app: dlis
    spec:
      nodeSelector:
        kubernetes.io/arch: s390x
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
      containers:
      - name: dlis
        image: registry.dlis.internal/dlis:1.0.0
        ports:
        - containerPort: 9080
          name: http
        - containerPort: 9443
          name: https
        env:
        - name: DB2_HOST
          value: "db2.your.host"
        - name: DB2_PORT
          value: "50000"
        - name: DB2_DBNAME
          value: "DLISDB"
        - name: DB2_USER
          valueFrom:
            secretKeyRef:
              name: dlis-db2-secret
              key: DB2_USER
        - name: DB2_PASSWORD
          valueFrom:
            secretKeyRef:
              name: dlis-db2-secret
              key: DB2_PASSWORD
        - name: DLIS_KEYSTORE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: dlis-keystore-secret
              key: DLIS_KEYSTORE_PASSWORD
        - name: WLP_LOGGING_CONSOLE_FORMAT
          value: "JSON"
        volumeMounts:
        - name: db2jcc
          mountPath: /opt/ibm/wlp/usr/shared/resources/db2jcc
          readOnly: true
        - name: keystore
          mountPath: /config/resources/security
          readOnly: true
        readinessProbe:
          httpGet:
            path: /dlis/api/v1/health
            port: 9080
          initialDelaySeconds: 60
          periodSeconds: 30
        livenessProbe:
          httpGet:
            path: /dlis/api/v1/health
            port: 9080
          initialDelaySeconds: 90
          periodSeconds: 60
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "2000m"
      volumes:
      - name: db2jcc
        secret:
          secretName: dlis-db2jcc
      - name: keystore
        secret:
          secretName: dlis-keystore
```

```bash
oc apply -f dlis-deployment.yaml
```

### 6.5 Create a Service and Route

```bash
oc expose deployment dlis --port=9080 --name=dlis-http
oc create route edge dlis-https \
  --service=dlis-http \
  --port=9080 \
  --hostname=dlis.apps.your.cluster.domain
```

---

## 7. CICS Transaction Bridge Setup (z/OS)

This section applies when using DLIS as CICS transactions on z/OS via the CICS Liberty JVM Server.

### 7.1 Prerequisites

- CICS TS 6.x region
- JCICS 2.0.0 available in the Liberty JVM Server classpath
- DB2 for z/OS subsystem `DLISDB` accessible from the CICS region
- The `dlis-cics-bridge-*.jar` and `dlis-core-*.jar` available on the JVM server classpath

### 7.2 Copy JARs to the JVM Server

Transfer the following JARs to the CICS Liberty JVM Server dropins or library path:

```
dlis-cics-bridge/target/dlis-cics-bridge-1.0.0.jar
dlis-core/target/dlis-core-1.0.0.jar
```

Example (using USS):

```bash
# From your build system, SCP to USS
scp dlis-cics-bridge/target/dlis-cics-bridge-1.0.0.jar \
    user@zos.host:/u/cics/dlis/lib/

scp dlis-core/target/dlis-core-1.0.0.jar \
    user@zos.host:/u/cics/dlis/lib/
```

### 7.3 Configure the JVM Server (DLISLRTY)

The JVM profile `dlis-cics-bridge/src/main/cics/DLISLRTY.jvmprofile` should be placed in the CICS JVM profile dataset. Key parameters:

```
JAVA_HOME=/usr/lpp/java/J11.0_64
WORK_DIR=/u/cics/dlis/work
CLASSPATH_PREFIX=/u/cics/dlis/lib/dlis-cics-bridge-1.0.0.jar
CLASSPATH_PREFIX=/u/cics/dlis/lib/dlis-core-1.0.0.jar
WLP_INSTALL_DIR=/usr/lpp/cicsts/cicsts61/wlp
HEAP_SIZE_INIT=256M
HEAP_SIZE_MAX=1024M
GC_POLICY=gencon
```

### 7.4 Install CICS Resource Definitions (DFHCSDUP)

Create a JCL job to run DFHCSDUP against `DLISCSD.csd`:

```jcl
//DLISDEF  JOB CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//DFHCSDUP EXEC PGM=DFHCSDUP,REGION=0M
//STEPLIB  DD DISP=SHR,DSN=CICSTS61.CICS.SDFHLOAD
//DFHCSD   DD DISP=SHR,DSN=your.cics.DFHCSD
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  DEFINE GROUP(DLISGRP)

  DEFINE JVMSERVER(DLISLRTY) GROUP(DLISGRP)
    DESCRIPTION(DLIS Liberty JVM Server)
    STATUS(ENABLED)
    JVMPROFILE(DLISLRTY)
    THREADLIMIT(20)

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

  DEFINE TRANSACTION(CAND) GROUP(DLISGRP)
    PROGRAM(DLISCAND)
    PROFILE(DFHCICST)
    STATUS(ENABLED)
    TASKDATALOC(ANY)
    ISOLATEST(YES)

  DEFINE TRANSACTION(APPL) GROUP(DLISGRP)
    PROGRAM(DLISAPPL)
    PROFILE(DFHCICST)
    STATUS(ENABLED)
    TASKDATALOC(ANY)
    ISOLATEST(YES)

  DEFINE TRANSACTION(PAAP) GROUP(DLISGRP)
    PROGRAM(DLISPAAP)
    PROFILE(DFHCICST)
    STATUS(ENABLED)
    TASKDATALOC(ANY)
    ISOLATEST(YES)

  DEFINE DB2ENTRY(DLISDB2) GROUP(DLISGRP)
    DB2ID(DLISDB)
    ACCOUNTREC(TASK)
    AUTHTYPE(USERID)
    DROLLBACK(YES)
    PRIORITY(HIGH)

  DEFINE DB2TRAN(CAND) GROUP(DLISGRP)
    TRANSACTION(CAND)
    ENTRY(DLISDB2)

  DEFINE DB2TRAN(APPL) GROUP(DLISGRP)
    TRANSACTION(APPL)
    ENTRY(DLISDB2)

  DEFINE DB2TRAN(PAAP) GROUP(DLISGRP)
    TRANSACTION(PAAP)
    ENTRY(DLISDB2)

  INSTALL GROUP(DLISGRP)
/*
```

Submit this JCL and verify `RC=0` in `SYSPRINT`.

### 7.5 Verify CICS Programs

From a CICS terminal:

```
CEMT INQUIRE PROGRAM(DLISCAND)
CEMT INQUIRE PROGRAM(DLISAPPL)
CEMT INQUIRE PROGRAM(DLISPAAP)
CEMT INQUIRE TRANSACTION(CAND)
```

All programs should show `Enabled` and `Language(Java)`.

---

## 8. Security Configuration

### 8.1 User Registry

Liberty uses the WIM (Federated User Registry) by default. For production, configure an LDAP registry in `server.xml`:

```xml
<ldapRegistry id="ldap"
  host="ldap.your.domain" port="636" ignoreCase="true"
  bindDN="cn=dlisbind,ou=service,dc=your,dc=domain"
  bindPassword="{xor}..."
  baseDN="ou=users,dc=your,dc=domain"
  realm="yourLdapRealm"
  ldapType="IBM Tivoli Directory Server">
</ldapRegistry>
```

Map the LDAP groups to security roles:

| Liberty Security Role | LDAP / WIM Group | Users |
|---|---|---|
| `DLIS_OFFICER` | `DlisOfficers` | Front-desk licensing officers |
| `DLIS_AUTHORITY` | `DlisAuthority` | Approving authority officials |
| `DLIS_ADMIN` | `DlisAdmin` | System administrators |

### 8.2 TLS Certificate

Replace the self-signed certificate with one issued by your internal CA:

```bash
keytool -importkeystore \
  -srckeystore your-ca-signed.p12 \
  -destkeystore $WLP_HOME/usr/servers/dlis/resources/security/dlis-keystore.p12 \
  -srcstoretype PKCS12 \
  -deststoretype PKCS12
```

Update the `keyStore` element in `server.xml` if the alias or store path changes.

### 8.3 DB2 Connection Encryption

The `server.xml` DataSource is configured with `sslConnection="true"`. Ensure the DB2 server certificate is trusted by adding it to the Liberty trust store, or set `sslTrustStoreLocation` and `sslTrustStorePassword` in the JCC properties.

---

## 9. Verification and Health Check

### 9.1 Health endpoint

```bash
curl -k https://<host>:9443/dlis/api/v1/health
# Expected: HTTP 200
```

### 9.2 Smoke test — create a candidate

```bash
curl -k -X POST https://<host>:9443/dlis/api/v1/candidates \
  -u officer1:password \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "dateOfBirth": "1990-01-01",
    "idNumber": "12345678",
    "addressLine1": "1 George Street",
    "city": "Brisbane",
    "stateProvince": "Queensland",
    "postalCode": "4000",
    "country": "AU",
    "createdBy": "SMOKE_TEST"
  }'
```

Expected response: `HTTP 201` with a JSON body containing `"id": <number>`.

### 9.3 Full workflow smoke test (sequential)

```bash
# 1. Create candidate → capture candidateId
# 2. POST /applications       → capture applicationId
# 3. POST /applications/{id}/eligibility-check
# 4. POST /applications/{id}/history-check
# 5. POST /applications/{id}/payment
# 6. POST /applications/{id}/approval1
# 7. POST /applications/{id}/approval2
# 8. POST /applications/{id}/issue
# 9. GET  /applications/{id}  → confirm applicationStatus = "IS"
```

---

## 10. Logging and Monitoring

### Liberty Logs

Default log location: `$WLP_HOME/usr/servers/dlis/logs/`

| File | Contents |
|---|---|
| `messages.log` | Server start/stop, application events, FFDC |
| `console.log` | stdout — mirrors messages.log |
| `trace.log` | Detailed trace (when `traceSpecification` is set) |

The `server.xml` configures trace for DLIS classes:

```xml
<logging
  traceSpecification="com.dlis.*=all:*=info"
  messageFormat="ENHANCED"
  maxFileSize="50"
  maxFiles="5"/>
```

To enable JSON logging (for log aggregation on OpenShift/ELK):

```
WLP_LOGGING_CONSOLE_FORMAT=JSON
```

### Container Logging

In Docker/OCP, all log output goes to stdout in JSON format and is collected by the container runtime log driver.

```bash
# Docker
docker logs -f dlis

# OpenShift
oc logs -f deployment/dlis
```

---

## 11. Troubleshooting

### CWWKZ0013E — Application failed to start

**Cause:** Missing Liberty feature, or `dlis.ear` not found.

**Fix:** Confirm all features are installed (`$WLP_HOME/bin/installUtility install …`) and that `dlis.ear` is in the `apps/` directory.

---

### DSRA0010E — Unable to establish connection — SQLSTATE=08001

**Cause:** DB2 connection cannot be established.

**Checklist:**
1. `DB2_HOST`, `DB2_PORT`, `DB2_DBNAME` environment variables are correct.
2. `db2jcc4.jar` is in `$WLP_HOME/usr/shared/resources/db2jcc/`.
3. DB2 TCP/IP listener is started: `db2 "GET DBM CFG"` → check `SVCENAME`.
4. Firewall allows port 50000 from the Liberty host.
5. SSL: if `sslConnection=true`, ensure DB2 SSL is configured and the certificate is trusted.

---

### CWWKS9104A — Authorization failed

**Cause:** User does not belong to the required security role.

**Fix:** Add the user to the appropriate WIM/LDAP group (`DlisOfficers`, `DlisAuthority`, or `DlisAdmin`).

---

### CICS ASRA / AICA abend (CICS bridge only)

**Cause:** JVM server not started, or class not found.

**Checklist:**
1. `CEMT INQUIRE JVMSERVER(DLISLRTY)` — status should be `Enabled`.
2. Confirm both JARs are on the JVM server classpath.
3. Check `SYSPRINT` in the CICS job output for Java class-loading errors.

---

### COMMAREA length mismatch (CICS bridge)

**Cause:** Calling program is using incorrect COMMAREA size.

**Fix:** Verify the COMMAREA sizes in `DLISCSD.csd`:
- `CAND` (DLISCAND): 396 bytes
- `APPL` (DLISAPPL): 180 bytes
- `PAAP` (DLISPAAP): 544 bytes

---

### High connection pool exhaustion

**Symptom:** `DSRA0080E: An exception was received by the Data Store Adapter`

**Fix:** Increase `maxPoolSize` in `server.xml` (default 20). Monitor DB2 active connections and DLIS transaction rates.

```xml
<connectionManager maxPoolSize="40" minPoolSize="5" .../>
```
