# syntax=docker/dockerfile:1
# ──────────────────────────────────────────────────────────────────────────────
# DLIS Candidate Service — Open Liberty Multi-stage Dockerfile
#
# Primary target:   linux/s390x  (IBM Z / LinuxONE — zLinux)
# Lift-and-shift:   linux/amd64  (x86-64 cloud) — rebuild without changes
#
# Build:
#   docker build -f dlis-candidate-svc/Dockerfile -t dlis-candidate-svc:latest .
# ──────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build WAR ─────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS build

WORKDIR /build

COPY pom.xml pom.xml
COPY dlis-common/ dlis-common/
COPY dlis-candidate-svc/ dlis-candidate-svc/

RUN mvn -pl dlis-common,dlis-candidate-svc -am -DskipTests clean package -q

# ── Stage 2: Runtime Open Liberty ─────────────────────────────────────────────
FROM icr.io/appcafe/open-liberty:full-java17-openj9-ubi

USER root

# Copy DB2 JDBC driver to shared resources directory
COPY --from=build /root/.m2/repository/com/ibm/db2/jcc/11.5.9.0/jcc-11.5.9.0.jar /opt/ol/wlp/usr/shared/resources/db2/jcc.jar

# Copy server configuration and application WAR
COPY dlis-candidate-svc/src/main/liberty/config/server.xml /config/server.xml
COPY --from=build /build/dlis-candidate-svc/target/dlis-candidate-svc.war /config/apps/

# Run configure.sh to apply features and pre-warm runtime
RUN configure.sh

# OpenShift non-root user (1001 is default for Open Liberty container image)
USER 1001

EXPOSE 9080 9443

ENV HTTP_PORT=9080
ENV HTTPS_PORT=9443
