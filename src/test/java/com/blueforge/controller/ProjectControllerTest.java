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
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.ClarifyingQuestionResponse;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.RequirementType;
import com.blueforge.service.InvalidAnswersException;
import com.blueforge.service.InvalidProjectVersionStatusException;
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
                        1L,
                        10L,
                        List.of(new ClarifyingQuestionResponse(100L, "What is the primary user type?", 0, null))));

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
                        List.of(new ClarifyingQuestionResponse(100L, "What is the primary user type?", 0, null)),
                        List.of(),
                        List.of()));

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

    @Test
    void submitAnswersReturnsOkWithBody() throws Exception {
        when(projectService.submitAnswers(eq(1L), eq(1), any()))
                .thenReturn(new ProjectVersionResponse(
                        10L,
                        1L,
                        1,
                        "An idea",
                        null,
                        ProjectVersionStatus.REQUIREMENTS_GENERATED,
                        List.of(new ClarifyingQuestionResponse(
                                100L, "What is the primary user type?", 0, "End consumers")),
                        List.of(new RequirementResponse(
                                200L, RequirementType.FUNCTIONAL, "User registration", "Users can sign up.", 0)),
                        List.of()));

        mockMvc.perform(post("/api/projects/1/versions/1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitAnswersRequest(List.of(new AnswerRequest(100L, "End consumers"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REQUIREMENTS_GENERATED")))
                .andExpect(jsonPath("$.questions[0].answerText", is("End consumers")))
                .andExpect(jsonPath("$.requirements[0].title", is("User registration")));
    }

    @Test
    void submitAnswersReturnsBadRequestWhenAnswersEmpty() throws Exception {
        mockMvc.perform(post("/api/projects/1/versions/1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitAnswersReturnsBadRequestWhenAnswersInvalid() throws Exception {
        when(projectService.submitAnswers(eq(1L), eq(1), any()))
                .thenThrow(new InvalidAnswersException("Question 999 does not belong to this project version"));

        mockMvc.perform(post("/api/projects/1/versions/1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitAnswersRequest(List.of(new AnswerRequest(999L, "answer"))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitAnswersReturnsConflictWhenVersionNotAwaitingAnswers() throws Exception {
        when(projectService.submitAnswers(eq(1L), eq(1), any()))
                .thenThrow(new InvalidProjectVersionStatusException(
                        1L, 1, ProjectVersionStatus.AWAITING_ANSWERS, ProjectVersionStatus.REQUIREMENTS_GENERATED));

        mockMvc.perform(post("/api/projects/1/versions/1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitAnswersRequest(List.of(new AnswerRequest(100L, "answer"))))))
                .andExpect(status().isConflict());
    }

    @Test
    void generateEpicsReturnsOkWithBody() throws Exception {
        when(projectService.generateEpics(eq(1L), eq(1)))
                .thenReturn(new ProjectVersionResponse(
                        10L,
                        1L,
                        1,
                        "An idea",
                        null,
                        ProjectVersionStatus.EPICS_GENERATED,
                        List.of(),
                        List.of(new RequirementResponse(
                                200L, RequirementType.FUNCTIONAL, "User registration", "Users can sign up.", 0)),
                        List.of(new EpicResponse(300L, "User onboarding", "Covers account creation.", 0))));

        mockMvc.perform(post("/api/projects/1/versions/1/epics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EPICS_GENERATED")))
                .andExpect(jsonPath("$.epics[0].title", is("User onboarding")));
    }

    @Test
    void generateEpicsReturnsNotFoundWhenMissing() throws Exception {
        when(projectService.generateEpics(eq(1L), eq(1))).thenThrow(new ProjectVersionNotFoundException(1L, 1));

        mockMvc.perform(post("/api/projects/1/versions/1/epics")).andExpect(status().isNotFound());
    }

    @Test
    void generateEpicsReturnsConflictWhenRequirementsNotYetGenerated() throws Exception {
        when(projectService.generateEpics(eq(1L), eq(1)))
                .thenThrow(new InvalidProjectVersionStatusException(
                        1L, 1, ProjectVersionStatus.REQUIREMENTS_GENERATED, ProjectVersionStatus.AWAITING_ANSWERS));

        mockMvc.perform(post("/api/projects/1/versions/1/epics")).andExpect(status().isConflict());
    }

    @Test
    void generateEpicsReturnsBadGatewayWhenAiClientFails() throws Exception {
        when(projectService.generateEpics(eq(1L), eq(1))).thenThrow(new AiClientException("boom"));

        mockMvc.perform(post("/api/projects/1/versions/1/epics")).andExpect(status().isBadGateway());
    }
}
