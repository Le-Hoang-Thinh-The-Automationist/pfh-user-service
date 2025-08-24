/*
 *  [USER-STORY] Security Endpoint Availability
 *      **As a** security auditor
 *      **I want** the system to confirm accessibility of key security endpoints
 *      **So that** I know authentication and authorization mechanisms are active
 *
 *      ✅ **Acceptance Criteria:**
 *
 *          * **AC.1:** `/api/auth/login` endpoint responds with `200` on valid request.
 *          * **AC.2:** `/api/auth/logout` endpoint responds with `200` on valid request.
 *          * **AC.3:** Unauthorized requests to protected endpoints return `401 Unauthorized`.
 *
 */
package com.pfh.user.functionality.infrastructure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityEndpointAvailabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    // * AC.1: `/api/auth/login` endpoint responds with `200` on valid request.
    void whenLoginRequestIsValid_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
            .andExpect(status().isOk());

        fail("Test failed intentionally: Need to check what is valid request in test");

    }

    @Test
    // * AC.2: `/api/auth/logout` endpoint responds with `200` on valid request.
    void whenLogoutRequestIsValid_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/auth/logout"))
            .andExpect(status().isOk());

        fail("Test failed intentionally: Need to check what is valid request in test");
    }

    @Test
    // * AC.3: Unauthorized requests to protected endpoints return `401 Unauthorized`.
    void whenRequestIsUnauthorized_thenStatus401() throws Exception {
        mockMvc.perform(get("/api/users")) // example protected endpoint
            .andExpect(status().isUnauthorized());
    }

}
