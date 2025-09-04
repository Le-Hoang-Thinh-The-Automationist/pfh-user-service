// ./LoginAttemptRateLimitingTest.java
/*
 *  [USER-STORY] Login Attempt Rate Limiting
 *      **As a** security officer
 *      **I want** to limit login attempts per user/IP
 *      **So that** we prevent brute force attacks on customer accounts
 *
 *      ✅ **Acceptance Criteria with Equivalence Partitions:**
 *
 *          * **AC.1:** Maximum 3 failed login attempts per user within 15 minutes
 *              - Valid Partitions (VP):
 *                  VP.1: 1-2 failed attempts → returns 401 Unauthorized
 *                  VP.2: Exactly 3 failed attempts → returns 401 Unauthorized (locks on next attempt)
 *              - Invalid Partitions (IP):
 *                  IP.1: 4th failed attempt within 15 minutes → returns 423 Locked
 *
 *          * **AC.2:** Account temporarily locked for 30 minutes after 3 failed attempts
 *              - Valid Partitions (VP):
 *                  VP.1: Any login attempt during lock period → returns 423 Locked
 *
 *          * **AC.3:** IP-based rate limiting: 10 attempts per IP per minute
 *              - Valid Partitions (VP):
 *                  VP.1: 1-10 attempts from same IP within 1 minute → returns 401 Unauthorized
 *              - Invalid Partitions (IP):
 *                  IP.1: 11th attempt from same IP within 1 minute → returns 429 Too Many Requests
 *
 *          * **AC.4:** Rate limit violations logged with IP, timestamp, and user identifier
 *              - Valid Partitions (VP):
 *                  VP.1: Any rate-limit violation → audit log entry created
 */

package com.pfh.user.functionality.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.functionality.abstraction.AbstractIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginAttemptRateLimitingTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOGIN_URL = "/api/auth/login";
    private LoginRequestDto invalidCredentials;

    @BeforeEach
    void setUp() {
        invalidCredentials = new LoginRequestDto("user123", "wrongPassword");
    }

    // --- AC.1 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - VP.1: 1-2 failed attempts return 401 Unauthorized")
    void ac1vp1_OneToTwoFailedAttempts_ShouldReturn401() throws Exception {
        // Given
        var request = invalidCredentials;

        // When & Then
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - IP.1: 4th failed attempt returns 423 Locked")
    void ac1ip1_FourthFailedAttempt_ShouldReturn423() throws Exception {
        // Given
        var request = invalidCredentials;

        // When: perform 4 failed attempts within 15 minutes
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        }

        // Then
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isLocked());
    }

    // --- AC.2 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.2 - VP.1: Attempt during lock period returns 423 Locked")
    void ac2vp1_AttemptDuringLockPeriod_ShouldReturn423() throws Exception {
        // Given
        var request = invalidCredentials;
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        }

        // When
        var result = mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isLocked());
    }

    // --- AC.3 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.3 - VP.1: 1-10 attempts from same IP return 401 Unauthorized")
    void ac3vp1_OneToTenAttemptsSameIp_ShouldReturn401() throws Exception {
        // Given
        var request = invalidCredentials;
        String clientIp = "203.0.113.5";

        // When & Then
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.3 - IP.1: 11th attempt from same IP returns 429 Too Many Requests")
    void ac3ip1_EleventhAttemptSameIp_ShouldReturn429() throws Exception {
        // Given
        var request = invalidCredentials;
        String clientIp = "203.0.113.5";
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        }

        // When & Then
        mockMvc.perform(post(LOGIN_URL)
            .header("X-Forwarded-For", clientIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isTooManyRequests());
    }

    // --- AC.4 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.4 - VP.1: Rate limit violations logged with IP, timestamp, and user identifier")
    void ac4vp1_RateLimitViolation_ShouldBeLogged() throws Exception {
        // Given
        var request = invalidCredentials;
        String clientIp = "203.0.113.5";

        // When: exceed IP limit
        for (int i = 1; i <= 11; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        }

        // Then
        // TODO: Verify audit log entry contains IP, timestamp, and user identifier
    }
}
