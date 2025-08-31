// ./BasicUserRegistrationApiTest.java
/*
 *  [USER-STORY] Basic User Registration API
 *      **As a** new customer
 *      **I want** to register with email and password
 *      **So that** I can create an account to access financial services
 *
 *      ✅ **Acceptance Criteria:**
 *
 *          * **AC.1:** POST /api/auth/register endpoint accepts email and password
 *          * **AC.2:** Email format validation returns 400 Bad Request for invalid emails
 *          * **AC.3:** Password complexity validation enforces minimum 12 characters
 *          * **AC.4:** Successful registration returns 201 Created with user ID
 *          * **AC.5:** Duplicate email registration returns 409 Conflict
 *          * **AC.6:** Email should be case insensitive and be stored in the database in lower case.
 *
 */
package com.pfh.user.functionality.registration;

import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.dto.RegistrationResponseDto;
import com.pfh.user.entity.UserEntity;
import com.pfh.user.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BasicUserRegistrationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String REGISTRATION_ENDPOINT = "/api/auth/register";
    
    private RegistrationRequestDto validRegistrationRequest;
    
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

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        validRegistrationRequest = RegistrationRequestDto.builder()
                .email("john.doe@example.com")
                .password("SecurePassword123!")
                .confirmPassword("SecurePassword123!")
                .build();
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.1 + AC.4: Valid registration returns 201 with user ID")
    // * AC.1: POST endpoint accepts email and password
    // * AC.4: Successful registration returns 201 Created with user ID
    void validRegistration_ShouldReturn201WithUserId() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);

        // When & Then
        MvcResult result = mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", notNullValue()))
                // .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andReturn();

        // Verify response contains valid UUID
        String responseBody = result.getResponse().getContentAsString();
        RegistrationResponseDto response = objectMapper.readValue(responseBody, RegistrationResponseDto.class);
        
        assertThat(response.getUserId()).isNotNull();
        // assertThat(response.getUserId().toString()).hasSize(36); // UUID format
        
        // Verify user was created in database
        assertThat(userRepository.findByEmailIgnoreCase("john.doe@example.com")).isPresent();
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.2: Invalid email format returns 400")
    // * AC.2: Email format validation returns 400 Bad Request for invalid emails
    void invalidEmailFormat_ShouldReturn400() throws Exception {
        // Given - Invalid email formats
        String[] invalidEmails = {
            "invalid-email",
            "missing@domain",
            "@domain.com",
            "user@",
            "user.domain.com",
            ""
        };

        for (String invalidEmail : invalidEmails) {
            // Given
            RegistrationRequestDto request = RegistrationRequestDto.builder()
                    .email(invalidEmail)
                    .password("SecurePassword123!")
                    .confirmPassword("SecurePassword123!")
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            // When & Then
            mockMvc.perform(post(REGISTRATION_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                    .andExpect(jsonPath("$.errors[?(@.field == 'email')].message").value("Invalid email format"));
        }
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.3: Short password returns 400")
    // * AC.3: Password complexity validation enforces minimum 12 characters
    void shortPassword_ShouldReturn400() throws Exception {
        // Given - Passwords shorter than 12 characters
        String[] shortPasswords = {
            "short",
            "password",
            "12345678901", // 11 characters
            ""
        };

        for (String shortPassword : shortPasswords) {
            // Given
            RegistrationRequestDto request = RegistrationRequestDto.builder()
                    .email("test@example.com")
                    .password(shortPassword)
                    .confirmPassword(shortPassword)
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            // When & Then
            mockMvc.perform(post(REGISTRATION_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                    .andExpect(jsonPath("$.errors[?(@.field == 'password')].message").value("Password must be at least 12 characters long"));
        }
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.3: Password mismatch returns 400")
    // * AC.3: Password validation includes confirmation matching
    void passwordMismatch_ShouldReturn400() throws Exception {
        // Given
        RegistrationRequestDto request = RegistrationRequestDto.builder()
                .email("test@example.com")
                .password("SecurePassword123!")
                .confirmPassword("DifferentPassword456!")
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'confirmPassword')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'confirmPassword')].message").value("Passwords do not match"));
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.5: Duplicate email returns 409")
    // * AC.5: Duplicate email registration returns 409 Conflict
    void duplicateEmail_ShouldReturn409() throws Exception {
        // Given - First registration
        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);

        mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // When - Second registration with same email
        RegistrationRequestDto duplicateRequest = RegistrationRequestDto.builder()
                .email("john.doe@example.com") // Same email
                .password("AnotherSecurePassword456!")
                .confirmPassword("AnotherSecurePassword456!")
                .build();

        String duplicateRequestBody = objectMapper.writeValueAsString(duplicateRequest);

        // Then
        mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequestBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')].message").value("Email already registered"))
                .andExpect(jsonPath("$.message", containsString("john.doe@example.com")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.1: Invalid content type returns 415")
    // * AC.1: POST endpoint requires proper content type
    void invalidContentType_ShouldReturn415() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);

        // When & Then
        mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(requestBody))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.1: Malformed JSON returns 400")
    // * AC.1: POST endpoint handles malformed JSON gracefully
    void malformedJson_ShouldReturn400() throws Exception {
        // Given
        String malformedJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'syntax')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'syntax')].message").value("Malformed JSON"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.4: Response includes security headers")
    // * AC.4: Successful registration includes proper security headers
    void successfulRegistration_ShouldIncludeSecurityHeaders() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);

        // When & Then
        mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("[Basic User Registration API] AC.1 + AC.4 + AC.6: Case insensitive email handling")
    // * AC.1: POST endpoint accepts email variations
    // * AC.4: Successful registration normalizes email
    // * AC.6: The case insensitivity for email should be activated, and the email should be stored in lower case.
    void caseInsensitiveEmail_ShouldNormalizeAndSucceed() throws Exception {
        // Given
        RegistrationRequestDto request = RegistrationRequestDto.builder()
                .email("JOHN.DOE@EXAMPLE.COM")
                .password("SecurePassword123!")
                .confirmPassword("SecurePassword123!")
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // When & Then
        MvcResult result = mockMvc.perform(post(REGISTRATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        // Verify email is normalized to lowercase in database
        UserEntity lowerCaseEmailQuery = userRepository.findByEmailIgnoreCase("john.doe@example.com").orElse(null);;
        UserEntity upperCaseEmailQuery = userRepository.findByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM").orElse(null);;

        
        assertThat(lowerCaseEmailQuery.getEmail().equals("john.doe@example.com")).isTrue();
        assertThat(upperCaseEmailQuery.getEmail().equals("john.doe@example.com")).isTrue();
    }
}