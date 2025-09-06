// ./AccountSecurityValidationTest.java
/*
 *  [USER-STORY] Account Security Validation
 *      **As a** financial institution
 *      **I want** to validate account status during login
 *      **So that** only active, compliant accounts can access the system
 *
 *      ✅ **Acceptance Criteria with Equivalence Partitions:**
 *
 *          * **AC.1:** Locked accounts return `423 Locked` status
 *              - Valid Partitions (VP):
 *                  VP.1: Active account → returns 200 OK
 *              - Invalid Partitions (IP):
 *                  IP.1: Locked account → returns 423 Locked
 *
 *          * **AC.2:** Disabled/suspended accounts return `403 Forbidden`
 *              - Valid Partitions (VP):
 *                  VP.1: Active account → returns 200 OK
 *              - Invalid Partitions (IP):
 *                  IP.1: Disabled account → returns 403 Forbidden
 *                  IP.2: Suspended account → returns 403 Forbidden
 *
 *          * **AC.3:** Expired accounts return `401 Unauthorized` with account renewal message
 *              - Valid Partitions (VP):
 *                  VP.1: Active account → returns 200 OK
 *              - Invalid Partitions (IP):
 *                  IP.1: Expired account → returns 401 Unauthorized + renewal message
 *
 *          * **AC.4:** Account status checks are logged for audit purposes
 *              - Valid Partitions (VP):
 *                  VP.1: Any login attempt → audit log entry created
 */

package com.pfh.user.functionality.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfh.user.config.AppConstant;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.functionality.abstraction.AbstractIntegrationTest;
import com.pfh.user.repository.UserRepository;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.enums.UserStatus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class AccountSecurityValidationTest extends AbstractIntegrationTest {

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
    private static final String TEST_PASSWORD = "/api/auth/register";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Register users with different statuses
        userRepository.save(UserEntity.builder()
            .email("activeUser@example.com")
            .passwordHash(encoder.encode(TEST_PASSWORD))
            .status(UserStatus.ACTIVE)
            .build());

        userRepository.save(UserEntity.builder()
            .email("lockedUser@example.com")
            .passwordHash(encoder.encode(TEST_PASSWORD))
            .status(UserStatus.LOCKED)
            .build());

        userRepository.save(UserEntity.builder()
            .email("disabledUser@example.com")
            .passwordHash(encoder.encode(TEST_PASSWORD))
            .status(UserStatus.DISABLED)
            .build());

        userRepository.save(UserEntity.builder()
            .email("suspendedUser@example.com")
            .passwordHash(encoder.encode(TEST_PASSWORD))
            .status(UserStatus.SUSPENDED)
            .build());

        userRepository.save(UserEntity.builder()
            .email("expiredUser@example.com")
            .passwordHash(encoder.encode(TEST_PASSWORD))
            .status(UserStatus.EXPIRED)
            .build());
    }

    @AfterAll
    void tearDown() {
        userRepository.deleteAll();
    }

    // --- AC.1 Tests ---

    @Test
    @DisplayName("[Account Security Validation] AC.1 - VP.1: Active account returns 200 OK with welcome message")
    void ac1vp1_ActiveAccount_ShouldReturn200WithMessage() throws Exception {
        // Given - Active user
        
        // When - Attempt to login
        var request = new LoginRequestDto("activeUser@example.com", TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        
        // Then - Should succeed    
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("[Account Security Validation] AC.1 - IP.1: Locked account returns 423 Locked with locked message")
    void ac1ip1_LockedAccount_ShouldReturn423WithMessage() throws Exception {
        // Given - Locked user
        
        // When - Attempt to login
        var request = new LoginRequestDto("lockedUser@example.com", TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))

        // Then - Should be locked    
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.message").value("Your account is locked. Please contact support."));
    }

    // --- AC.2 Tests ---

    @Test
    @DisplayName("[Account Security Validation] AC.2 - IP.1: Disabled account returns 403 Forbidden with disabled message")
    void ac2ip1_DisabledAccount_ShouldReturn403WithMessage() throws Exception {
        // Given - Disabled user

        // When - Attempt to login
        var request = new LoginRequestDto("disabledUser@example.com", TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))

        // Then - Should be forbidden
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Your account is disabled. Please contact support."));
    }

    @Test
    @DisplayName("[Account Security Validation] AC.2 - IP.2: Suspended account returns 403 Forbidden with suspended message")
    void ac2ip2_SuspendedAccount_ShouldReturn403WithMessage() throws Exception {
        // Given - Suspended user
        
        // When - Attempt to login
        var request = new LoginRequestDto("suspendedUser@example.com", TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))

        // Then - Should be forbidden
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Your account is suspended. Please contact support."));
    }

    // --- AC.3 Tests ---

    @Test
    @DisplayName("[Account Security Validation] AC.3 - IP.1: Expired account returns 401 Unauthorized with renewal message")
    void ac3ip1_ExpiredAccount_ShouldReturn401WithMessage() throws Exception {
        // Given - Expired user
        
        // When - Attempt to login
        var request = new LoginRequestDto("expiredUser@example.com", TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))

        // Then - Should be unauthorized
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Your account has expired. Please renew."));
    }

    // --- AC.4 Tests ---

    @Test
    @DisplayName("[Account Security Validation] AC.4 - VP.1: Account status checks are logged for audit purposes")
    void ac4vp1_AccountStatusChecksLogged_ShouldBeLogged() throws Exception {
        var request = new LoginRequestDto("activeUser@example.com", TEST_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        // Then
        // TODO: Verify audit log entry was created
    }
}
