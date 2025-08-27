# Service Configuration & Infrastructure Status Monitoring

## **User Story: Application Health Check**

* **As a** system administrator
* **I want** to check the health of the User Service via a REST endpoint
* **So that** I can integrate it with monitoring tools (Prometheus, Grafana, Kubernetes probes, etc.)

✅ **Acceptance Criteria:**

- [ ] **AC.1:** `/actuator/health` endpoint is enabled.
- [ ] **AC.2:** Returns HTTP `200` and `{ "status": "UP" }` when service is healthy.
- [ ] **AC.3:** If DB is connected, it should response with  `{ "status": "connected", "db": "<CONNECTED_DATABASE_HOST>" }`
- [ ] **AC.4:** Returns HTTP `503` when service is unhealthy.
- [ ] **AC.5:** If DB is not connected, it should response with `{ "status": "disconnected", "db": "<CONNECTED_DATABASE_HOST>" }`

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