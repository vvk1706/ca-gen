# DLIS — Lift-and-Shift Guide: x86 Cloud Deployment (Liberty)

## 1. Overview

The DLIS Open Liberty microservices are architected to run seamlessly on both **s390x (IBM Z / LinuxONE)** and standard **x86-64 (amd64)** clouds without code changes.

---

## 2. Multi-Arch Container Image Builds

The base image `icr.io/appcafe/open-liberty:full-java17-openj9-ubi` is multi-architecture. You can build multi-platform container images using Docker Buildx:

```bash
docker buildx build --platform linux/s390x,linux/amd64 \
  -f dlis-candidate-svc/Dockerfile \
  -t quay.io/yourorg/dlis/dlis-candidate-svc-liberty:2.0.0 --push .
```

---

## 3. Deploying to Generic Kubernetes (EKS / AKS / GKE / ROSA)

For Kubernetes clusters without OpenShift Route CRDs, simply replace the `Route` definitions with standard Kubernetes `Ingress` objects.
