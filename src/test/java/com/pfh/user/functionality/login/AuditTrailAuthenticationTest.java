// ./AuditTrailAuthenticationTest.java
/*
 *  [USER-STORY] Audit Trail for Authentication
 *      **As a** compliance officer
 *      **I want** all authentication events logged
 *      **So that** we maintain regulatory audit trails
 *
 *      ✅ **Acceptance Criteria with Equivalence Partitions:**
 *
 *          * **AC.1:** Successful logins logged with user ID, IP, timestamp, user agent
 *              - VP.1: Valid login creates audit log with required fields
 *
 *          * **AC.2:** Failed login attempts logged with attempted email, IP, failure reason
 *              - VP.1: Wrong password logged as "invalid_credentials"
 *              - VP.2: Unknown email logged as "user_not_found"
 *
 *          * **AC.3:** Account lockouts logged with duration and trigger event
 *              - VP.1: After 5 failed attempts, lockout logged with 15min duration
 *
 *          * **AC.4:** Logs stored in tamper-evident format for compliance (7 years)
 *              - VP.1: Log entries contain integrity hash / signature field
 */

package com.pfh.user.functionality.login;

import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.entity.AuditLogEntity;
import com.pfh.user.functionality.abstraction.AbstractIntegrationTest;
import com.pfh.user.repository.AuditLogRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuditTrailAuthenticationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final String LOGIN_URL = "/api/auth/login";

    private LoginRequestDto validLogin;

    @BeforeEach
    void setUp() {
        // Preloaded test user in DB: user@example.com / SecurePass123
        validLogin = LoginRequestDto.builder()
                .email("user@example.com")
                .password("SecurePass123")
                .build();
    }

    // --- AC.1 Tests ---
    @Test
    @DisplayName("[Audit Trail for Authentication] AC.1 - VP.1: Successful login logged with userId, IP, timestamp, userAgent")
    void ac1vp1_SuccessfulLogin_ShouldCreateAuditLog() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .header("User-Agent", "JUnit-Test-Agent")
                .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk());

        List<AuditLogEntity> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AuditLogEntity log = logs.get(0);

        assertThat(log.getUserId()).isNotNull();
        assertThat(log.getIpAddress()).isNotBlank();
        assertThat(log.getTimestamp()).isNotNull();
        assertThat(log.getUserAgent()).isEqualTo("JUnit-Test-Agent");
        assertThat(log.getEventType()).isEqualTo("LOGIN_SUCCESS");
    }

    // --- AC.2 Tests ---
    @Test
    @DisplayName("[Audit Trail for Authentication] AC.2 - VP.1: Wrong password attempt logged with reason invalid_credentials")
    void ac2vp1_WrongPassword_ShouldLogInvalidCredentials() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("user@example.com")
                .password("WrongPass")
                .build();

        mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        AuditLogEntity log = auditLogRepository.findTopByOrderByIdDesc();
        assertThat(log.getEmail()).isEqualTo("user@example.com");
        assertThat(log.getEventType()).isEqualTo("LOGIN_FAILURE");
        assertThat(log.getFailureReason()).isEqualTo("invalid_credentials");
    }

    @Test
    @DisplayName("[Audit Trail for Authentication] AC.2 - VP.2: Unknown email attempt logged with reason user_not_found")
    void ac2vp2_UnknownEmail_ShouldLogUserNotFound() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("unknown@example.com")
                .password("SomePass123")
                .build();

        mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        AuditLogEntity log = auditLogRepository.findTopByOrderByIdDesc();
        assertThat(log.getEmail()).isEqualTo("unknown@example.com");
        assertThat(log.getEventType()).isEqualTo("LOGIN_FAILURE");
        assertThat(log.getFailureReason()).isEqualTo("user_not_found");
    }

    // --- AC.3 Tests ---
    @Test
    @DisplayName("[Audit Trail for Authentication] AC.3 - VP.1: Lockout after 5 failed attempts logged with duration")
    void ac3vp1_LockoutAfterFailedAttempts_ShouldBeLogged() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("user@example.com")
                .password("WrongPass")
                .build();

        // Simulate 5 failed login attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(LOGIN_URL)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        AuditLogEntity log = auditLogRepository.findTopByOrderByIdDesc();
        assertThat(log.getEventType()).isEqualTo("ACCOUNT_LOCKOUT");
        assertThat(log.getLockoutDurationMinutes()).isEqualTo(15);
        assertThat(log.getTriggerEvent()).isEqualTo("5_failed_logins");
    }

    // --- AC.4 Tests ---
    @Test
    @DisplayName("[Audit Trail for Authentication] AC.4 - VP.1: Audit log entry contains integrity hash for tamper-evidence")
    void ac4vp1_AuditLog_ShouldContainIntegrityHash() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk());

        AuditLogEntity log = auditLogRepository.findTopByOrderByIdDesc();
        assertThat(log.getIntegrityHash()).isNotBlank();
    }
}
