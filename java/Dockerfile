# ===========================================================================
# DLIS — Dockerfile for WAS Liberty on zLinux (IBM z / LinuxONE)
#
# Base image: IBM WebSphere Application Server Liberty on s390x
# Compatible with:
#   - z/OS Container Extensions (zCX) on z16
#   - LinuxONE native Linux on IBM Z
#   - Red Hat OpenShift on IBM Z (OCP/s390x)
# ===========================================================================

# ── Stage 1: Maven build ────────────────────────────────────────────────────
FROM ibmcom/ibm-semeru-runtimes:open-11-jdk-focal AS builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copy POM files first to leverage layer caching
COPY pom.xml .
COPY dlis-core/pom.xml       dlis-core/
COPY dlis-cics-bridge/pom.xml dlis-cics-bridge/
COPY dlis-web/pom.xml        dlis-web/
COPY dlis-ear/pom.xml        dlis-ear/

# Resolve dependencies only (cache layer)
RUN mvn dependency:go-offline -q 2>/dev/null || true

# Copy all source
COPY dlis-core/       dlis-core/
COPY dlis-cics-bridge/ dlis-cics-bridge/
COPY dlis-web/        dlis-web/
COPY dlis-ear/        dlis-ear/

# Build — skip tests in container build; run them in CI pipeline
RUN mvn package -DskipTests -q

# ── Stage 2: WAS Liberty runtime ────────────────────────────────────────────
# Pin to s390x-compatible Liberty image
FROM icr.io/appcafe/websphere-liberty:23.0.0.9-full-java11-openj9-ubi-s390x

# Install DB2 JCC shared library
#   In production, mount /opt/ibm/wlp/usr/shared/resources/db2jcc/db2jcc4.jar
#   from a ConfigMap or PersistentVolume.
#   For this image we copy a local copy if present.
# COPY --chown=1001:0 vendor/db2jcc4.jar /opt/ibm/wlp/usr/shared/resources/db2jcc/db2jcc4.jar

# ── Liberty server config ────────────────────────────────────────────────────
COPY --chown=1001:0 dlis-ear/src/main/liberty/config/server.xml \
     /config/server.xml

# ── Application EAR ─────────────────────────────────────────────────────────
COPY --chown=1001:0 --from=builder \
     /build/dlis-ear/target/dlis.ear \
     /config/apps/dlis.ear

# ── Security: run as non-root ────────────────────────────────────────────────
USER 1001

# ── Environment — override via Kubernetes secrets ───────────────────────────
ENV DB2_HOST=db2.dlis.internal \
    DB2_PORT=50000 \
    DB2_DBNAME=DLISDB \
    DB2_USER=dlisapp \
    DB2_PASSWORD=changeme \
    DLIS_KEYSTORE_PASSWORD=changeme \
    WLP_LOGGING_CONSOLE_FORMAT=JSON \
    WLP_LOGGING_CONSOLE_LOGLEVEL=INFO

EXPOSE 9080 9443

# ── Health check ─────────────────────────────────────────────────────────────
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:9080/dlis/api/v1/health || exit 1
