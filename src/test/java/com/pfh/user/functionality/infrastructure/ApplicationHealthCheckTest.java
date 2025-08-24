/*
 *  [USER-STORY] Application Health Check
 *      **As a** system administrator
 *      **I want** the User Service to check the health of the application via a REST endpoint
 *      **So that** I can integrate it with monitoring tools (Prometheus, Grafana, Kubernetes probes, etc.)
 *
 *      ✅ **Acceptance Criteria:**
 *
 *          * **AC.1:** /actuator/health endpoint is enabled.
 *          * **AC.2:** Returns HTTP 200 and { "status": "UP" } when service is healthy.
 *          * **AC.3:** Includes DB, cache, and message broker health indicators (if configured).
 *          * **AC.4:** Returns HTTP 503 when service is unhealthy.
 *
 */
package com.pfh.user.functionality.infrastructure;

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
    // * AC.1: /actuator/health endpoint is enabled.
    // * AC.2: Returns HTTP 200 and { "status": "UP" } when service is healthy.
    // * AC.3: Includes DB, cache, and message broker health indicators (if configured).
    void whenServiceHealthy_thenStatus200AndUpJson() throws Exception {
        when(dbHealthIndicator.health()).thenReturn(Health.up().build());
        when(cacheHealthIndicator.health()).thenReturn(Health.up().build());
        when(messageBrokerHealthIndicator.health()).thenReturn(Health.up().build());

        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.db.status").value("UP"))
            .andExpect(jsonPath("$.components.cache.status").value("UP"))
            .andExpect(jsonPath("$.components.messageBroker.status").value("UP"));
    }

    @Test
    // * AC.1: /actuator/health endpoint is enabled.
    // * AC.4: Returns HTTP 503 when service is unhealthy.
    void whenServiceUnhealthy_thenStatus503() throws Exception {
        when(dbHealthIndicator.health()).thenReturn(Health.up().build());
        when(cacheHealthIndicator.health()).thenReturn(Health.up().build());
        when(messageBrokerHealthIndicator.health())
            .thenReturn(Health.down().withDetail("error", "connection failed").build());

        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isServiceUnavailable());
    }
}