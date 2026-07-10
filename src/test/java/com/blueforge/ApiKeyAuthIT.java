package com.blueforge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// ApiKeyAuthFilterTest exercises the filter class in isolation; this proves Spring Boot actually
// registers and applies it in the real filter chain once blueforge.security.api-key is configured.
@SpringBootTest(properties = "blueforge.security.api-key=test-secret-key")
@AutoConfigureMockMvc
class ApiKeyAuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestWithWrongApiKey() throws Exception {
        mockMvc.perform(get("/api/projects").header("X-API-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsRequestWithCorrectApiKey() throws Exception {
        mockMvc.perform(get("/api/projects").header("X-API-Key", "test-secret-key"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsOpenApiDocsWithoutApiKey() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
