# DLIS — OpenShift on IBM Z / LinuxONE (zLinux) Deployment Guide

## 1. Prerequisites

### 1.1 Infrastructure

| Requirement | Minimum | Recommended |
|---|---|---|
| Red Hat OpenShift | 4.14 | 4.15+ |
| IBM Z / LinuxONE | z15 / LinuxONE III | z16 / LinuxONE 4 |
| Architecture | s390x | s390x |
| Worker nodes | 2 × s390x | 3 × s390x |
| Worker RAM | 16 GB | 32 GB |
| Worker CPU | 4 vCPUs | 8 vCPUs |
| DB2 | LUW 11.5 on zLinux or DB2 for z/OS v12+ | DB2 for z/OS v13 |
| Image registry | Any OCI registry accessible from OCP | Quay.io / OCP internal |

### 1.2 Local tooling

```bash
# Install oc CLI (must match OCP cluster version)
curl -LO https://mirror.openshift.com/pub/openshift-v4/s390x/clients/ocp/latest/openshift-client-linux-s390x.tar.gz
tar xzf openshift-client-linux-s390x.tar.gz -C /usr/local/bin

# Verify
oc version

# Maven 3.9+ and Java 17
mvn --version
java -version
```

---

## 2. Build Phase

### 2.1 Build all microservices

```bash
cd java-modernisation
mvn clean package -DskipTests
```

Output: `dlis-{candidate,application,payment,approval}-svc/target/quarkus-app/`

### 2.2 Run unit tests

```bash
mvn test
# Tests use H2 in-memory DB in DB2 compatibility mode — no DB2 required for tests
```

### 2.3 Build container images for s390x

**Option A: Build directly on an s390x machine (recommended)**
```bash
# Set your registry
export IMAGE_REGISTRY=quay.io/yourorg
export IMAGE_TAG=2.0.0

# Build candidate service image
docker build \
  -f dlis-candidate-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-candidate-svc:${IMAGE_TAG} \
  .

# Build remaining services
docker build -f dlis-application-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-application-svc:${IMAGE_TAG} .
docker build -f dlis-payment-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-payment-svc:${IMAGE_TAG} .
docker build -f dlis-approval-svc/Dockerfile \
  -t ${IMAGE_REGISTRY}/dlis/dlis-approval-svc:${IMAGE_TAG} .

# Push all images
docker push ${IMAGE_REGISTRY}/dlis/dlis-candidate-svc:${IMAGE_TAG}
docker push ${IMAGE_REGISTRY}/dlis/dlis-application-svc:${IMAGE_TAG}
docker push ${IMAGE_REGISTRY}/dlis/dlis-payment-svc:${IMAGE_TAG}
docker push ${IMAGE_REGISTRY}/dlis/dlis-approval-svc:${IMAGE_TAG}
```

**Option B: Quarkus Jib build (no local Docker required)**
```bash
export IMAGE_REGISTRY=quay.io/yourorg

# Candidate service (repeat for others)
mvn -pl dlis-common,dlis-candidate-svc -am package \
  -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.registry=${IMAGE_REGISTRY} \
  -Dquarkus.container-image.tag=2.0.0
```

**Option C: OpenShift Source-to-Image (S2I) — build inside OCP**
```bash
# Create a BuildConfig in OCP that fetches from your Git repo
oc new-build --strategy=docker \
  --docker-image=registry.access.redhat.com/ubi9/openjdk-17:latest \
  --name=dlis-candidate-svc \
  https://your-git-repo.internal/dlis.git \
  --context-dir=java-modernisation/dlis-candidate-svc

oc start-build dlis-candidate-svc --follow
```

---

## 3. Database Setup

The DLIS DB2 schema is shared with the monolith. If deploying alongside the monolith, the schema already exists. For a fresh deployment:

```bash
# From the original monolith project
cd java
export DB2_HOST=db2.dlis.internal
export DB2_PORT=50000
export DB2_DBNAME=DLISDB
export DB2_USER=dlisapp
export DB2_PASSWORD=<password>

./build.sh db2-deploy
```

This runs the original SQL scripts:
1. `java/dlis-core/src/main/resources/db2/DLIS-CREATE-TABLES.sql`
2. `java/dlis-core/src/main/resources/db2/DLIS-CREATE-INDEXES.sql`
3. `java/dlis-core/src/main/resources/db2/DLIS-SEED-DATA.sql`

---

## 4. OpenShift Deployment

### 4.1 Login and namespace setup

```bash
oc login https://api.your-ocp-cluster.internal:6443 -u admin

# Namespace is created by dlis-all.yaml if it doesn't exist
# Or create manually:
oc new-project dlis
```

### 4.2 Create DB2 Secret

**Important**: Never commit real passwords to Git.

```bash
oc create secret generic dlis-db2-secret \
  --from-literal=DB2_PASSWORD='your_actual_password' \
  -n dlis

# Verify
oc get secret dlis-db2-secret -n dlis
```

### 4.3 Update image references

Edit `deploy/ocp/dlis-all.yaml` and replace `IMAGE_REGISTRY` with your actual registry:

```bash
export IMAGE_REGISTRY=quay.io/yourorg

sed -i "s|IMAGE_REGISTRY|${IMAGE_REGISTRY}|g" deploy/ocp/dlis-all.yaml
```

Or use a Kustomize overlay (see Section 6).

### 4.4 Apply all manifests

```bash
oc apply -f deploy/ocp/dlis-all.yaml

# Watch rollout
oc rollout status deployment/dlis-candidate-svc -n dlis
oc rollout status deployment/dlis-application-svc -n dlis
oc rollout status deployment/dlis-payment-svc -n dlis
oc rollout status deployment/dlis-approval-svc -n dlis
```

### 4.5 Verify deployments

```bash
# All pods should be Running
oc get pods -n dlis

# Check service endpoints
oc get svc -n dlis

# Get public route URLs
oc get routes -n dlis

# Test health endpoint
CANDIDATE_ROUTE=$(oc get route dlis-candidate-svc -n dlis -o jsonpath='{.spec.host}')
curl https://${CANDIDATE_ROUTE}/health/ready
```

Expected health response:
```json
{
  "status": "UP",
  "checks": [
    { "name": "Database connection health check", "status": "UP" }
  ]
}
```

### 4.6 Test end-to-end API

```bash
CANDIDATE_ROUTE=$(oc get route dlis-candidate-svc -n dlis -o jsonpath='{.spec.host}')
APPLICATION_ROUTE=$(oc get route dlis-application-svc -n dlis -o jsonpath='{.spec.host}')

# 1. Create candidate
curl -s -X POST https://${CANDIDATE_ROUTE}/api/v1/candidates \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"Jane","lastName":"Smith",
    "dateOfBirth":"1990-05-14","idNumber":"123456789",
    "addressLine1":"12 Main St","city":"Brisbane",
    "stateProvince":"QLD","postalCode":"4000","country":"Australia"
  }' | jq .

# 2. Create application (use candidateId from step 1)
curl -s -X POST https://${APPLICATION_ROUTE}/api/v1/applications \
  -H "Content-Type: application/json" \
  -d '{"candidateId": 1, "licenseType":"L", "createdBy":"OFFICER1"}' | jq .
```

---

## 5. OpenShift-Specific Configuration

### 5.1 Security Context Constraints (SCC)

The Dockerfiles use `USER 185` (non-root). OpenShift's default `restricted` SCC
allows non-root containers. No special SCC configuration is needed unless you require
privileged operations.

```bash
# Verify current SCC
oc get pod dlis-candidate-svc-xxxxx -n dlis -o jsonpath='{.metadata.annotations.openshift\.io/scc}'
# Should show: restricted or restricted-v2
```

### 5.2 ImagePullSecret (if using private registry)

```bash
oc create secret docker-registry registry-credentials \
  --docker-server=quay.io \
  --docker-username=youruser \
  --docker-password=yourtoken \
  -n dlis

# Link to default service account
oc secrets link default registry-credentials --for=pull -n dlis
```

### 5.3 Resource Quotas

Apply a namespace-level resource quota to prevent runaway consumption:

```yaml
# deploy/ocp/resourcequota.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: dlis-quota
  namespace: dlis
spec:
  hard:
    requests.cpu: "2"
    requests.memory: "2Gi"
    limits.cpu: "4"
    limits.memory: "4Gi"
    pods: "20"
```

```bash
oc apply -f deploy/ocp/resourcequota.yaml
```

### 5.4 Horizontal Pod Autoscaler (HPA)

```bash
# Scale candidate-svc based on CPU
oc autoscale deployment/dlis-candidate-svc \
  --min=2 --max=6 --cpu-percent=70 \
  -n dlis
```

### 5.5 Node Affinity for s390x

The manifests include `nodeSelector: kubernetes.io/arch: s390x`. In a mixed-arch cluster (s390x + x86 worker nodes), this ensures pods are scheduled only on s390x nodes.

In a dedicated s390x cluster, you can remove the `nodeSelector` and `tolerations` blocks.

---

## 6. Kustomize Overlays

For multi-environment deployment (dev / test / prod) without editing the base YAML:

```
deploy/
├── base/
│   └── dlis-all.yaml       ← the manifest in this project
└── overlays/
    ├── dev/
    │   └── kustomization.yaml
    ├── test/
    │   └── kustomization.yaml
    └── prod/
        └── kustomization.yaml
```

Example `overlays/prod/kustomization.yaml`:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../../base/dlis-all.yaml
patches:
  - patch: |
      - op: replace
        path: /spec/replicas
        value: 4
    target:
      kind: Deployment
      name: dlis-candidate-svc
images:
  - name: IMAGE_REGISTRY/dlis/dlis-candidate-svc
    newName: quay.io/yourorg/dlis/dlis-candidate-svc
    newTag: "2.0.0"
```

Apply with:
```bash
oc apply -k deploy/overlays/prod/
```

---

## 7. CI/CD Pipeline (Tekton on OCP)

Recommended pipeline stages for OpenShift Pipelines (Tekton):

```
git push
  → Pipeline trigger (EventListener / WebHook)
    → Task: mvn test                    (unit tests, H2)
    → Task: mvn package -DskipTests     (build fat-jar)
    → Task: buildah build --platform linux/s390x   (OCI image)
    → Task: trivy scan                  (vulnerability scan)
    → Task: oc apply                    (deploy to test namespace)
    → Task: integration tests           (REST API smoke tests)
    → Manual gate
    → Task: oc apply                    (deploy to prod namespace)
```

---

## 8. Log Management

All services log to stdout in JSON format when `LOG_JSON=true` (set in ConfigMap).

```bash
# Stream logs from all candidate pods
oc logs -l app=dlis-candidate-svc -n dlis -f

# OpenShift Logging (EFK/Loki stack) ingests stdout automatically
# Filter by service: { kubernetes_namespace_name="dlis" } | json | app="dlis-candidate-svc"
```

---

## 9. Troubleshooting

| Symptom | Diagnosis | Fix |
|---|---|---|
| Pods in `CrashLoopBackOff` | Check logs: `oc logs <pod>` | Usually DB2 connectivity; verify Secret DB2_PASSWORD |
| `ImagePullBackOff` | Registry credentials | Create and link `registry-credentials` secret |
| `Readiness probe failed` | DB2 connection pool not ready | Check `DB2_HOST` / `DB2_PORT` in ConfigMap |
| `422 Unprocessable Entity` | Business rule violation | Check JSON payload; read `message` field in response |
| RestClient `Connection refused` | Service discovery | Verify Service names match `CANDIDATE_SVC_HOST` config |
| `java.lang.ClassNotFoundException: com.ibm.db2.jcc.DB2Driver` | Missing DB2 JCC jar | Ensure `quarkus-jdbc-db2` extension is in pom.xml |
