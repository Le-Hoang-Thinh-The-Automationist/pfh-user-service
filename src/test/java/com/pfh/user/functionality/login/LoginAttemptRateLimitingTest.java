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
 *                  VP.1: Exactly 3 failed attempts in under 15 → returns 401 Unauthorized (locks on next attempt)
 *                  VP.2: Perform like in VP1 first and then wait for 15 minutes since the first attempt. After that perform
 *                        exactly 3 more failed attempts in under 15 → returns 401 Unauthorized (locks on next attempt)
 *              - Invalid Partitions (IP):
 *                  IP.1: 4th failed attempt within 15 minutes → returns 423 Locked
 *                  IP.2: Perform like in VP1 first and then wait for 15 minutes since the first attempt. After that perform
 *                        4 failed attempt within 15 minutes → returns 423 Locked at the 4th attempt
 *
 *          * **AC.2:** Account temporarily locked for 30 minutes after 3 failed attempts
 *              - Valid Partitions (VP):
 *                  VP.1: Any login attempt during lock period → returns 423 Locked
 *              - Invalid Partitions (IP):
 *                  IP.1: Attempt after 30 minutes lock period → returns 401 Unauthorized for invalid credential
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
import com.pfh.user.config.AppConstant;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.enums.UserStatus;
import com.pfh.user.functionality.abstraction.AbstractIntegrationTest;
import com.pfh.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
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

    @Autowired
    private UserRepository userRepository;

    // Use Argon2 for password hashing with OWASP recommended parameters
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
        AppConstant.ARGON2_SALT_LENGTH,
        AppConstant.ARGON2_HASH_LENGTH,
        AppConstant.ARGON2_PARALLELISM,
        AppConstant.ARGON2_MEMORY,
        AppConstant.ARGON2_ITERATIONS
    );

    private static final String LOGIN_URL = "/api/auth/login";
    private LoginRequestDto invalidCredentials;
    private LoginRequestDto ValidCredentials;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create a test user
        userRepository.save(UserEntity.builder()
            .email("user123@example.com")
            .passwordHash(encoder.encode("correctPassword123!!!"))
            .status(UserStatus.ACTIVE)
            .build());       

        invalidCredentials = new LoginRequestDto("user123@example.com", "wrongPassword123!!!");
        ValidCredentials = new LoginRequestDto("user123@example.com", "correctPassword123!!!");
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    void perform3AttemptsFailedLoginWithoutBeingLocked() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCredentials)))
                .andExpect(status().isUnauthorized());
        }
    }

    // --- AC.1 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - VP.1: 3 failed attempts return 401 Unauthorized but not locked")
    void ac1vp1_OneToTwoFailedAttempts_ShouldReturn401() throws Exception {
        // Given - invalid credentials

        // When perform 3 failed attempts
        // Then - expect 401 Unauthorized each time
        perform3AttemptsFailedLoginWithoutBeingLocked();

        // Verify account is not locked by attempting a valid login
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ValidCredentials)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - VP.2: 3 failed attempts after 15 minutes window resets, returns 401 Unauthorized")
    void ac1vp2_ThreeFailedAttemptsAfterWindowReset_ShouldReturn401() throws Exception {
        // Given - invalid credentials and attempt 3 failed attempts within 15 minutes
        ac1ip1_FourthFailedAttempt_ShouldReturn423();

        // When - Wait for 15 minutes to reset the window and perform 3 more failed attempts
        Thread.sleep(15 * 60 * 1000);

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCredentials)))

        // Then - expect 401 Unauthorized each time and not locked
                .andExpect(status().isUnauthorized());
        }

        // Verify account is not locked by attempting a valid login
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ValidCredentials)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - IP.1: 4th failed attempt returns 423 Locked")
    void ac1ip1_FourthFailedAttempt_ShouldReturn423() throws Exception {
        // Given - invalid credentials and perform 3 failed attempts within 15 minutes
        perform3AttemptsFailedLoginWithoutBeingLocked();
        
        // When - perform 4 failed attempts within 15 minutes
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCredentials)))

        // Then - expect 401 Unauthorized for first 3 and 423 Locked on 4th
            .andExpect(status().isLocked());

    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - IP.2: 4th failed attempt after window reset returns 423 Locked")
    void ac1ip2_FourthFailedAttemptAfterWindowReset_ShouldReturn423() throws Exception {
        // Given - invalid credentials, perform 3 failed attempts within 15 minutes and wait for 15 minutes
        perform3AttemptsFailedLoginWithoutBeingLocked();
        Thread.sleep(15 * 60 * 1000); // wait for 15 minutes

        // When - when 3 more failed attempts within new 15 minutes window and then 4th attempt
        // Then - expect 401 Unauthorized for first 3 and 423 Locked on 4th
        perform3AttemptsFailedLoginWithoutBeingLocked();

        // 4th attempt
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCredentials)))
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
