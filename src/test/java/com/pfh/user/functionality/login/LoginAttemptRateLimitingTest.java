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
 *                  VP.1: Exactly 3 failed attempts (wrong password only) in under 15 → returns 401 Unauthorized (locks on next attempt)
 *                  VP.2: - 1) Perform like in VP1 first and then wait for 15 minutes since the first attempt. 
 *                        - 2) After that perform exactly 3 more failed attempts in under 15 → returns 401 Unauthorized (locks on next attempt)
 *                  VP.3: Perform 3 failed attempts with three different time zone in under 15 minutes → returns 401 Unauthorized.
 *              - Invalid Partitions (IP):
 *                  IP.1: 4th failed attempt within 15 minutes → returns 423 Locked
 *                  IP.2: - 1) Perform like in VP1 first and then wait for 15 minutes since the first attempt. After that perform
 *                        - 2) 4 failed attempt within 15 minutes → returns 423 Locked at the 4th attempt
 *                  IP.3: 4 failed attempt at four different time zone in under 15 minutes → returns 423 Locked
 *
 *          * **AC.2:** Account temporarily locked for 30 minutes after 3 failed attempts
 *              - Valid Partitions (VP):
 *                  VP.1: Perform valid and invalid login attempt (wrong password only) during lock period in under 30 minutes → returns 423 Locked
 *                  VP.2: Do like VP.1 but at different time zones in under 30 minutes → returns 423 Locked
 *              - Invalid Partitions (IP):
 *                  IP.1: - 1) Perform 2 invalid attempts (wrong password), one at local time zone and other at another time zone
 *                        - 2) After 30 minutes lock period → returns 401 Unauthorized for invalid credential
 *                  IP.2: Valid attempt after 30 minutes lock period → returns successful login for valid credential
 *                  IP.3: Do like IP.2 but at different time zones in under 30 minutes → returns successful login for valid credential
 *
 *          * **AC.3:** IP-based rate limiting: 10 attempts per IP per minute
 *              - Valid Partitions (VP):
 *                  VP.1: 1-10 attempts from same IP within 1 minute → returns either 401 Unauthorized, or 423 Locked if user gets locked 
 *                  VP.2: - 1) 1-10 attempts from same IP within 1 minute. Wait for 1 minute, 
 *                        - 2) Then perform 10 more attempt from the same IP →  returns either 401 Unauthorized, or 423 Locked if user gets locked 
 *                  VP.3: 10 attempts from same IP and then perform 10 attempts at another IP within 1 minutes  →  returns either 401 Unauthorized, or 423 Locked if user gets locked 
 *              - Invalid Partitions (IP):
 *                  IP.1: - 1) From the 11th attempt of either valid or invalid from same IP within 1 minute → returns 429 Too Many Requests
 *                        - 2) After that wait for 1 minute and then perform another attempt → returns either 401 Unauthorized, or 423 Locked if user gets locked
 *                  IP.2: - 1) 11th attempt from same IP → returns 429 Too Many Requests 
 *                        - 2) and then perform 10 attempts at another IP within 1 minutes →  returns either 401 Unauthorized, or 423 Locked if user gets locked 
 *
 *          * **AC.4:** Rate limit violations logged with IP, timestamp, and user identifier
 *              - Valid Partitions (VP):
 *                  VP.1: Any rate-limit violation → audit log entry created
 */

package com.pfh.user.functionality.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfh.user.config.AppConstant;
import com.pfh.user.config.LoginRateLimitingProperties;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

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

    @Autowired
    private LoginRateLimitingProperties LoginRateLimitingProperties;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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

    private final long LOCKED_DURATION_MS   = 1 * 15 * 1000; // mock 30 minutes with 15 seconds for faster tests
    private final long ATTEMPT_WINDOW_MS    = 1 * 10 * 1000; // mock 15 minutes with 10 seconds for faster tests
    private final long IP_ATTEMPT_WINDOW_MS    = 1 * 10 * 1000; // mock 1 minutes with 10 seconds for faster tests

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        clearRedis();

        // Create a test user
        userRepository.save(UserEntity.builder()
            .email("user123@example.com")
            .passwordHash(encoder.encode("correctPassword123!!!"))
            .status(UserStatus.ACTIVE)
            .build());       

        invalidCredentials = new LoginRequestDto("user123@example.com", "wrongPassword123!!!");
        ValidCredentials = new LoginRequestDto("user123@example.com", "correctPassword123!!!");

        // Set rate limiting properties for tests purposes
        LoginRateLimitingProperties.setLockedDurationMs(LOCKED_DURATION_MS);
        LoginRateLimitingProperties.setAttemptWindowMs(ATTEMPT_WINDOW_MS);
        LoginRateLimitingProperties.setIpAttemptWindowMs(IP_ATTEMPT_WINDOW_MS);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        clearRedis();
    }

    private void clearRedis() {
        redisTemplate.getConnectionFactory()
            .getConnection()
            .serverCommands().flushAll();
    }

    void performThreeAttemptsFailedLoginWithoutBeingLocked() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCredentials)))
                .andExpect(status().isUnauthorized());
        }
    }

    void performThreeAttemptsFailedLoginAtDifferentTimezoneWithoutBeingLocked() throws Exception {
        String[] timeZones = {"UTC", "America/New_York", "Asia/Tokyo"};

        // Perform 3 failed attempts, each with a different time zone
        for (String tz : timeZones) {
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Timezone", tz) // Simulate different time zones
                    .content(objectMapper.writeValueAsString(invalidCredentials)))
                    .andExpect(status().isUnauthorized());
        }
    }

    void performFourAttemptsFailedLoginToLocked() throws Exception {
        performThreeAttemptsFailedLoginWithoutBeingLocked();
        
        // 4th attempt to lock the account
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidCredentials)))
            .andExpect(status().isLocked());

    }

    private ResultMatcher statusIsUnauthorizedOrForbidden() {
        return result -> {
            int status = result.getResponse().getStatus();
            if (status != HttpStatus.UNAUTHORIZED.value() &&
                status != HttpStatus.LOCKED.value()) {
                throw new AssertionError("Expected 401 or 403 but was " + status);
            }
        };
    }    
    // --- AC.1 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - VP.1: 3 failed attempts return 401 Unauthorized but not locked")
    void ac1vp1_OneToTwoFailedAttempts_ShouldReturn401() throws Exception {
        // Given - invalid credentials

        // When perform 3 failed attempts
        // Then - expect 401 Unauthorized each time
        performThreeAttemptsFailedLoginWithoutBeingLocked();

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
        performThreeAttemptsFailedLoginWithoutBeingLocked();
        Thread.sleep(ATTEMPT_WINDOW_MS);

        // When - Wait for 15 minutes to reset the window and perform 3 more failed attempts
        // Then - expect 401 Unauthorized each time and not locked
        performThreeAttemptsFailedLoginWithoutBeingLocked();

        // Verify account is not locked by attempting a valid login
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ValidCredentials)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - VP.3: 3 failed attempts from different time zones within 15 minutes returns 401 Unauthorized")
    void ac1vp3_ThreeFailedAttemptsDifferentTimeZones_ShouldReturn401() throws Exception {
        // Given - invalid credentials

        // When - perform 3 failed attempts at three different time zones
        // Then - expect 401 Unauthorized each time
        performThreeAttemptsFailedLoginAtDifferentTimezoneWithoutBeingLocked();

        // Verify account is not locked by attempting a valid login
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Timezone", "Europe/London")
            .content(objectMapper.writeValueAsString(ValidCredentials)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - IP.1: 4th failed attempt returns 423 Locked")
    void ac1ip1_FourthFailedAttempt_ShouldReturn423() throws Exception {
        // Given - invalid credentials and perform 3 failed attempts within 15 minutes
        performThreeAttemptsFailedLoginWithoutBeingLocked();
        
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
        performThreeAttemptsFailedLoginWithoutBeingLocked();
        Thread.sleep(ATTEMPT_WINDOW_MS); // wait for 15 minutes

        // When - when 3 more failed attempts within new 15 minutes window and then 4th attempt
        // Then - expect 401 Unauthorized for first 3 and 423 Locked on 4th
        performThreeAttemptsFailedLoginWithoutBeingLocked();

        // 4th attempt
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCredentials)))
            .andExpect(status().isLocked());

    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.1 - IP.3: 4 failed attempts from different time zones within 15 minutes returns 423 Locked")
    void ac1ip3_FourFailedAttemptsDifferentTimeZones_ShouldReturn423() throws Exception {
        // Given - invalid credentials

        // When - perform 3 failed attempts at three different time zones
        // Then - expect 401 Unauthorized each time
        performThreeAttemptsFailedLoginAtDifferentTimezoneWithoutBeingLocked();

        // 4th attempt (different time zone) should lock the account
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Timezone", "Europe/London")
                .content(objectMapper.writeValueAsString(invalidCredentials)))
                .andExpect(status().isLocked());
    }

    // --- AC.2 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.2 - VP.1: Attempt valid and invalid login during lock period returns 423 Locked")
    void ac2vp1_AttemptDuringLockPeriod_ShouldReturn423() throws Exception {
        // Given - invalid credentials and perform 4 failed attempts to lock the account
        performFourAttemptsFailedLoginToLocked();

        // When - attempt valid and invalid login during lock period
        // Then - expect 423 Locked for both attempts

        // Invalid attempt during lock period
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidCredentials)))
        .andExpect(status().isLocked());

        // Valid attempt during lock period
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ValidCredentials)))
        .andExpect(status().isLocked());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.2 - VP.2: Attempt valid and invalid login during lock period at different time zones returns 423 Locked")
    void ac2vp2_AttemptDuringLockPeriodDifferentTimeZones_ShouldReturn423() throws Exception {
        // Given - lock the account
        performFourAttemptsFailedLoginToLocked();
        String[] timeZones = {"UTC", "America/New_York", "Asia/Tokyo"};

        // When - attempt valid and invalid login during lock period at different time zones
        // Then - expect 423 Locked for both attempts at each time zone
        for (String tz : timeZones) {
            // Invalid attempt
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Timezone", tz)
                .content(objectMapper.writeValueAsString(invalidCredentials)))
                .andExpect(status().isLocked());

            // Valid attempt
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Timezone", tz)
                .content(objectMapper.writeValueAsString(ValidCredentials)))
                .andExpect(status().isLocked());
        }
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.2 - IP.1: Perform 2 invalid attempts, one at local time zone and other at another time zone, returns 401 Unauthorized")
    void ac2ip1_InvalidAttemptAfterLockPeriod_ShouldReturn401() throws Exception {
        // Given - invalid credentials and perform 4 failed attempts to lock the account, and wait for 30 minutes
        performFourAttemptsFailedLoginToLocked();
        Thread.sleep(LOCKED_DURATION_MS);

        // when - perform 2 invalid attempts, one at local time zone and other at another time zone
        // Then - expect 401 Unauthorized for invalid credential at both attempts
        
        // Local time zone attempt
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidCredentials)))
        .andExpect(status().isUnauthorized());

        // Different time zone attempt
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Timezone", "America/New_York")
            .content(objectMapper.writeValueAsString(invalidCredentials)))
        .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.2 - IP.2: Valid attempt after 30 minutes lock period at local time zone returns successful login")
    void ac2ip2_ValidAttemptAfterLockPeriod_ShouldReturnOk() throws Exception {
        // Given - invalid credentials and perform 4 failed attempts to lock the account, and wait for 30 minutes
        performFourAttemptsFailedLoginToLocked();
        Thread.sleep(LOCKED_DURATION_MS);

        // When - Valid attempt after lock period at local time zone
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ValidCredentials)))
        
        //  Then - expect successful login
        .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.2 - IP.3: Valid attempt after 30 minutes lock period at different time zones returns successful login")
    void ac2ip3_ValidAttemptAfterLockPeriodDifferentTimeZones_ShouldReturnOk() throws Exception {
        // Given - lock the account and wait for lock period
        performFourAttemptsFailedLoginToLocked();
        Thread.sleep(LOCKED_DURATION_MS);
        String[] timeZones = {"UTC", "America/New_York", "Asia/Tokyo"};

        // When - valid attempt after lock period at different time zones
        for (String tz : timeZones) {
            mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Timezone", tz)
                .content(objectMapper.writeValueAsString(ValidCredentials)))

            // Then - expect successful login each time at each time zone
            .andExpect(status().isOk());
        }
    }

    // --- AC.3 Tests ---

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.3 - VP.1: 1-10 attempts from same IP returns either 401 Unauthorized, or 423 Locked")
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
            .andExpect(statusIsUnauthorizedOrForbidden());
        }
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.3 - VP.2: 1-10 attempts from same IP, wait 1 minute, then 10 more attempts from same IP returns either 401 Unauthorized, or 423 Locked")
    void ac3vp2_TenAttemptsWaitThenTenMoreSameIp_ShouldReturn401() throws Exception {
        // Given
        var request = invalidCredentials;
        String clientIp = "203.0.113.6";

        // When: Perform 10 attempts from same IP
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(statusIsUnauthorizedOrForbidden());
        }

        // Wait for 1 minute (use IP_ATTEMPT_WINDOW_MS for test speed)
        Thread.sleep(IP_ATTEMPT_WINDOW_MS);

        // Then: Perform 10 more attempts from same IP
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(statusIsUnauthorizedOrForbidden());
        }
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.3 - VP.3: 10 attempts from one IP, then 10 attempts from another IP within 1 minutes returns either 401 Unauthorized, or 423 Locked")
    void ac3vp3_TenAttemptsEachDifferentIp_ShouldReturn401() throws Exception {
        // Given
        var request = invalidCredentials;
        String ip1 = "203.0.113.7";
        String ip2 = "203.0.113.8";

        // When: 10 attempts from ip1
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", ip1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(statusIsUnauthorizedOrForbidden());
        }

        // Then: 10 attempts from ip2
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", ip2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(statusIsUnauthorizedOrForbidden());
        }
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.3 - IP.1: 11th attempt from same IP returns 429 Too Many Requests and after 1 minutes returns either 401 Unauthorized, or 423 Locked")
    void ac3ip1_EleventhAttemptAfterWaitSameIp_ShouldReturn429() throws Exception {
        // Given
        var request = invalidCredentials;
        String clientIp = "203.0.113.9";

        // When: Perform 10 attempts from same IP 
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))    
            .andExpect(statusIsUnauthorizedOrForbidden());
        }

        // Then: From 11th attempt of valid or invalid from same IP returns 429
        // (performing valid attempts here)
        mockMvc.perform(post(LOGIN_URL)
            .header("X-Forwarded-For", clientIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ValidCredentials)))
            .andExpect(status().isTooManyRequests());

        // (performing invalid attempts here)
        mockMvc.perform(post(LOGIN_URL)
            .header("X-Forwarded-For", clientIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidCredentials)))
            .andExpect(status().isTooManyRequests());

        // Wait for 1 minute (use IP_ATTEMPT_WINDOW_MS for test speed)
        Thread.sleep(IP_ATTEMPT_WINDOW_MS);

        // Then: Next attempt from same IP returns either 401 Unauthorized, or 423 Locked
        mockMvc.perform(post(LOGIN_URL)
            .header("X-Forwarded-For", clientIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(statusIsUnauthorizedOrForbidden());
    }

    @Test
    @DisplayName("[Login Attempt Rate Limiting] AC.3 - IP.2: 11th attempt from same IP returns 429, then 10 attempts from another IP returns either 401 Unauthorized, or 423 Locked")
    void ac3ip2_EleventhAttemptThenTenFromOtherIp_ShouldReturn429And401() throws Exception {
        // Given
        var request = invalidCredentials;
        String ip1 = "203.0.113.10";
        String ip2 = "203.0.113.11";

        // 10 attempts from ip1
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", ip1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(statusIsUnauthorizedOrForbidden());
        }

        // When: 11th attempt from ip1
        mockMvc.perform(post(LOGIN_URL)
            .header("X-Forwarded-For", ip1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isTooManyRequests());

        // Then: 10 attempts from ip2
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", ip2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(statusIsUnauthorizedOrForbidden());
        }
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
