package com.blueforge.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blueforge.ai.AiClientException;
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.ClarifyingQuestionResponse;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.ProjectDetailResponse;
import com.blueforge.dto.ProjectSummaryResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.ProjectVersionSummaryResponse;
import com.blueforge.dto.DiffSummary;
import com.blueforge.dto.RegenerateVersionRequest;
import com.blueforge.dto.RequirementDiffEntry;
import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.dto.TaskResponse;
import com.blueforge.dto.UserStoryResponse;
import com.blueforge.dto.VersionDiffResponse;
import com.blueforge.entity.ChangeType;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.RequirementType;
import com.blueforge.entity.TaskEffort;
import com.blueforge.entity.TaskPriority;
import com.blueforge.service.ExportedMarkdown;
import com.blueforge.service.InvalidAnswersException;
import com.blueforge.service.InvalidProjectVersionStatusException;
import com.blueforge.service.InvalidRegenerationTargetException;
import com.blueforge.service.MarkdownExportService;
import com.blueforge.service.ProjectNotFoundException;
import com.blueforge.service.ProjectService;
import com.blueforge.service.ProjectVersionNotFoundException;
import com.blueforge.service.RegenerationNotAllowedException;
import com.blueforge.service.UnsupportedExportFormatException;
import com.blueforge.service.VersionDiffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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

    @MockitoBean
    private VersionDiffService versionDiffService;

    @MockitoBean
    private MarkdownExportService markdownExportService;

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
                        List.of(),
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
                        List.of(),
                        List.of(),
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
                        List.of(new EpicResponse(300L, "User onboarding", "Covers account creation.", 0)),
                        List.of(),
                        List.of()));

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

    @Test
    void generateUserStoriesReturnsOkWithBody() throws Exception {
        when(projectService.generateUserStories(eq(1L), eq(1)))
                .thenReturn(new ProjectVersionResponse(
                        10L,
                        1L,
                        1,
                        "An idea",
                        null,
                        ProjectVersionStatus.USER_STORIES_GENERATED,
                        List.of(),
                        List.of(),
                        List.of(new EpicResponse(300L, "User onboarding", "Covers account creation.", 0)),
                        List.of(new UserStoryResponse(
                                400L,
                                300L,
                                "Email sign-up",
                                "As a new user, I want to sign up with my email.",
                                "- User can register with email and password",
                                0)),
                        List.of()));

        mockMvc.perform(post("/api/projects/1/versions/1/user-stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("USER_STORIES_GENERATED")))
                .andExpect(jsonPath("$.userStories[0].title", is("Email sign-up")));
    }

    @Test
    void generateUserStoriesReturnsNotFoundWhenMissing() throws Exception {
        when(projectService.generateUserStories(eq(1L), eq(1))).thenThrow(new ProjectVersionNotFoundException(1L, 1));

        mockMvc.perform(post("/api/projects/1/versions/1/user-stories")).andExpect(status().isNotFound());
    }

    @Test
    void generateUserStoriesReturnsConflictWhenEpicsNotYetGenerated() throws Exception {
        when(projectService.generateUserStories(eq(1L), eq(1)))
                .thenThrow(new InvalidProjectVersionStatusException(
                        1L, 1, ProjectVersionStatus.EPICS_GENERATED, ProjectVersionStatus.REQUIREMENTS_GENERATED));

        mockMvc.perform(post("/api/projects/1/versions/1/user-stories")).andExpect(status().isConflict());
    }

    @Test
    void generateUserStoriesReturnsBadGatewayWhenAiClientFails() throws Exception {
        when(projectService.generateUserStories(eq(1L), eq(1))).thenThrow(new AiClientException("boom"));

        mockMvc.perform(post("/api/projects/1/versions/1/user-stories")).andExpect(status().isBadGateway());
    }

    @Test
    void generateTasksReturnsOkWithBody() throws Exception {
        when(projectService.generateTasks(eq(1L), eq(1)))
                .thenReturn(new ProjectVersionResponse(
                        10L,
                        1L,
                        1,
                        "An idea",
                        null,
                        ProjectVersionStatus.TASKS_GENERATED,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new TaskResponse(
                                500L,
                                400L,
                                "Add POST /register endpoint",
                                "Implement the registration endpoint.",
                                TaskPriority.HIGH,
                                TaskEffort.M,
                                0))));

        mockMvc.perform(post("/api/projects/1/versions/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("TASKS_GENERATED")))
                .andExpect(jsonPath("$.tasks[0].title", is("Add POST /register endpoint")));
    }

    @Test
    void generateTasksReturnsNotFoundWhenMissing() throws Exception {
        when(projectService.generateTasks(eq(1L), eq(1))).thenThrow(new ProjectVersionNotFoundException(1L, 1));

        mockMvc.perform(post("/api/projects/1/versions/1/tasks")).andExpect(status().isNotFound());
    }

    @Test
    void generateTasksReturnsConflictWhenUserStoriesNotYetGenerated() throws Exception {
        when(projectService.generateTasks(eq(1L), eq(1)))
                .thenThrow(new InvalidProjectVersionStatusException(
                        1L, 1, ProjectVersionStatus.USER_STORIES_GENERATED, ProjectVersionStatus.EPICS_GENERATED));

        mockMvc.perform(post("/api/projects/1/versions/1/tasks")).andExpect(status().isConflict());
    }

    @Test
    void generateTasksReturnsBadGatewayWhenAiClientFails() throws Exception {
        when(projectService.generateTasks(eq(1L), eq(1))).thenThrow(new AiClientException("boom"));

        mockMvc.perform(post("/api/projects/1/versions/1/tasks")).andExpect(status().isBadGateway());
    }

    @Test
    void regenerateVersionReturnsOkWithBody() throws Exception {
        when(projectService.regenerateVersion(eq(1L), eq(1), any()))
                .thenReturn(new ProjectVersionResponse(
                        11L,
                        1L,
                        2,
                        "An idea",
                        "Tried again",
                        ProjectVersionStatus.EPICS_GENERATED,
                        List.of(),
                        List.of(new RequirementResponse(
                                200L, RequirementType.FUNCTIONAL, "User registration", "Users can sign up.", 0)),
                        List.of(new EpicResponse(301L, "Different onboarding", "A fresh take.", 0)),
                        List.of(),
                        List.of()));

        mockMvc.perform(post("/api/projects/1/versions/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, "Tried again"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber", is(2)))
                .andExpect(jsonPath("$.changeDescription", is("Tried again")))
                .andExpect(jsonPath("$.status", is("EPICS_GENERATED")))
                .andExpect(jsonPath("$.epics[0].title", is("Different onboarding")));
    }

    @Test
    void regenerateVersionReturnsNotFoundWhenMissing() throws Exception {
        when(projectService.regenerateVersion(eq(1L), eq(1), any()))
                .thenThrow(new ProjectVersionNotFoundException(1L, 1));

        mockMvc.perform(post("/api/projects/1/versions/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void regenerateVersionReturnsConflictWhenStageNotYetReached() throws Exception {
        when(projectService.regenerateVersion(eq(1L), eq(1), any()))
                .thenThrow(new RegenerationNotAllowedException(
                        1L, 1, ProjectVersionStatus.EPICS_GENERATED, ProjectVersionStatus.AWAITING_ANSWERS));

        mockMvc.perform(post("/api/projects/1/versions/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, null))))
                .andExpect(status().isConflict());
    }

    @Test
    void regenerateVersionReturnsBadRequestWhenTargetStageNotRegenerable() throws Exception {
        when(projectService.regenerateVersion(eq(1L), eq(1), any()))
                .thenThrow(new InvalidRegenerationTargetException(ProjectVersionStatus.AWAITING_ANSWERS));

        mockMvc.perform(post("/api/projects/1/versions/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegenerateVersionRequest(ProjectVersionStatus.AWAITING_ANSWERS, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void regenerateVersionReturnsBadRequestWhenTargetStageMissing() throws Exception {
        mockMvc.perform(post("/api/projects/1/versions/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getVersionDiffReturnsOkWithBody() throws Exception {
        when(versionDiffService.diff(eq(1L), eq(1), eq(2)))
                .thenReturn(new VersionDiffResponse(
                        1L,
                        1,
                        2,
                        new DiffSummary(1, 0, 0, 1),
                        List.of(
                                new RequirementDiffEntry(
                                        ChangeType.UNCHANGED,
                                        new RequirementResponse(
                                                200L, RequirementType.FUNCTIONAL, "Same", "Same", 0),
                                        new RequirementResponse(
                                                300L, RequirementType.FUNCTIONAL, "Same", "Same", 0)),
                                new RequirementDiffEntry(
                                        ChangeType.ADDED,
                                        null,
                                        new RequirementResponse(
                                                301L, RequirementType.FUNCTIONAL, "New req", "Description", 1))),
                        List.of()));

        mockMvc.perform(get("/api/projects/1/versions/1/diff/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromVersionNumber", is(1)))
                .andExpect(jsonPath("$.toVersionNumber", is(2)))
                .andExpect(jsonPath("$.summary.addedCount", is(1)))
                .andExpect(jsonPath("$.summary.unchangedCount", is(1)))
                .andExpect(jsonPath("$.requirements[1].changeType", is("ADDED")))
                .andExpect(jsonPath("$.requirements[1].after.title", is("New req")));
    }

    @Test
    void getVersionDiffReturnsNotFoundWhenFromVersionMissing() throws Exception {
        when(versionDiffService.diff(eq(1L), eq(1), eq(2))).thenThrow(new ProjectVersionNotFoundException(1L, 1));

        mockMvc.perform(get("/api/projects/1/versions/1/diff/2")).andExpect(status().isNotFound());
    }

    @Test
    void listProjectsReturnsOkWithBody() throws Exception {
        when(projectService.listProjects())
                .thenReturn(List.of(new ProjectSummaryResponse(
                        1L, "Test Project", Instant.parse("2026-07-01T00:00:00Z"), 1,
                        ProjectVersionStatus.AWAITING_ANSWERS)));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Test Project")))
                .andExpect(jsonPath("$[0].latestVersionNumber", is(1)))
                .andExpect(jsonPath("$[0].latestStatus", is("AWAITING_ANSWERS")));
    }

    @Test
    void getProjectReturnsOkWithBody() throws Exception {
        when(projectService.getProject(eq(1L)))
                .thenReturn(new ProjectDetailResponse(
                        1L,
                        "Test Project",
                        Instant.parse("2026-07-01T00:00:00Z"),
                        List.of(new ProjectVersionSummaryResponse(10L, 1, ProjectVersionStatus.AWAITING_ANSWERS, null))));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Project")))
                .andExpect(jsonPath("$.versions[0].versionId", is(10)))
                .andExpect(jsonPath("$.versions[0].versionNumber", is(1)));
    }

    @Test
    void getProjectReturnsNotFoundWhenMissing() throws Exception {
        when(projectService.getProject(eq(1L))).thenThrow(new ProjectNotFoundException(1L));

        mockMvc.perform(get("/api/projects/1")).andExpect(status().isNotFound());
    }

    @Test
    void listVersionsReturnsOkWithBody() throws Exception {
        when(projectService.listVersions(eq(1L)))
                .thenReturn(List.of(
                        new ProjectVersionSummaryResponse(10L, 1, ProjectVersionStatus.AWAITING_ANSWERS, null),
                        new ProjectVersionSummaryResponse(
                                11L, 2, ProjectVersionStatus.TASKS_GENERATED, "Regenerated from v1")));

        mockMvc.perform(get("/api/projects/1/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber", is(1)))
                .andExpect(jsonPath("$[1].versionNumber", is(2)))
                .andExpect(jsonPath("$[1].status", is("TASKS_GENERATED")))
                .andExpect(jsonPath("$[1].changeDescription", is("Regenerated from v1")));
    }

    @Test
    void listVersionsReturnsNotFoundWhenProjectMissing() throws Exception {
        when(projectService.listVersions(eq(1L))).thenThrow(new ProjectNotFoundException(1L));

        mockMvc.perform(get("/api/projects/1/versions")).andExpect(status().isNotFound());
    }

    @Test
    void exportVersionReturnsMarkdownWithDownloadHeaders() throws Exception {
        when(markdownExportService.export(eq(1L), eq(1)))
                .thenReturn(new ExportedMarkdown("test-project-v1.md", "# Test Project\n\n**Version:** 1\n\n"));

        mockMvc.perform(get("/api/projects/1/versions/1/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test-project-v1.md\""))
                .andExpect(content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(content().string("# Test Project\n\n**Version:** 1\n\n"));
    }

    @Test
    void exportVersionReturnsNotFoundWhenMissing() throws Exception {
        when(markdownExportService.export(eq(1L), eq(1))).thenThrow(new ProjectVersionNotFoundException(1L, 1));

        mockMvc.perform(get("/api/projects/1/versions/1/export")).andExpect(status().isNotFound());
    }

    @Test
    void exportVersionReturnsBadRequestForUnsupportedFormat() throws Exception {
        mockMvc.perform(get("/api/projects/1/versions/1/export").param("format", "json"))
                .andExpect(status().isBadRequest());
    }
}
