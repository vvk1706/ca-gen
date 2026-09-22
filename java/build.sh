#!/usr/bin/env bash
# ===========================================================================
# DLIS — Build script for zLinux / WAS Liberty
# Usage:
#   ./build.sh              — full Maven build
#   ./build.sh docker       — build Docker image for zLinux (s390x)
#   ./build.sh clean        — clean all target directories
# ===========================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

VERSION="1.0.0"
IMAGE_NAME="dlis"
IMAGE_TAG="${IMAGE_NAME}:${VERSION}"

case "${1:-build}" in

  # ── Maven build ───────────────────────────────────────────────────────────
  build)
    echo "==> Building DLIS Java project (Maven)..."
    mvn clean package -f pom.xml
    echo "==> Build complete: dlis-ear/target/dlis.ear"
    ;;

  # ── Run unit tests ────────────────────────────────────────────────────────
  test)
    echo "==> Running unit tests..."
    mvn test -f pom.xml
    ;;

  # ── Docker build (s390x / zLinux) ─────────────────────────────────────────
  docker)
    echo "==> Building Docker image for zLinux (s390x): ${IMAGE_TAG}"
    # If building on x86 for cross-platform, enable buildx:
    #   docker buildx build --platform linux/s390x -t ${IMAGE_TAG} .
    docker build -t "${IMAGE_TAG}" .
    echo "==> Image built: ${IMAGE_TAG}"
    ;;

  # ── Push image to registry ────────────────────────────────────────────────
  push)
    REGISTRY="${REGISTRY:-registry.dlis.internal}"
    FULL_TAG="${REGISTRY}/${IMAGE_TAG}"
    echo "==> Tagging and pushing ${FULL_TAG}"
    docker tag "${IMAGE_TAG}" "${FULL_TAG}"
    docker push "${FULL_TAG}"
    ;;

  # ── DB2 DDL deploy ────────────────────────────────────────────────────────
  db2-deploy)
    DB2_HOST="${DB2_HOST:?DB2_HOST not set}"
    DB2_PORT="${DB2_PORT:-50000}"
    DB2_DBNAME="${DB2_DBNAME:?DB2_DBNAME not set}"
    DB2_USER="${DB2_USER:?DB2_USER not set}"
    echo "==> Deploying DB2 DDL to ${DB2_HOST}:${DB2_PORT}/${DB2_DBNAME}"
    for f in dlis-core/src/main/resources/db2/DLIS-CREATE-TABLES.sql \
              dlis-core/src/main/resources/db2/DLIS-CREATE-INDEXES.sql \
              dlis-core/src/main/resources/db2/DLIS-SEED-DATA.sql; do
        echo "--- Executing $f ---"
        db2 -tvf "$f" -z "${f%.sql}.log"
    done
    echo "==> DB2 DDL deployment complete."
    ;;

  # ── CICS DFHCSDUP definition install ─────────────────────────────────────
  cics-install)
    echo "==> Installing CICS program definitions (DFHCSDUP)..."
    echo "    Submit dlis-cics-bridge/src/main/cics/DLISCSD.csd via your z/OS DFHCSDUP batch job"
    ;;

  clean)
    echo "==> Cleaning..."
    mvn clean -f pom.xml -q
    ;;

  *)
    echo "Usage: $0 {build|test|docker|push|db2-deploy|cics-install|clean}"
    exit 1
    ;;
esac
