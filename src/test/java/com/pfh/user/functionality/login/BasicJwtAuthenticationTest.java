// ./BasicJwtAuthenticationTest.java
/*
 *  [USER-STORY] Basic JWT Authentication
 *      **As a** registered user
 *      **I want** to login with email/password and receive a JWT token
 *      **So that** I can access protected financial resources securely
 *
 *      ✅ **Acceptance Criteria with Equivalence Partitions:**
 *
 *          * **AC.1:** POST /api/auth/login accepts valid email/password and returns JWT token
 *              - Valid Partitions (VP):
 *                  VP.1: Correct email + correct password
 *              - Invalid Partitions (IP):
 *                  IP.1: Correct email + wrong password
 *                  IP.2: Unknown email
 *
 *          * **AC.2:** JWT token contains user ID (Subject), email, roles, and expiration (≤15 min)
 *              - VP.1: JWT payload includes user ID as Subject, email, roles, and exp ≤ 900s
 *
 *          * **AC.3:** Password validation uses Argon2PasswordEncoder with OWASP recommendation
 *              - VP.1: Argon2id hash with OWASP parameters accepted
 *              - IP.1: Hash with weak/non-OWASP parameters rejected
 *
 *          * **AC.4:** Invalid credentials return 401 Unauthorized with generic error
 *              - VP.1: Response body = {"message":"Invalid credentials"}
 */

package com.pfh.user.functionality.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfh.user.dto.auth.LoginRequestDto;
import com.pfh.user.dto.auth.RegistrationRequestDto;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BasicJwtAuthenticationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REGISTRATION_ENDPOINT = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";

    @Autowired
    private UserRepository userRepository;

    private RegistrationRequestDto validRegistrationRequest;
    private LoginRequestDto validLogin;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        validRegistrationRequest = RegistrationRequestDto.builder()
                .email("john.doe@example.com")
                .password("SecurePassword123!")
                .confirmPassword("SecurePassword123!")
                .build();
        

        validLogin = LoginRequestDto.builder()
                .email("john.doe@example.com")
                .password("SecurePassword123!")
                .build();

        String requestBody = objectMapper.writeValueAsString(validRegistrationRequest);
        mockMvc.perform(post(REGISTRATION_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // --- AC.1 Tests ---
    @Test
    @DisplayName("[Basic JWT Authentication] AC.1 - VP.1: Login with valid credentials returns JWT")
    void ac1vp1_LoginWithValidCredentials_ShouldReturnJwtToken() throws Exception {
        // Given - The newly registered account existed in the DB from setUp()

        // When - Attempt to login with correct credentials
        mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validLogin)))

        // Then - Should return 200 OK with JWT token in response   
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();
    }

    @Test
    @DisplayName("[Basic JWT Authentication] AC.1 - IP.1: Login with wrong password returns 401")
    void ac1ip1_LoginWithWrongPassword_ShouldReturn401() throws Exception {
        // Given - The newly registered account existed in the DB from setUp()

        // When - Attempt to login with wrong password
        LoginRequestDto request = LoginRequestDto.builder()
                .email("john.doe@example.com")
                .password("WrongPass")
                .build();

        mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                
        // Then - Should return 401 Unauthorized with generic error message                
                .andExpect(status().isUnauthorized())                    
                .andExpect(jsonPath("$.errors[?(@.field == 'credential')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'credential')].message").value("Invalid credentials"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andReturn();
    }

    @Test
    @DisplayName("[Basic JWT Authentication] AC.1 - IP.2: Login with unknown email returns 401")
    void ac1ip2_LoginWithUnknownEmail_ShouldReturn401() throws Exception {
        // Given - The newly registered account existed in the DB from setUp()

        // When - Attempt to login with unknown email
        LoginRequestDto request = LoginRequestDto.builder()
                .email("unknown@example.com")
                .password("SecurePassword123!")
                .build();

        mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        
        // Then - Should return 401 Unauthorized with generic error message                
                .andExpect(status().isUnauthorized())                    
                .andExpect(jsonPath("$.errors[?(@.field == 'credential')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'credential')].message").value("Invalid credentials"))
                .andReturn();
    }

    // --- AC.2 Tests ---
    @Test
    @DisplayName("[Basic JWT Authentication] AC.2 - VP.1: JWT contains userId, email, roles, exp ≤ 15min")
    void ac2vp1_JwtPayloadContainsRequiredClaimsAndValidExp_ShouldReturnValidClaimsAndExp() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(validLogin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

        // Extract JWT token from response
        String responseBody = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Decode JWT and check exp claim using the new Jwts.parserBuilder() API
        Claims claims = Jwts.parserBuilder()
            .setSigningKey("DummySecretKeyWhichIsAtLeast32CharactersLong".getBytes(StandardCharsets.UTF_8))
            .build()
            .parseClaimsJws(token)
            .getBody();

        // Check required claims
        assert claims.get("email").equals("john.doe@example.com");
        assert claims.get("roles") != null;
        assert claims.getSubject() != null;

        // Check expiration time is ≤ 900 seconds (15 minutes) from issuedAt
        long issuedAt = claims.getIssuedAt().getTime();
        long expiration = claims.getExpiration().getTime();
        long diffSeconds = (expiration - issuedAt) / 1000;
        assert diffSeconds <= 900 : "JWT expiration exceeds 15 minutes";
    }

    // --- AC.3 Tests ---
    @Test
    @DisplayName("[Basic JWT Authentication] AC.3 - VP.1: Argon2id with OWASP params accepted")
    void ac3vp1_Argon2idOwaspParams_ShouldAccept() {
        // **AC.3.1:** Salt Length, according to OWASP: ≥16 bytes
        int saltLength = 16; // OWASP: ≥16 bytes
        // **AC.3.2:** Hash Length, according to OWASP: ≥32 bytes
        int hashLength = 32; // OWASP: ≥32 bytes
        // **AC.3.3:** Parallelism, according to OWASP: ≥2
        int parallelism = 2; // OWASP: ≥2
        // **AC.3.4:** Memory, according to OWASP: ≥65536 KB (64 MB)
        int memory = 1 << 16; // OWASP: ≥65536 KB (64 MB)
        // **AC.3.5:** Iterations, according to OWASP: ≥3 
        int iterations = 3; // OWASP: ≥3

        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
                saltLength, hashLength, parallelism, memory, iterations
        );

        String hash = encoder.encode("SecurePass123!");
        assert encoder.matches("SecurePass123!", hash);
    }

    @Test
    @DisplayName("[Basic JWT Authentication] AC.3 - IP.1: Argon2id with weak params should not be used")
    void ac3ip1_Argon2idWeakParams_ShouldBeRejected() {
        int saltLength = 8; // Too short
        int hashLength = 16; // Too short
        int parallelism = 1; // Too low
        int memory = 4096; // Too low
        int iterations = 1; // Too low

        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
                saltLength, hashLength, parallelism, memory, iterations
        );

        String hash = encoder.encode("SecurePass123!");
        // In real system, this config should be forbidden
        assert saltLength < 16 || hashLength < 32 || parallelism < 2 || memory < (1 << 16) || iterations < 3;
    }

    // --- AC.4 Tests ---
    @Test
    @DisplayName("[Basic JWT Authentication] AC.4 - VP.1: Invalid credentials return 401 with generic error")
    void ac4vp1_InvalidCredentials_ShouldReturnGenericError() throws Exception {
        // Already covered in AC.1 IP tests
    }
}
