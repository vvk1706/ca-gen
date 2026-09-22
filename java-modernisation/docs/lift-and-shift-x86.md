# DLIS — Lift-and-Shift Guide: x86 Cloud Deployment

## 1. Overview

The DLIS microservices are designed with **zero platform-specific code**. The same
OCI container images that run on IBM Z / LinuxONE (s390x) can be rebuilt and
deployed on any x86-64 cloud environment with no code changes.

This document covers:
- Rebuilding images for `linux/amd64` (x86-64)
- Deploying to generic Kubernetes (EKS, AKS, GKE, IKS, ROSA)
- DB2 compatibility — LUW on x86
- Optional: replacing DB2 with PostgreSQL (Phase 2 path)

---

## 2. What Changes (and What Doesn't)

| Aspect | s390x / zLinux | x86-64 / Cloud | Change needed? |
|---|---|---|---|
| Application code | Same | Same | ✅ None |
| Business logic | Same | Same | ✅ None |
| REST API contract | Same | Same | ✅ None |
| DB2 SQL | Same | Same | ✅ None |
| OpenShift manifests | Same (OCP on Z) | Same (ROSA / OCP on x86) | ✅ None |
| Generic K8s manifests | Remove OpenShift Routes | Use Ingress | Minor YAML |
| Container image | linux/s390x | linux/amd64 | **Rebuild only** |
| Base image | UBI 9 openjdk-17-runtime s390x | UBI 9 openjdk-17-runtime amd64 | Auto-selected by Docker |
| DB2 host | DB2 LUW zLinux or z/OS | DB2 LUW on Linux/x86 or DB2 on Cloud | Config only |
| Node selector | `kubernetes.io/arch: s390x` | `kubernetes.io/arch: amd64` | ConfigMap / Kustomize |

---

## 3. Rebuilding Images for x86-64

The Dockerfiles use `registry.access.redhat.com/ubi9/openjdk-17-runtime:latest`
which is a multi-arch manifest. Docker automatically selects the correct platform layer.

### 3.1 Rebuild on an x86 machine

```bash
cd java-modernisation

# Build for linux/amd64 (default on x86)
export IMAGE_REGISTRY=your-registry.example.com
export IMAGE_TAG=2.0.0-amd64

docker build -f dlis-candidate-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-candidate-svc:${IMAGE_TAG} .
docker build -f dlis-application-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-application-svc:${IMAGE_TAG} .
docker build -f dlis-payment-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-payment-svc:${IMAGE_TAG} .
docker build -f dlis-approval-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-approval-svc:${IMAGE_TAG} .

docker push ${IMAGE_REGISTRY}/dlis/dlis-candidate-svc:${IMAGE_TAG}
docker push ${IMAGE_REGISTRY}/dlis/dlis-application-svc:${IMAGE_TAG}
docker push ${IMAGE_REGISTRY}/dlis/dlis-payment-svc:${IMAGE_TAG}
docker push ${IMAGE_REGISTRY}/dlis/dlis-approval-svc:${IMAGE_TAG}
```

### 3.2 Cross-platform build from any machine using buildx

```bash
# Create multi-arch builder (one-time setup)
docker buildx create --name multiarch --driver docker-container --use
docker buildx inspect --bootstrap

# Build and push both platforms in one command
docker buildx build \
  --platform linux/amd64,linux/s390x \
  -f dlis-candidate-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-candidate-svc:2.0.0 \
  --push .
```

This creates a multi-arch manifest — the same image tag works on both s390x and amd64 clusters.

---

## 4. Kubernetes Ingress (non-OpenShift)

Replace OpenShift `Route` objects with standard Kubernetes `Ingress`. The Deployment and Service objects are identical.

### 4.1 Ingress example (NGINX Ingress Controller)

```yaml
# deploy/k8s/dlis-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: dlis-ingress
  namespace: dlis
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - dlis.example.com
      secretName: dlis-tls-secret
  rules:
    - host: dlis.example.com
      http:
        paths:
          - path: /api/v1/candidates
            pathType: Prefix
            backend:
              service:
                name: dlis-candidate-svc
                port:
                  number: 8080
          - path: /api/v1/applications
            pathType: Prefix
            backend:
              service:
                name: dlis-application-svc
                port:
                  number: 8080
```

**Note**: Payment (`/api/v1/applications/{id}/payment`) and Approval (`/api/v1/applications/{id}/approval1/2`) share the `/api/v1/applications` prefix with the Application service. Route them by path pattern suffix or deploy behind an API Gateway (Kong, AWS API Gateway).

---

## 5. Cloud-Specific Deployment Guides

### 5.1 Amazon EKS (Elastic Kubernetes Service)

```bash
# Authenticate to EKS cluster
aws eks update-kubeconfig --region eu-west-1 --name dlis-cluster

# Create namespace
kubectl create namespace dlis

# Create DB2 secret (DB2 on IBM Cloud or RDS-compatible)
kubectl create secret generic dlis-db2-secret \
  --from-literal=DB2_PASSWORD='your_password' \
  -n dlis

# Update ConfigMap with AWS RDS / DB2 on Cloud endpoint
kubectl create configmap dlis-config \
  --from-literal=DB2_HOST=dlis.xxxxxxxxxxxx.eu-west-1.rds.amazonaws.com \
  --from-literal=DB2_PORT=50000 \
  --from-literal=DB2_DBNAME=DLISDB \
  --from-literal=DB2_USER=dlisapp \
  --from-literal=LOG_JSON=true \
  --from-literal=CANDIDATE_SVC_HOST=dlis-candidate-svc \
  --from-literal=CANDIDATE_SVC_PORT=8080 \
  --from-literal=APPLICATION_SVC_HOST=dlis-application-svc \
  --from-literal=APPLICATION_SVC_PORT=8080 \
  -n dlis

# Apply Deployments and Services (remove nodeSelector/tolerations for s390x)
kubectl apply -f deploy/ocp/dlis-all.yaml

# Apply Ingress
kubectl apply -f deploy/k8s/dlis-ingress.yaml
```

### 5.2 Azure AKS

```bash
az aks get-credentials --resource-group dlis-rg --name dlis-aks

# Steps identical to EKS above
# DB2 host: IBM Db2 on Cloud or DB2 on Azure VM
```

### 5.3 Google GKE

```bash
gcloud container clusters get-credentials dlis-cluster --zone europe-west1-b

# Steps identical to EKS above
# DB2 host: IBM Db2 on Cloud or DB2 on GCE
```

### 5.4 Red Hat OpenShift on AWS (ROSA)

ROSA uses the same OCP API as on-premises. The `deploy/ocp/dlis-all.yaml` manifests
work without modification — Routes are supported natively.

```bash
rosa login
oc apply -f deploy/ocp/dlis-all.yaml
```

### 5.5 IBM Cloud Kubernetes Service (IKS) / ROKS

```bash
ibmcloud ks cluster config --cluster <cluster-id>

# For ROKS (OCP on IBM Cloud): use dlis-all.yaml unchanged
oc apply -f deploy/ocp/dlis-all.yaml

# DB2 on IBM Cloud: use Db2 Flex service
#   DB2_HOST: <instance>.databases.appdomain.cloud
#   DB2_PORT: 50001 (SSL)
```

---

## 6. Node Selector Adjustment for x86

Remove the s390x-specific `nodeSelector` and `tolerations` from `deploy/ocp/dlis-all.yaml`
when deploying to an x86 cluster:

```bash
# Kustomize patch for x86 (create deploy/overlays/x86/kustomization.yaml)
cat > deploy/overlays/x86/remove-nodeselector.yaml << 'EOF'
- op: remove
  path: /spec/template/spec/nodeSelector
- op: remove
  path: /spec/template/spec/tolerations
EOF
```

Or use sed for a quick one-off:
```bash
# Strip nodeSelector and tolerations sections
# (In practice, use Kustomize for repeatable deployments)
sed '/nodeSelector:/,/kubernetes\.io\/arch: s390x/d;/tolerations:/,/effect: "NoSchedule"/d' \
  deploy/ocp/dlis-all.yaml > deploy/k8s/dlis-x86.yaml
```

---

## 7. DB2 Compatibility on x86

### 7.1 DB2 LUW on Linux x86

**IBM DB2 Community Edition** runs on Linux x86 and uses the same JDBC driver (`db2jcc4.jar`) and identical SQL dialect. No schema changes are needed.

```bash
# DB2 LUW 11.5 installation on RHEL/Ubuntu x86
# Then run the same DDL:
cd java
./build.sh db2-deploy   # creates DLIS schema on DB2 LUW x86
```

### 7.2 IBM Db2 on Cloud (DBaaS)

IBM Cloud Db2 Standard Plan provides a DB2 LUW-compatible endpoint accessible over JDBC:
```
DB2_HOST=<instance>.databases.appdomain.cloud
DB2_PORT=50001
DB2_DBNAME=BLUDB
```
Update `dlis-config` ConfigMap accordingly. SSL is required — add `:sslConnection=true;` to the JDBC URL.

### 7.3 Optional: PostgreSQL Migration (Phase 2)

If DB2 is not available in the target cloud, migrate to PostgreSQL. Key SQL differences:

| DB2 SQL | PostgreSQL equivalent |
|---|---|
| `GENERATED ALWAYS AS IDENTITY` | `GENERATED ALWAYS AS IDENTITY` (PostgreSQL 10+) — identical |
| `DECIMAL(10,0)` | `NUMERIC(10,0)` |
| `CHAR(1)` | `CHAR(1)` — identical |
| `FETCH FIRST 1 ROWS ONLY` | `LIMIT 1` |
| `CURRENT DATE` | `CURRENT_DATE` |
| `quarkus.datasource.db-kind=db2` | `quarkus.datasource.db-kind=postgresql` |
| `quarkus-jdbc-db2` extension | `quarkus-jdbc-postgresql` extension |

Changes required: ~20 SQL strings across 4 service files + pom.xml extension swap.
Zero business logic changes.

---

## 8. Environment Variable Reference (all environments)

The following environment variables are read by all four services. Supply them via
Kubernetes `ConfigMap` (non-sensitive) and `Secret` (sensitive).

| Variable | Example (zLinux) | Example (AWS) | Sensitive? |
|---|---|---|---|
| `DB2_HOST` | `db2.dlis.internal` | `dlis.xxx.eu-west-1.rds.amazonaws.com` | No |
| `DB2_PORT` | `50000` | `50000` | No |
| `DB2_DBNAME` | `DLISDB` | `DLISDB` | No |
| `DB2_USER` | `dlisapp` | `dlisapp` | No |
| `DB2_PASSWORD` | _(from Secret)_ | _(from Secret)_ | **Yes** |
| `LOG_JSON` | `true` | `true` | No |
| `CANDIDATE_SVC_HOST` | `dlis-candidate-svc` | `dlis-candidate-svc` | No |
| `CANDIDATE_SVC_PORT` | `8080` | `8080` | No |
| `APPLICATION_SVC_HOST` | `dlis-application-svc` | `dlis-application-svc` | No |
| `APPLICATION_SVC_PORT` | `8080` | `8080` | No |

Service discovery hostnames (`CANDIDATE_SVC_HOST` etc.) resolve via Kubernetes DNS
in any cluster — no change needed across cloud providers.

---

## 9. Lift-and-Shift Validation Checklist

Before declaring lift-and-shift complete, run through this checklist on the x86 environment:

```
[ ] All 4 pods in Running state
[ ] /health/ready returns HTTP 200 for each service
[ ] POST /api/v1/candidates — creates candidate, returns 201
[ ] GET  /api/v1/candidates/{id} — returns created candidate
[ ] POST /api/v1/applications — creates application, returns 201
[ ] POST /api/v1/applications/{id}/eligibility-check — returns status P or F
[ ] POST /api/v1/applications/{id}/history-check — returns status P or F
[ ] POST /api/v1/applications/{id}/payment — returns receipt number
[ ] POST /api/v1/applications/{id}/approval1 — returns approval message
[ ] POST /api/v1/applications/{id}/approval2 — returns approval message
[ ] POST /api/v1/applications/{id}/issue — returns issued license with number
[ ] License number format: LRN/PRB/OPN + vehicleClass + candidateId + applicationId
[ ] GET /openapi — OpenAPI spec loads for each service
```
