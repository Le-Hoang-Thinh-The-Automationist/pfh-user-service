/*
 *  [USER-STORY] Application Health Check
 *      **As a** system administrator
 *      **I want** to check the health of the User Service via a REST endpoint
 *      **So that** I can integrate it with monitoring tools (Prometheus, Grafana, Kubernetes probes, etc.)
 *
 *      ✅ **Acceptance Criteria:**
 *
 *          * **AC.1:** /actuator/health endpoint is enabled.
 *          * **AC.2:** A service healthy is when the application is up and running, and the DB is connected.
 *          * **AC.3:** Returns HTTP 200 and { "status": "UP" } when service is healthy.
 *          * **AC.4:** If DB is connected, it should respond with { "status": "connected", "db": "<CONNECTED_DATABASE_HOST>" }.
 *          * **AC.5:** Returns HTTP 503 when service is unhealthy.
 *          * **AC.6:** If DB is not connected, it should respond with { "status": "disconnected", "db": "<CONNECTED_DATABASE_HOST>" }.
 *
 */
package com.pfh.user.functionality.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationHealthCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "dbHealthIndicator")
    private HealthIndicator dbHealthIndicator;

    @MockBean(name = "cacheHealthIndicator")
    private HealthIndicator cacheHealthIndicator;

    @MockBean(name = "messageBrokerHealthIndicator")
    private HealthIndicator messageBrokerHealthIndicator;

    @Test
    @DisplayName("[Application Health Check] AC.1: /actuator/health endpoint is enabled")
    // * AC.1: /actuator/health endpoint must be enabled.
    void whenHealthEndpointEnabled_thenReachable() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Application Health Check] AC.2 + AC.3 + AC.4: Service healthy when app UP & DB connected")
    // * AC.2: Service is healthy when app is running AND DB is connected.
    // * AC.3: Returns HTTP 200 and { "status": "UP" } when service is healthy.
    // * AC.4: If DB is connected, it should respond with { "status": "connected", "db": "<CONNECTED_DATABASE_HOST>" }.
    void whenAppUpAndDbConnected_thenHealthUp() throws Exception {
        when(dbHealthIndicator.health())
                .thenReturn(Health.up()
                        .withDetail("status", "connected")
                        .withDetail("db", "postgresql")
                        .build());
        when(cacheHealthIndicator.health()).thenReturn(Health.up().build());
        when(messageBrokerHealthIndicator.health()).thenReturn(Health.up().build());

        mockMvc.perform(get("/actuator/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("connected"))
                .andExpect(jsonPath("$.components.db.details.db").value("postgresql"))
                .andExpect(jsonPath("$.components.cache.status").value("UP"))
                .andExpect(jsonPath("$.components.messageBroker.status").value("UP"));
    }

    @Test
    @DisplayName("[Application Health Check] AC.5 + AC.6: Service unhealthy when DB disconnected")
    // * AC.5: Returns HTTP 503 when service is unhealthy.
    // * AC.6: If DB is not connected, it should respond with { "status": "disconnected", "db": "<CONNECTED_DATABASE_HOST>" }.
    void whenDbDisconnected_thenServiceUnhealthy() throws Exception {
        when(dbHealthIndicator.health())
                .thenReturn(Health.down()
                        .withDetail("status", "disconnected")
                        .withDetail("db", "postgresql")
                        .build());
        when(cacheHealthIndicator.health()).thenReturn(Health.up().build());
        when(messageBrokerHealthIndicator.health()).thenReturn(Health.up().build());

        mockMvc.perform(get("/actuator/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.db.status").value("disconnected"))
                .andExpect(jsonPath("$.components.db.details.db").value("postgresql"));
    }
}
