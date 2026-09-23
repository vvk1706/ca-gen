# DLIS — Microservices Architecture (Open Liberty)

## 1. Architectural Overview

The DLIS Open Liberty microservices architecture follows a decentralised, service-oriented model where each microservice runs independently within an Open Liberty container runtime on Red Hat OpenShift.

```
                      ┌────────────────────────────────────────────────────────┐
                      │              OpenShift Ingress / Routes                │
                      └────────┬──────────────┬──────────────┬─────────┬───────┘
                               │              │              │         │
               ┌───────────────▼┐     ┌───────▼────────┐     │         │
               │ candidate-svc  │◄────┤application-svc ├─────┼─────────┤
               │   (Port 9080)  │     │  (Port 9080)   │     │         │
               └───────┬────────┘     └───────┬────────┘     │         │
                       │                      │▲             │         │
                       │                      ││ MP Rest     │ MP Rest │
                       │                      ││ Client      │ Client  │
                       │                      │▼             │         │
                       │              ┌───────┴────────┐     │         │
                       │              │  payment-svc   │◄────┘         │
                       │              │  (Port 9080)   │               │
                       │              └───────┬────────┘               │
                       │                      │                        │
                       │              ┌───────▼────────┐               │
                       │              │  approval-svc  │◄──────────────┘
                       │              │  (Port 9080)   │
                       │              └───────┬────────┘
                       │                      │
                       ▼                      ▼
         ┌─────────────────────────────────────────────────────────────┐
         │              IBM DB2 Database (Schema: DLIS)                │
         └─────────────────────────────────────────────────────────────┘
```

---

## 2. Microservice Specifications

### 2.1 `dlis-candidate-svc`
- **Context Path**: `/`
- **Endpoints**:
  - `POST /api/v1/candidates` — Create candidate
  - `GET /api/v1/candidates/{id}` — Get candidate by ID
  - `GET /api/v1/candidates?idNumber={idNumber}` — Query candidate by national ID
- **Health**: `/health/live`, `/health/ready`

### 2.2 `dlis-application-svc`
- **Context Path**: `/`
- **Endpoints**:
  - `POST /api/v1/applications` — Submit new application
  - `GET /api/v1/applications/{id}` — Inquire application status
  - `POST /api/v1/applications/{id}/eligibility-check` — Verify candidate age & prerequisite license
  - `POST /api/v1/applications/{id}/history-check` — Check driving violations & suspensions
  - `POST /api/v1/applications/{id}/payment-status` — Internal callback for payment settlement
  - `POST /api/v1/applications/{id}/approval1-status` — Internal callback for Level 1 approval
  - `POST /api/v1/applications/{id}/approval2-status` — Internal callback for Level 2 approval
  - `POST /api/v1/applications/{id}/status` — Internal status updater

### 2.3 `dlis-payment-svc`
- **Context Path**: `/`
- **Endpoints**:
  - `POST /api/v1/applications/{id}/payment` — Process application fee payment
- **Outbound Dependencies**: `dlis-application-svc` via MicroProfile REST Client

### 2.4 `dlis-approval-svc`
- **Context Path**: `/`
- **Endpoints**:
  - `POST /api/v1/applications/{id}/approval1` — Level 1 authority approval
  - `POST /api/v1/applications/{id}/approval2` — Level 2 authority approval
  - `POST /api/v1/applications/{id}/issue` — Issue finalized driver license
- **Outbound Dependencies**: `dlis-application-svc` via MicroProfile REST Client
