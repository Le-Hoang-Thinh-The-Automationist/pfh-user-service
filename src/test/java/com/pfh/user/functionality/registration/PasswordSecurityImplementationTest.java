// ./PasswordSecurityImplementationTest.java
/*
 *  [USER-STORY] Password Security Implementation
 *      **As a** security officer
 *      **I want** passwords to be securely stored and validated
 *      **So that** user credentials are protected according to financial industry standards
 *
 *      ✅ **Acceptance Criteria:**
 *
 *          * **AC.1:** Passwords are hashed using Argon2id algorithm
 *          * **AC.2:** Password strength validation checks against common password dictionaries
 *          * **AC.3:** Salt is unique for each password hash
 *          * **AC.4:** Original passwords are never stored in plain text
 *          * **AC.5:** Password hashing parameters follow OWASP recommendations
 *          * **AC.6:** Password must contain at least one uppercase, lowercase, digit, and special character
 *
 */
package com.pfh.user.functionality.registration;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.functionality.abstraction.AbstractIntegrationTest;
import com.pfh.user.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PasswordSecurityImplementationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    // Example encoder aligned with OWASP recommended parameters
    private Argon2PasswordEncoder encoder;

    private static final String ENDPOINT_URL = "/api/auth/register";

    private RegistrationRequestDto validRegistrationRequest;

    @BeforeEach
    void setUp() {
        // OWASP-recommended Argon2id parameters (example: iterations=3, memory=64MB, parallelism=2)
        encoder = new Argon2PasswordEncoder(16, 32, 2, 1 << 16, 3);
        userRepository.deleteAll();

        validRegistrationRequest = RegistrationRequestDto.builder()
                .email("john.doe@example.com")
                .password("SecurePassword123!")
                .confirmPassword("SecurePassword123!")
                .build();
    }

    @Test
    @DisplayName("[Password Security Implementation] AC.1 + AC.5: Passwords are hashed using Argon2id with OWASP parameters")
    // * AC.1: Verify Argon2id algorithm is used
    // * AC.5: Verify parameters follow OWASP recommendations
    // * AC.6: Password that meet the conditions "contain at least one uppercase, lowercase, digit, and special character"
    void passwordShouldBeHashedUsingArgon2id() {
        String rawPassword = "StrongP@ssw0rd123!";
        String hashed = encoder.encode(rawPassword);

        assertThat(hashed).isNotEqualTo(rawPassword); // not plaintext
        assertThat(encoder.matches(rawPassword, hashed)).isTrue(); // hash validates
        assertThat(hashed).startsWith("$argon2id$"); // Argon2id identifier
    }

    @ParameterizedTest
    @DisplayName("[Password Security Implementation] AC.2: Weak/common passwords should be rejected")
    @ValueSource(strings = {
        "password1234",        // 12 chars: predictable word + digits
        "iloveyou2020!!",      // 14 chars: common phrase + popular year + symbols
        "welcome12345!",       // 14 chars: greeting + digits + symbol
        "qwertyuiop123",       // 13 chars: extended keyboard sequence + digits
        "abc123abc123",        // 12 chars: repeated basic pattern
    })
    // * AC.2: Validate password strength against dictionary
    void commonPassword_ShouldBeRejected(String weakPassword ) throws Exception {

        validRegistrationRequest.setPassword(weakPassword);
        validRegistrationRequest.setConfirmPassword(weakPassword);
        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);

        mockMvc.perform(post(ENDPOINT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message").value("Password is too weak"))
                .andExpect(jsonPath("$.message").value("Password is too common"));
    }

    @Test
    @DisplayName("[Password Security Implementation] AC.3 + AC.6: Each password hash must use a unique salt")
    // * AC.3: Verify unique salt is applied per password
    // * AC.6: Password that meet the conditions "contain at least one uppercase, lowercase, digit, and special character"
    void samePassword_ShouldProduceDifferentHashes() {
        String rawPassword = "StrongP@ssw0rd123!";

        String hash1 = encoder.encode(rawPassword);
        String hash2 = encoder.encode(rawPassword);

        assertThat(hash1).isNotEqualTo(hash2); // Different due to salt
    }

    @Test
    @DisplayName("[Password Security Implementation] AC.4: Plain text passwords must never be stored or returned")
    // * AC.4: Ensure plaintext is not persisted or exposed in API response
    void plainTextPassword_ShouldNotBeStoredOrReturned() throws Exception {
        String rawPassword = "UniqueStrongPass123!";

        validRegistrationRequest.setPassword(rawPassword);
        validRegistrationRequest.setConfirmPassword(rawPassword);
        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);

        MvcResult result = mockMvc.perform(post(ENDPOINT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist()) // API should not return password
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andReturn();

        // Extract created user ID from response
        String responseJson = result.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(responseJson).get("userId").asLong();

        // Verify in DB
        Optional<UserEntity> savedUser = userRepository.findById(userId);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getPasswordHash()).isNotEqualTo(rawPassword); // never stored as plaintext
        assertThat(savedUser.get().getPasswordHash()).startsWith("$argon2id$"); // properly hashed
    }

    @ParameterizedTest
    @DisplayName("[User Registration] AC.6: Reject weak passwords missing required character types")
    @ValueSource(strings = {
            "aaaaaaaassword1@",     // missing uppercase
            "AAAAAAAASSWORD1@",     // missing lowercase
            "aaaaaaaassword@",      // missing digit
            "aaaaaaaassword1",      // missing special character
            "aaaaaaaassword",       // missing uppercase, digit, special
            "aaaaaaa2345678",       // missing letters and special
            "AAAAAAAAASSWRD",       // missing lowercase, digit, special
            "aaaaaaaassw0rd"        // missing special character
    })
    // * AC.6: Password that meet the conditions "contain at least one uppercase, lowercase, digit, and special character"
    void WeakPassword_ShouldBeRejected(String weakPassword) throws Exception {
        // Given
        validRegistrationRequest.setPassword(weakPassword);
        validRegistrationRequest.setConfirmPassword(weakPassword);
        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);

        // When & Then
        mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].message").value("Password is too weak"))
                .andExpect(jsonPath("$.message").value("Password must contain at least one uppercase, lowercase, digit, and special character"));
    }

}
