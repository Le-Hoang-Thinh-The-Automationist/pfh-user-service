// // ./ConfigurationPropertiesStatusTest.java
// /*
//  *  [USER-STORY] Configuration Properties Status
//  *      **As a** DevOps engineer
//  *      **I want** to verify that application configuration properties are loaded correctly
//  *      **So that** I can ensure proper deployment settings across environments
//  *
//  *      ✅ **Acceptance Criteria:**
//  *
//  *          * **AC.1:** `/actuator/env` endpoint is enabled (secured).
//  *          * **AC.2:** Returns application-level configs (masked for sensitive values).
//  *          * **AC.3:** Accessible only by authorized roles (e.g., `ADMIN`).
//  *
//  */
// package com.pfh.user.functionality.infrastructure;

// import org.junit.jupiter.api.Test;
// import static org.junit.jupiter.api.Assertions.fail;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.web.servlet.MockMvc;

// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest
// @AutoConfigureMockMvc
// class ConfigurationPropertiesStatusTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @Test
//     // * AC.1: `/actuator/env` endpoint is enabled (secured).
//     // * AC.2: Returns application-level configs (masked for sensitive values).
//     void whenAdminAccessesEnvEndpoint_thenStatus200AndConfigsMasked() throws Exception {
//         // mockMvc.perform(get("/actuator/env")
//         //         .with(user("admin").roles("ADMIN"))) // simulating ADMIN role
//         //     .andExpect(status().isOk())
//         //     .andExpect(jsonPath("$.propertySources").exists())
//         //     .andExpect(jsonPath("$.propertySources[0].properties.*.value"));

//         fail("Test failed intentionally: Need to check on hơw to add ADMIN role");
//         fail("Test failed intentionally: Need to check what data should be masked in add in this test later");
//     }

//     @Test
//     // * AC.3: Accessible only by authorized roles (e.g., `ADMIN`).
//     void whenUnauthorizedUserAccessesEnvEndpoint_thenStatus401() throws Exception {
//         mockMvc.perform(get("/actuator/env"))
//             .andExpect(status().isUnauthorized());
//     }
// }
