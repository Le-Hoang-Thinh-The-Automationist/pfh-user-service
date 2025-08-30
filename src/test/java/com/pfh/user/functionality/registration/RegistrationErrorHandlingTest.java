// ./RegistrationErrorHandlingTest.java
/*
 *  [USER-STORY] Registration Error Handling
 *      **As a** new customer
 *      **I want** clear error messages when registration fails
 *      **So that** I can understand and correct any issues with my information
 *
 *      ✅ **Acceptance Criteria:**
 *
 *          * **AC.1:** Validation errors return structured JSON with field-specific messages
 *          * **AC.2:** Security errors return generic messages to prevent information disclosure
 *          * **AC.3:** HTTP status codes accurately reflect error types (400, 409, 429, 500)
 *          * **AC.4:** Error messages are user-friendly and actionable
 *          * **AC.5:** Internal error details are logged separately for debugging
 */
package com.pfh.user.functionality.registration;

import com.pfh.user.service.UserService;
import com.pfh.user.dto.RegistrationRequestDto;
import com.pfh.user.exception.DuplicateResourceException;
import com.pfh.user.exception.RateLimitExceededException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RegistrationErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static final String ENDPOINT_URL = "/api/auth/register";

    private RegistrationRequestDto validRequest;
    
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
        validRequest = RegistrationRequestDto.builder()
            .email("new.user@example.com")
            .password("ValidPass123!")
            .build();
    }

    @Test
    @DisplayName("[Registration Error Handling] AC.1: Validation errors return structured JSON")
    // * AC.1: Validation errors return structured JSON with field-specific messages
    void whenRequestMissingRequiredFields_then400AndFieldErrors() throws Exception {
        // Given
        RegistrationRequestDto invalid = RegistrationRequestDto.builder()
            .email("not-an-email")
            .password("123")
            .build();

        String body = objectMapper.writeValueAsString(invalid);

        // When & Then
        mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errors", hasSize(2)))
            .andExpect(jsonPath("$.errors[?(@.field=='email')].message",
                    containsString("must be a well-formed email address")))
            .andExpect(jsonPath("$.errors[?(@.field=='password')].message",
                    containsString("size must be between")));
    }

    @Test
    @DisplayName("[Registration Error Handling] AC.2: Security errors return generic messages")
    // * AC.2: Security errors return generic messages to prevent information disclosure
    void whenSecurityException_then403AndGenericMessage() throws Exception {
        // Given
        given(userService.register(any()))
            .willThrow(new SecurityException("Password hash failure"));

        String body = objectMapper.writeValueAsString(validRequest);

        // When & Then
        mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error")
                .value("Registration failed due to security constraints"));
    }

    @Test
    @DisplayName("[Registration Error Handling] AC.3: Duplicate registration returns 409")
    // * AC.3: HTTP status codes accurately reflect error types (409 conflict)
    // * AC.4: Error messages are user-friendly and actionable
    void whenDuplicateEmail_then409AndHelpfulMessage() throws Exception {
        // Given
        given(userService.register(any()))
            .willThrow(new DuplicateResourceException("email"));

        String body = objectMapper.writeValueAsString(validRequest);

        // When & Then
        mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error")
                .value("Email already registered"))
            .andExpect(jsonPath("$.message")
                .value("Try logging in or use another email address"));
    }

    @Test
    @DisplayName("[Registration Error Handling] AC.3: Rate limit exceeded returns 429")
    // * AC.3: HTTP status codes accurately reflect error types (429 too many requests)
    void whenTooManyRequests_then429AndRetryMessage() throws Exception {
        // Given
        given(userService.register(any()))
            .willThrow(new RateLimitExceededException("Too many attempts"));

        String body = objectMapper.writeValueAsString(validRequest);

        // When & Then
        mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error")
                .value("Too many registration attempts"))
            .andExpect(jsonPath("$.message")
                .value("Please wait a few minutes and try again"));
    }

    @Test
    @DisplayName("[Registration Error Handling] AC.3 & AC.5: Internal server error returns 500")
    // * AC.3: HTTP status codes accurately reflect error types (500 server error)
    // * AC.5: Internal error details are logged separately for debugging
    void whenUnexpectedException_then500AndNoStackTraceInResponse() throws Exception {
        // Given
        given(userService.register(any()))
            .willThrow(new RuntimeException("Database unavailable"));

        String body = objectMapper.writeValueAsString(validRequest);

        // When & Then
        mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error")
                .value("An unexpected error occurred"))
            // ensure no internal details leak
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist());
    }
}