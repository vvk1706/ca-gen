# DLIS — OpenShift on IBM Z / LinuxONE (zLinux) Deployment Guide (Liberty)

## 1. Overview

This guide explains how to deploy the modernised DLIS Open Liberty microservices on **Red Hat OpenShift 4.x on IBM Z / LinuxONE (s390x architecture)**.

---

## 2. Open Liberty on IBM Z Advantages

1. **Optimised JIT Compiler**: Open Liberty on IBM Semeru Runtime utilizes OpenJ9 which provides aggressive shared class cache and memory footprint reduction on IBM Z architecture.
2. **Co-location with DB2 for z/OS**: Direct HiperSockets network performance when connecting from OpenShift on zLinux to DB2 on z/OS.
3. **Open Liberty Operator**: Simplifies Day-2 operations, autoscaling, server dump collection, and rollout management on OpenShift.

---

## 3. Deployment Steps

```bash
# 1. Log in to the OpenShift cluster
oc login -u <developer> -p <token> --server=https://api.ocp-z.internal:6443

# 2. Apply all Kubernetes/OpenShift resources
oc apply -f deploy/ocp/dlis-all.yaml

# 3. Verify deployment status
oc get pods -n dlis-liberty
oc get routes -n dlis-liberty
```
