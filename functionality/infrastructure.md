# Service Configuration & Infrastructure Status Monitoring

## **User Story: Database Connectivity Status**

* **As a** DevOps engineer
* **I want** the User Service to expose an endpoint that checks PostgreSQL connectivity
* **So that** I can verify the application is properly connected to the database

✅ **Acceptance Criteria:**

- [ ] **AC.1:** Endpoint `/db/status` (or equivalent) is available.
- [ ] **AC.2:** Returns HTTP `200` when DB is reachable.
- [ ] **AC.3:** JSON response includes `{ "status": "connected", "db": "postgresql" }`.
- [ ] **AC.4:** Returns HTTP `503` if DB is unreachable.

---

## **User Story: Application Health Check**

* **As a** system administrator
* **I want** to check the health of the User Service via a REST endpoint
* **So that** I can integrate it with monitoring tools (Prometheus, Grafana, Kubernetes probes, etc.)

✅ **Acceptance Criteria:**

- [ ] **AC.1:** `/actuator/health` endpoint is enabled.
- [ ] **AC.2:** Returns HTTP `200` and `{ "status": "UP" }` when service is healthy.
- [ ] **AC.3:** Includes DB, cache, and message broker health indicators (if configured).
- [ ] **AC.4:** Returns HTTP `503` when service is unhealthy.

---

## **User Story: Security Endpoint Availability**

* **As a** security auditor
* **I want** the system to confirm accessibility of key security endpoints
* **So that** I know authentication and authorization mechanisms are active

✅ **Acceptance Criteria:**

- [ ] **AC.1:** `/api/auth/login` endpoint responds with `200` on valid request.
- [ ] **AC.2:** `/api/auth/logout` endpoint responds with `200` on valid request.
- [ ] **AC.3:** Unauthorized requests to protected endpoints return `401 Unauthorized`.

---

## **User Story: Configuration Properties Status**

* **As a** DevOps engineer
* **I want** to verify that application configuration properties are loaded correctly
* **So that** I can ensure proper deployment settings across environments

✅ **Acceptance Criteria:**

- [ ] **AC.1:** `/actuator/env` endpoint is enabled (secured).
- [ ] **AC.2:** Returns application-level configs (masked for sensitive values).
- [ ] **AC.3:** Accessible only by authorized roles (e.g., `ADMIN`).