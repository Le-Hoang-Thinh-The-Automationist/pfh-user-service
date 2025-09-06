// ./[UserStoryName]Test.java
/*
 *  [USER-STORY] [User Story Name]
 *      **As a** [actor/role]
 *      **I want** [action/goal]
 *      **So that** [business value/outcome]
 *
 *      ✅ **Acceptance Criteria with Equivalence Partitions:**
 *
 *          * **AC.1:** [First acceptance criterion]
 *              - Valid Partitions (VP):
 *                  VP.1: [Description of valid partition 1]
 *                  VP.2: [Description of valid partition 2]
 *              - Invalid Partitions (IP):
 *                  IP.1: [Description of invalid partition 1]
 *                  IP.2: [Description of invalid partition 2]
 *
 *          * **AC.2:** [Second acceptance criterion]
 *              - Valid Partitions (VP):
 *                  VP.1: ...
 *              - Invalid Partitions (IP):
 *                  IP.1: ...
 *
 */

package [your.package.structure];

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class [UserStoryName]Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ENDPOINT_URL = "/api/your/endpoint";

    private YourRequestDto validRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        // validRequest = YourRequestDto.builder().field1("validValue1").field2("validValue2").build();
    }

    // --- AC.1 Tests ---

    @Test
    @DisplayName("[User Story Name] AC.1 - VP.1: Accept valid input scenario 1")
    void ac1vp1_ValidScenario1_ShouldReturn201() throws Exception {
        // Given
        YourRequestDto request = YourRequestDto.builder()
            .field1("validValue1")
            .field2("validValue2")
            .build();

        // When
        var result = mockMvc.perform(post(ENDPOINT_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isCreated())
              .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("[User Story Name] AC.1 - IP.1: Reject when field1 is empty")
    void ac1ip1_Field1Empty_ShouldReturn400() throws Exception {
        // Given
        YourRequestDto request = YourRequestDto.builder()
            .field1("")
            .field2("validValue2")
            .build();

        // When
        var result = mockMvc.perform(post(ENDPOINT_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.errors.field1").exists());
    }

    // --- AC.2 Tests ---
    @Test
    @DisplayName("[User Story Name] AC.2 - VP.1: Accept valid variation")
    void ac2vp1_ValidVariation_ShouldReturn201() throws Exception {
        // Given
        YourRequestDto request = YourRequestDto.builder()
            .field1("variationValue")
            .field2("validValue2")
            .build();

        // When
        var result = mockMvc.perform(post(ENDPOINT_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isCreated())
              .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("[User Story Name] AC.2 - IP.1: Reject when required field missing")
    void ac2ip1_MissingField_ShouldReturn400() throws Exception {
        // Given
        YourRequestDto request = YourRequestDto.builder()
            // field1 is missing
            .field2("validValue2")
            .build();

        // When
        var result = mockMvc.perform(post(ENDPOINT_URL)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.errors.field1").value("Field1 is required"));
    }
}
