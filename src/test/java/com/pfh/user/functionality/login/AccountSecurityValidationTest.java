// // ./AccountSecurityValidationTest.java
// /*
//  *  [USER-STORY] Account Security Validation
//  *      **As a** financial institution
//  *      **I want** to validate account status during login
//  *      **So that** only active, compliant accounts can access the system
//  *
//  *      ✅ **Acceptance Criteria with Equivalence Partitions:**
//  *
//  *          * **AC.1:** Locked accounts return `423 Locked` status
//  *              - Valid Partitions (VP):
//  *                  VP.1: Active account → returns 200 OK
//  *              - Invalid Partitions (IP):
//  *                  IP.1: Locked account → returns 423 Locked
//  *
//  *          * **AC.2:** Disabled/suspended accounts return `403 Forbidden`
//  *              - Valid Partitions (VP):
//  *                  VP.1: Active account → returns 200 OK
//  *              - Invalid Partitions (IP):
//  *                  IP.1: Disabled account → returns 403 Forbidden
//  *                  IP.2: Suspended account → returns 403 Forbidden
//  *
//  *          * **AC.3:** Expired accounts return `401 Unauthorized` with account renewal message
//  *              - Valid Partitions (VP):
//  *                  VP.1: Active account → returns 200 OK
//  *              - Invalid Partitions (IP):
//  *                  IP.1: Expired account → returns 401 Unauthorized + renewal message
//  *
//  *          * **AC.4:** Account status checks are logged for audit purposes
//  *              - Valid Partitions (VP):
//  *                  VP.1: Any login attempt → audit log entry created
//  */

// package com.pfh.user.functionality.login;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.pfh.user.dto.auth.LoginRequestDto;
// import com.pfh.user.functionality.abstraction.AbstractIntegrationTest;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.annotation.DirtiesContext;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;

// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest
// @AutoConfigureMockMvc
// @Transactional
// @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// class AccountSecurityValidationTest extends AbstractIntegrationTest{

//     @Autowired
//     private MockMvc mockMvc;

//     @Autowired
//     private ObjectMapper objectMapper;

//     private static final String LOGIN_URL = "/api/auth/login";

//     private LoginRequestDto activeAccount;
//     private LoginRequestDto lockedAccount;
//     private LoginRequestDto disabledAccount;
//     private LoginRequestDto suspendedAccount;
//     private LoginRequestDto expiredAccount;

//     @BeforeEach
//     void setUp() {
//         activeAccount    = new LoginRequestDto("activeUser",    "password123");
//         lockedAccount    = new LoginRequestDto("lockedUser",    "password123");
//         disabledAccount  = new LoginRequestDto("disabledUser",  "password123");
//         suspendedAccount = new LoginRequestDto("suspendedUser", "password123");
//         expiredAccount   = new LoginRequestDto("expiredUser",   "password123");
//     }

//     // --- AC.1 Tests ---

//     @Test
//     @DisplayName("[Account Security Validation] AC.1 - VP.1: Active account returns 200 OK")
//     void ac1vp1_ActiveAccount_ShouldReturn200() throws Exception {
//         // Given
//         var request = activeAccount;

//         // When
//         var result = mockMvc.perform(post(LOGIN_URL)
//             .contentType("application/json")
//             .content(objectMapper.writeValueAsString(request)));

//         // Then
//         result.andExpect(status().isOk());
//     }

//     @Test
//     @DisplayName("[Account Security Validation] AC.1 - IP.1: Locked account returns 423 Locked")
//     void ac1ip1_LockedAccount_ShouldReturn423() throws Exception {
//         // Given
//         var request = lockedAccount;

//         // When
//         var result = mockMvc.perform(post(LOGIN_URL)
//             .contentType("application/json")
//             .content(objectMapper.writeValueAsString(request)));

//         // Then
//         result.andExpect(status().isLocked());
//     }

//     // --- AC.2 Tests ---

//     @Test
//     @DisplayName("[Account Security Validation] AC.2 - IP.1: Disabled account returns 403 Forbidden")
//     void ac2ip1_DisabledAccount_ShouldReturn403() throws Exception {
//         // Given
//         var request = disabledAccount;

//         // When
//         var result = mockMvc.perform(post(LOGIN_URL)
//             .contentType("application/json")
//             .content(objectMapper.writeValueAsString(request)));

//         // Then
//         result.andExpect(status().isForbidden());
//     }

//     @Test
//     @DisplayName("[Account Security Validation] AC.2 - IP.2: Suspended account returns 403 Forbidden")
//     void ac2ip2_SuspendedAccount_ShouldReturn403() throws Exception {
//         // Given
//         var request = suspendedAccount;

//         // When
//         var result = mockMvc.perform(post(LOGIN_URL)
//             .contentType("application/json")
//             .content(objectMapper.writeValueAsString(request)));

//         // Then
//         result.andExpect(status().isForbidden());
//     }

//     // --- AC.3 Tests ---

//     @Test
//     @DisplayName("[Account Security Validation] AC.3 - IP.1: Expired account returns 401 Unauthorized with renewal message")
//     void ac3ip1_ExpiredAccount_ShouldReturn401WithMessage() throws Exception {
//         // Given
//         var request = expiredAccount;

//         // When
//         var result = mockMvc.perform(post(LOGIN_URL)
//             .contentType("application/json")
//             .content(objectMapper.writeValueAsString(request)));

//         // Then
//         result.andExpect(status().isUnauthorized())
//               .andExpect(jsonPath("$.message").value("Your account has expired. Please renew."));
//     }

//     // --- AC.4 Tests ---

//     @Test
//     @DisplayName("[Account Security Validation] AC.4 - VP.1: Account status checks are logged for audit purposes")
//     void ac4vp1_AccountStatusChecksLogged_ShouldBeLogged() throws Exception {
//         // Given
//         var request = activeAccount;

//         // When
//         mockMvc.perform(post(LOGIN_URL)
//             .contentType("application/json")
//             .content(objectMapper.writeValueAsString(request)))
//             .andExpect(status().isOk());

//         // Then
//         // TODO: Verify audit log entry was created
//     }
// }
