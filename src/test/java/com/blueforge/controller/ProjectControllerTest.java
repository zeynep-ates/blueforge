package com.blueforge.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blueforge.ai.AiClientException;
import com.blueforge.dto.ClarifyingQuestionResponse;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.service.ProjectService;
import com.blueforge.service.ProjectVersionNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void createProjectReturnsOkWithBody() throws Exception {
        when(projectService.createProject(any()))
                .thenReturn(new CreateProjectResponse(
                        1L, 10L, List.of(new ClarifyingQuestionResponse(100L, "What is the primary user type?", 0))));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Test Project", "An idea"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId", is(1)))
                .andExpect(jsonPath("$.versionId", is(10)))
                .andExpect(jsonPath("$.questions[0].questionText", is("What is the primary user type?")));
    }

    @Test
    void createProjectReturnsBadRequestWhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("", "An idea"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProjectReturnsBadGatewayWhenAiClientFails() throws Exception {
        when(projectService.createProject(any())).thenThrow(new AiClientException("boom"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Test Project", "An idea"))))
                .andExpect(status().isBadGateway());
    }

    @Test
    void getProjectVersionReturnsOkWithBody() throws Exception {
        when(projectService.getProjectVersion(eq(1L), eq(1)))
                .thenReturn(new ProjectVersionResponse(
                        10L,
                        1L,
                        1,
                        "An idea",
                        null,
                        ProjectVersionStatus.AWAITING_ANSWERS,
                        List.of(new ClarifyingQuestionResponse(100L, "What is the primary user type?", 0))));

        mockMvc.perform(get("/api/projects/1/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionId", is(10)))
                .andExpect(jsonPath("$.projectId", is(1)))
                .andExpect(jsonPath("$.status", is("AWAITING_ANSWERS")))
                .andExpect(jsonPath("$.questions[0].questionText", is("What is the primary user type?")));
    }

    @Test
    void getProjectVersionReturnsNotFoundWhenMissing() throws Exception {
        when(projectService.getProjectVersion(eq(1L), eq(1))).thenThrow(new ProjectVersionNotFoundException(1L, 1));

        mockMvc.perform(get("/api/projects/1/versions/1")).andExpect(status().isNotFound());
    }
}
