# Service Configuration & Infrastructure Status Monitoring

## **User Story: Application Health Check**

* **As a** system administrator
* **I want** to check the health of the User Service via a REST endpoint
* **So that** I can integrate it with monitoring tools (Prometheus, Grafana, Kubernetes probes, etc.)

✅ **Acceptance Criteria:**

- [X] **AC.1:** `/actuator/health` endpoint is enabled.
- [X] **AC.2:** A service healthy is when the application is up and running, and the DB is connected.
- [X] **AC.3:** Returns HTTP `200` and `{ "status": "UP" }` when service is healthy.
- [X] **AC.4:** If DB is connected, it should provide information like  `{ "connectionStatus": "connected", "dbHostSource": "<CONNECTED_DATABASE_HOST>" }`
- [X] **AC.5:** Returns HTTP `503` when service is unhealthy.
- [X] **AC.6:** If DB is not connected, it should provide information like `{ "connectionStatus": "disconnected", "dbHostSource": "<CONNECTED_DATABASE_HOST>" }`

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