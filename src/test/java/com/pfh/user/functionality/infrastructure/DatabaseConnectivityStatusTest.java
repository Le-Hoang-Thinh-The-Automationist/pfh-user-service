/* 
 *  [USER-STORY]: Database Connectivity Status
 *      **As a** DevOps engineer
 *      **I want** the User Service to expose an endpoint that checks PostgreSQL connectivity
 *      **So that** I can verify the application is properly connected to the database
 *
 *      ✅ **Acceptance Criteria:**
 *
 *          * **AC.1:** Endpoint `/db/status` (or equivalent) is available.
 *          * **AC.2:** Returns HTTP `200` when DB is reachable.
 *          * **AC.3:** JSON response includes `{ "status": "connected", "db": "postgresql" }`.
 *          * **AC.4:** Returns HTTP `503` if DB is unreachable.
 *
 */


package com.pfh.user.functionality.infrastructure;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class DatabaseConnectivityStatusTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    // * **AC.1:** Endpoint `/db/status` (or equivalent) is available.
    // * **AC.2:** Returns HTTP `200` when DB is reachable.
    // * **AC.3:** JSON response includes `{ "status": "connected", "db": "postgresql" }`.
    void whenDbIsReachable_thenStatus200AndConnectedJson() throws Exception {
        // simulate SELECT 1 returning a value
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenReturn(1);

        mockMvc.perform(get("/db/status"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("connected"))
            .andExpect(jsonPath("$.db").value("postgresql"));
    }

    @Test
    // * **AC.4:** Returns HTTP `503` if DB is unreachable.
    void whenDbIsUnreachable_thenStatus503() throws Exception {
        // simulate a failure when querying the DB
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new DataAccessException("DB down") {});

        mockMvc.perform(get("/db/status"))
            .andExpect(status().isServiceUnavailable());
    }
}