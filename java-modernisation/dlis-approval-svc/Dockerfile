# syntax=docker/dockerfile:1
# ──────────────────────────────────────────────────────────────────────────────
# DLIS Approval Service — Multi-stage Dockerfile
# Primary target: linux/s390x  |  Lift-and-shift: linux/amd64
# ──────────────────────────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS build

WORKDIR /build

COPY dlis-common/pom.xml            dlis-common/pom.xml
COPY dlis-common/src                dlis-common/src
COPY dlis-approval-svc/pom.xml      dlis-approval-svc/pom.xml
COPY dlis-approval-svc/src          dlis-approval-svc/src
COPY pom.xml                        pom.xml

RUN mvn -pl dlis-common,dlis-approval-svc -am \
        -DskipTests clean package -q

FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest

ENV LANGUAGE='en_US:en'

COPY --from=build /build/dlis-approval-svc/target/quarkus-app/lib/     /deployments/lib/
COPY --from=build /build/dlis-approval-svc/target/quarkus-app/*.jar    /deployments/
COPY --from=build /build/dlis-approval-svc/target/quarkus-app/app/     /deployments/app/
COPY --from=build /build/dlis-approval-svc/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
