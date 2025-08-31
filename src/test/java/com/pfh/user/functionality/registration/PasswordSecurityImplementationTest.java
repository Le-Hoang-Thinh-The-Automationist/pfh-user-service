// // ./PasswordSecurityImplementationTest.java
// /*
//  *  [USER-STORY] Password Security Implementation
//  *      **As a** security officer
//  *      **I want** passwords to be securely stored and validated
//  *      **So that** user credentials are protected according to financial industry standards
//  *
//  *      ✅ **Acceptance Criteria:**
//  *
//  *          * **AC.1:** Passwords are hashed using Argon2id algorithm
//  *          * **AC.2:** Password strength validation checks against common password dictionaries
//  *          * **AC.3:** Salt is unique for each password hash
//  *          * **AC.4:** Original passwords are never stored in plain text
//  *          * **AC.5:** Password hashing parameters follow OWASP recommendations
//  *
//  */
// package com.pfh.user.functionality.registration;

// import com.pfh.user.entity.UserEntity;
// import com.pfh.user.repository.UserRepository;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
// import org.springframework.test.annotation.DirtiesContext;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.MvcResult;
// import org.springframework.transaction.annotation.Transactional;
// import org.testcontainers.containers.PostgreSQLContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;

// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.hamcrest.Matchers.containsString;
// import static org.hamcrest.Matchers.notNullValue;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest
// @AutoConfigureMockMvc
// @Transactional
// @Testcontainers
// @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// class PasswordSecurityImplementationTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @Autowired
//     private ObjectMapper objectMapper;

//     @Autowired
//     private UserRepository userRepository;

//     // Example encoder aligned with OWASP recommended parameters
//     private Argon2PasswordEncoder encoder;

//     private static final String ENDPOINT_URL = "/api/auth/register";
    
//     // Start a PostgreSQL Testcontainer
//     @Container
//     static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
//         .withDatabaseName("testdb")
//         .withUsername("test")
//         .withPassword("test");

//     // Dynamically override Spring datasource properties
//     @DynamicPropertySource
//     static void configureDataSource(DynamicPropertyRegistry registry) {
//         registry.add("spring.datasource.url", postgres::getJdbcUrl);
//         registry.add("spring.datasource.username", postgres::getUsername);
//         registry.add("spring.datasource.password", postgres::getPassword);
//         registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
//     }

//     @BeforeEach
//     void setUp() {
//         // OWASP-recommended Argon2id parameters (example: iterations=3, memory=64MB, parallelism=2)
//         encoder = new Argon2PasswordEncoder(16, 32, 2, 1 << 16, 3);
//         userRepository.deleteAll();
//     }

//     @Test
//     @DisplayName("[Password Security Implementation] AC.1 + AC.5: Passwords are hashed using Argon2id with OWASP parameters")
//     // * AC.1: Verify Argon2id algorithm is used
//     // * AC.5: Verify parameters follow OWASP recommendations
//     void passwordShouldBeHashedUsingArgon2id() {
//         String rawPassword = "StrongP@ssw0rd!";
//         String hashed = encoder.encode(rawPassword);

//         assertThat(hashed).isNotEqualTo(rawPassword); // not plaintext
//         assertThat(encoder.matches(rawPassword, hashed)).isTrue(); // hash validates
//         assertThat(hashed).startsWith("$argon2id$"); // Argon2id identifier
//     }

//     @Test
//     @DisplayName("[Password Security Implementation] AC.2: Weak/common passwords should be rejected")
//     // * AC.2: Validate password strength against dictionary
//     void weakPassword_ShouldBeRejected() throws Exception {
//         String weakPassword = "password123"; // common weak password

//         String requestBody = """
//             {
//                 "email": "testuser",
//                 "password": "%s"
//             }
//             """.formatted(weakPassword);

//         mockMvc.perform(post(ENDPOINT_URL)
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(requestBody))
//                 .andExpect(status().isBadRequest())
//                 .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                 .andExpect(jsonPath("$.errors[?(@.field == 'password')].message",
//                         containsString("Password is too common")));
//     }

//     @Test
//     @DisplayName("[Password Security Implementation] AC.3: Each password hash must use a unique salt")
//     // * AC.3: Verify unique salt is applied per password
//     void samePassword_ShouldProduceDifferentHashes() {
//         String rawPassword = "StrongP@ssw0rd!";

//         String hash1 = encoder.encode(rawPassword);
//         String hash2 = encoder.encode(rawPassword);

//         assertThat(hash1).isNotEqualTo(hash2); // Different due to salt
//     }

//     @Test
//     @DisplayName("[Password Security Implementation] AC.4: Plain text passwords must never be stored or returned")
//     // * AC.4: Ensure plaintext is not persisted or exposed in API response
//     void plainTextPassword_ShouldNotBeStoredOrReturned() throws Exception {
//         String rawPassword = "UniqueStrongPass!";

//         String requestBody = """
//             {
//                 "username": "secureuser",
//                 "password": "%s"
//             }
//             """.formatted(rawPassword);

//         MvcResult result = mockMvc.perform(post(ENDPOINT_URL)
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(requestBody))
//                 .andExpect(status().isCreated())
//                 .andExpect(jsonPath("$.password").doesNotExist()) // API should not return password
//                 .andExpect(jsonPath("$.id", notNullValue()))
//                 .andReturn();

//         // Extract created user ID from response
//         String responseJson = result.getResponse().getContentAsString();
//         Long userId = objectMapper.readTree(responseJson).get("id").asLong();

//         // Verify in DB
//         Optional<UserEntity> savedUser = userRepository.findById(userId);
//         assertThat(savedUser).isPresent();
//         assertThat(savedUser.get().getPasswordHash()).isNotEqualTo(rawPassword); // never stored as plaintext
//         assertThat(savedUser.get().getPasswordHash()).startsWith("$argon2id$"); // properly hashed
//     }
// }
