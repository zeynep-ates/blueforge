package com.blueforge.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.UpdateRequirementRequest;
import com.blueforge.entity.RequirementType;
import com.blueforge.service.RequirementNotFoundException;
import com.blueforge.service.RequirementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RequirementController.class)
class RequirementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RequirementService requirementService;

    @Test
    void updateRequirementReturnsOkWithBody() throws Exception {
        when(requirementService.updateRequirement(eq(200L), any()))
                .thenReturn(new RequirementResponse(
                        200L, RequirementType.FUNCTIONAL, "Updated title", "Updated description", 0));

        mockMvc.perform(patch("/api/requirements/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateRequirementRequest("Updated title", "Updated description"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(200)))
                .andExpect(jsonPath("$.title", is("Updated title")))
                .andExpect(jsonPath("$.description", is("Updated description")));
    }

    @Test
    void updateRequirementReturnsBadRequestWhenTitleBlank() throws Exception {
        mockMvc.perform(patch("/api/requirements/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRequirementRequest("", "description"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRequirementReturnsNotFoundWhenMissing() throws Exception {
        when(requirementService.updateRequirement(eq(200L), any()))
                .thenThrow(new RequirementNotFoundException(200L));

        mockMvc.perform(patch("/api/requirements/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRequirementRequest("title", "description"))))
                .andExpect(status().isNotFound());
    }
}
