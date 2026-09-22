# syntax=docker/dockerfile:1
# ──────────────────────────────────────────────────────────────────────────────
# DLIS Candidate Service — Multi-stage Dockerfile
#
# Primary target:   linux/s390x  (IBM Z / LinuxONE — zLinux)
# Lift-and-shift:   linux/amd64  (x86-64 cloud) — rebuild without changes
#
# Build:
#   docker build -t dlis-candidate-svc:latest \
#     --build-arg TARGETPLATFORM=linux/s390x .
#
# Cross-build for x86:
#   docker buildx build --platform linux/amd64 -t dlis-candidate-svc:x86 .
# ──────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS build

WORKDIR /build

# Copy common module and candidate service source
COPY dlis-common/pom.xml                  dlis-common/pom.xml
COPY dlis-common/src                      dlis-common/src
COPY dlis-candidate-svc/pom.xml           dlis-candidate-svc/pom.xml
COPY dlis-candidate-svc/src               dlis-candidate-svc/src
COPY pom.xml                              pom.xml

# Build candidate-svc (skipping other modules)
RUN mvn -pl dlis-common,dlis-candidate-svc -am \
        -DskipTests clean package -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
# UBI 9 OpenJDK 17 runtime — supports both s390x and amd64
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest

ENV LANGUAGE='en_US:en'

# Quarkus fast-jar layout
COPY --from=build /build/dlis-candidate-svc/target/quarkus-app/lib/          /deployments/lib/
COPY --from=build /build/dlis-candidate-svc/target/quarkus-app/*.jar         /deployments/
COPY --from=build /build/dlis-candidate-svc/target/quarkus-app/app/          /deployments/app/
COPY --from=build /build/dlis-candidate-svc/target/quarkus-app/quarkus/      /deployments/quarkus/

EXPOSE 8080

# Non-root user (required for OpenShift restricted SCC)
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
