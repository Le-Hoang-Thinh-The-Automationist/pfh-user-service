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
 *          * **AC.4:** If DB is connected, it should provide information like { "connectionStatus": "connected", "dbHostSource": "<CONNECTED_DATABASE_HOST>" }`.
 *          * **AC.5:** Returns HTTP 503 when service is unhealthy.
 *          * **AC.6:** If DB is not connected, it should provide information like { "connectionStatus": "disconnected", "dbHostSource": "<CONNECTED_DATABASE_HOST>" }`.
 *
 */
package com.pfh.user.functionality.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApplicationHealthCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${db.dbHostSource}")
    private String dbHostSource;
    
    // Start a PostgreSQL Testcontainer
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    // Dynamically override Spring datasource properties
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

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
    // * AC.4: If DB is connected, it should provide information like { "connectionStatus": "connected", "dbHostSource": "<CONNECTED_DATABASE_HOST>" }`.
    void whenAppUpAndDbConnected_thenHealthUp() throws Exception {

        mockMvc.perform(get("/actuator/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details.connectionStatus").value("connected"))
                .andExpect(jsonPath("$.components.db.details.dbHostSource").value(dbHostSource));
    }

    @Test
    @DisplayName("[Application Health Check] AC.5 + AC.6: Service unhealthy when DB disconnected")
    // * AC.5: Returns HTTP 503 when service is unhealthy.
    // * AC.6: If DB is not connected, it should provide information like { "connectionStatus": "disconnected", "dbHostSource": "<CONNECTED_DATABASE_HOST>" }`.
    void whenDbDisconnected_thenServiceUnhealthy() throws Exception {
        // Pause DB container to simulate failure
        postgres.getDockerClient().pauseContainerCmd(postgres.getContainerId()).exec();

        try {
                mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$.status").value("DOWN"))
                        .andExpect(jsonPath("$.components.db.details.connectionStatus").value("disconnected"))
                        .andExpect(jsonPath("$.components.db.details.dbHostSource").value(dbHostSource));
        } finally {
                // Unpause so other tests don’t fail
                postgres.getDockerClient().unpauseContainerCmd(postgres.getContainerId()).exec();
        }
    }
}
