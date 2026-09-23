# DLIS — Modernisation Strategy (Open Liberty)

## 1. Executive Summary

This document defines the strategy for modernising and re-platforming the DLIS (Driver License Issuance System) into cloud-native microservices running on **Open Liberty** (Jakarta EE 10 / MicroProfile 6.1). The primary target deployment platform is **Red Hat OpenShift 4.x on IBM Z / LinuxONE (s390x / zLinux)**, with seamless lift-and-shift portability to x86-64 cloud environments.

The DLIS microservices maintain 100% functional equivalence with the original CA Gen business logic and the Quarkus modernisation while adopting IBM's premier cloud-native enterprise Java runtime.

---

## 2. Why Open Liberty for DLIS Replatforming?

| Factor | Open Liberty Advantage |
|---|---|
| **IBM Ecosystem Alignment** | Native IBM DB2 optimisation, shared codebase with WebSphere Liberty on z/OS, and premier support on IBM Z / LinuxONE. |
| **Enterprise Standards** | 100% compliant with Jakarta EE 10 Web/Core Profile and Eclipse MicroProfile 6.1. |
| **Operational Consistency** | Uses standard `server.xml` configuration, JNDI DataSources, and MicroProfile Config — aligning development and mainframe operations teams. |
| **Lightweight Footprint** | Dynamic feature loading ensures memory consumption is kept low (~150–250MB RSS per pod) with sub-3s container startup. |
| **OpenShift Operator Support** | Fully supported by the Open Liberty Operator on OpenShift for zero-downtime rolling upgrades and automatic certificate management. |

---

## 3. Microservice Decomposition Boundaries

The decomposition preserves the 4 domain-driven microservices:

1. **`dlis-candidate-svc`** — Candidate profile creation and verification (`DLIS.CANDIDATE`).
2. **`dlis-application-svc`** — License application lifecycle, age/eligibility and driving record history checks (`DLIS.LICENSE_APPLICATION`).
3. **`dlis-payment-svc`** — Fee schedule lookup and payment receipting (`DLIS.PAYMENT`).
4. **`dlis-approval-svc`** — Dual authority approval workflow and driver license number generation (`DLIS.ISSUED_LICENSE`).
