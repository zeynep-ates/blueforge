package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectDetailResponse;
import com.blueforge.dto.ProjectSummaryResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.ProjectVersionSummaryResponse;
import com.blueforge.dto.RegenerateVersionRequest;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.entity.ArchitectureRecommendation;
import com.blueforge.entity.ClarifyingAnswer;
import com.blueforge.entity.ClarifyingQuestion;
import com.blueforge.entity.Epic;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.Requirement;
import com.blueforge.entity.RequirementType;
import com.blueforge.entity.Task;
import com.blueforge.entity.TaskEffort;
import com.blueforge.entity.TaskPriority;
import com.blueforge.entity.UserStory;
import com.blueforge.repository.ProjectRepository;
import com.blueforge.repository.ProjectVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectVersionRepository projectVersionRepository;

    @Mock
    private AiClient aiClient;

    private ProjectService projectService;

    private ProjectService newService() {
        return new ProjectService(projectRepository, projectVersionRepository, aiClient, new ObjectMapper());
    }

    @Test
    void createProjectPersistsProjectAndVersionWithOrderedQuestions() {
        projectService = newService();

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> {
            ProjectVersion v = inv.getArgument(0);
            v.setId(10L);
            long questionId = 100L;
            for (ClarifyingQuestion q : v.getClarifyingQuestions()) {
                q.setId(questionId++);
            }
            return v;
        });
        when(aiClient.complete(any())).thenReturn("""
                ["What is the primary user type?", "Is multi-tenancy required?"]
                """);

        CreateProjectResponse response =
                projectService.createProject(new CreateProjectRequest("Test Project", "An idea"));

        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.versionId()).isEqualTo(10L);
        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).questionText()).isEqualTo("What is the primary user type?");
        assertThat(response.questions().get(0).orderIndex()).isEqualTo(0);
        assertThat(response.questions().get(1).questionText()).isEqualTo("Is multi-tenancy required?");
        assertThat(response.questions().get(1).orderIndex()).isEqualTo(1);
    }

    @Test
    void createProjectThrowsAiResponseParsingExceptionWhenAiReturnsInvalidJson() {
        projectService = newService();

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiClient.complete(any())).thenReturn("not valid json");

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectRequest("Test Project", "An idea")))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void getProjectVersionReturnsStoredVersionWithOrderedQuestions() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(version, "What is the primary user type?", 0);
        q1.setId(100L);
        ClarifyingQuestion q2 = new ClarifyingQuestion(version, "Is multi-tenancy required?", 1);
        q2.setId(101L);
        version.getClarifyingQuestions().addAll(java.util.List.of(q1, q2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        ProjectVersionResponse response = projectService.getProjectVersion(1L, 1);

        assertThat(response.versionId()).isEqualTo(10L);
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.versionNumber()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(ProjectVersionStatus.AWAITING_ANSWERS);
        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).questionText()).isEqualTo("What is the primary user type?");
        assertThat(response.questions().get(1).questionText()).isEqualTo("Is multi-tenancy required?");
    }

    @Test
    void getProjectVersionThrowsNotFoundWhenNoMatchingVersion() {
        projectService = newService();

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectVersion(1L, 1))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void submitAnswersPersistsAnswersAndGeneratedRequirementsAndTransitionsStatus() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(version, "What is the primary user type?", 0);
        q1.setId(100L);
        ClarifyingQuestion q2 = new ClarifyingQuestion(version, "Is multi-tenancy required?", 1);
        q2.setId(101L);
        version.getClarifyingQuestions().addAll(List.of(q1, q2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> {
            ProjectVersion v = inv.getArgument(0);
            long requirementId = 200L;
            for (Requirement r : v.getRequirements()) {
                r.setId(requirementId++);
            }
            return v;
        });
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"type": "FUNCTIONAL", "title": "User registration", "description": "Users can sign up with email."},
                          {"type": "NON_FUNCTIONAL", "title": "Fast response times", "description": "95 percent of requests complete within 500ms."}
                        ]
                        """);

        SubmitAnswersRequest request = new SubmitAnswersRequest(
                List.of(new AnswerRequest(100L, "End consumers"), new AnswerRequest(101L, "Yes")));

        ProjectVersionResponse response = projectService.submitAnswers(1L, 1, request);

        assertThat(response.status()).isEqualTo(ProjectVersionStatus.REQUIREMENTS_GENERATED);
        assertThat(response.questions().get(0).answerText()).isEqualTo("End consumers");
        assertThat(response.questions().get(1).answerText()).isEqualTo("Yes");
        assertThat(response.requirements()).hasSize(2);
        assertThat(response.requirements().get(0).type()).isEqualTo(RequirementType.FUNCTIONAL);
        assertThat(response.requirements().get(0).title()).isEqualTo("User registration");
        assertThat(response.requirements().get(1).type()).isEqualTo(RequirementType.NON_FUNCTIONAL);
    }

    @Test
    void submitAnswersThrowsNotFoundWhenNoMatchingVersion() {
        projectService = newService();

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.submitAnswers(1L, 1, new SubmitAnswersRequest(List.of())))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void submitAnswersThrowsWhenVersionIsNotAwaitingAnswers() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.submitAnswers(1L, 1, new SubmitAnswersRequest(List.of())))
                .isInstanceOf(InvalidProjectVersionStatusException.class);
    }

    @Test
    void submitAnswersThrowsInvalidAnswersWhenAnswerCountDoesNotMatchQuestionCount() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(version, "What is the primary user type?", 0);
        q1.setId(100L);
        version.getClarifyingQuestions().add(q1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.submitAnswers(1L, 1, new SubmitAnswersRequest(List.of())))
                .isInstanceOf(InvalidAnswersException.class);
    }

    @Test
    void submitAnswersThrowsInvalidAnswersWhenQuestionDoesNotBelongToVersion() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(version, "What is the primary user type?", 0);
        q1.setId(100L);
        version.getClarifyingQuestions().add(q1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        SubmitAnswersRequest request = new SubmitAnswersRequest(List.of(new AnswerRequest(999L, "answer")));

        assertThatThrownBy(() -> projectService.submitAnswers(1L, 1, request))
                .isInstanceOf(InvalidAnswersException.class);
    }

    @Test
    void submitAnswersThrowsInvalidAnswersWhenDuplicateAnswerForSameQuestion() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(version, "What is the primary user type?", 0);
        q1.setId(100L);
        ClarifyingQuestion q2 = new ClarifyingQuestion(version, "Is multi-tenancy required?", 1);
        q2.setId(101L);
        version.getClarifyingQuestions().addAll(List.of(q1, q2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        SubmitAnswersRequest request = new SubmitAnswersRequest(
                List.of(new AnswerRequest(100L, "answer one"), new AnswerRequest(100L, "answer two")));

        assertThatThrownBy(() -> projectService.submitAnswers(1L, 1, request))
                .isInstanceOf(InvalidAnswersException.class);
    }

    @Test
    void submitAnswersThrowsAiResponseParsingExceptionWhenAiReturnsInvalidJson() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(version, "What is the primary user type?", 0);
        q1.setId(100L);
        version.getClarifyingQuestions().add(q1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(aiClient.complete(any())).thenReturn("not valid json");

        SubmitAnswersRequest request = new SubmitAnswersRequest(List.of(new AnswerRequest(100L, "answer")));

        assertThatThrownBy(() -> projectService.submitAnswers(1L, 1, request))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void generateEpicsPersistsGeneratedEpicsAndTransitionsStatus() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        version.setId(10L);
        Requirement r1 = new Requirement(
                version, RequirementType.FUNCTIONAL, "User registration", "Users can sign up with email.", 0);
        r1.setId(200L);
        version.getRequirements().add(r1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> {
            ProjectVersion v = inv.getArgument(0);
            long epicId = 300L;
            for (Epic e : v.getEpics()) {
                e.setId(epicId++);
            }
            return v;
        });
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"title": "User onboarding", "description": "Covers account creation and first-time setup."},
                          {"title": "Billing", "description": "Covers plan selection and payment processing."}
                        ]
                        """);

        ProjectVersionResponse response = projectService.generateEpics(1L, 1);

        assertThat(response.status()).isEqualTo(ProjectVersionStatus.EPICS_GENERATED);
        assertThat(response.epics()).hasSize(2);
        assertThat(response.epics().get(0).title()).isEqualTo("User onboarding");
        assertThat(response.epics().get(0).orderIndex()).isEqualTo(0);
        assertThat(response.epics().get(1).title()).isEqualTo("Billing");
        assertThat(response.epics().get(1).orderIndex()).isEqualTo(1);
    }

    @Test
    void generateEpicsThrowsNotFoundWhenNoMatchingVersion() {
        projectService = newService();

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.generateEpics(1L, 1))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void generateEpicsThrowsWhenRequirementsNotYetGenerated() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateEpics(1L, 1))
                .isInstanceOf(InvalidProjectVersionStatusException.class);
    }

    @Test
    void generateEpicsThrowsWhenRequirementsListIsEmpty() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateEpics(1L, 1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateEpicsThrowsAiResponseParsingExceptionWhenAiReturnsInvalidJson() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        version.setId(10L);
        Requirement r1 = new Requirement(
                version, RequirementType.FUNCTIONAL, "User registration", "Users can sign up with email.", 0);
        r1.setId(200L);
        version.getRequirements().add(r1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(aiClient.complete(any())).thenReturn("not valid json");

        assertThatThrownBy(() -> projectService.generateEpics(1L, 1))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void generateUserStoriesPersistsGeneratedUserStoriesAndTransitionsStatus() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        version.setId(10L);
        Epic epic1 = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        Epic epic2 = new Epic(version, "Billing", "Covers payment processing.", 1);
        epic2.setId(301L);
        version.getEpics().addAll(List.of(epic1, epic2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> {
            ProjectVersion v = inv.getArgument(0);
            long storyId = 400L;
            for (Epic e : v.getEpics()) {
                for (UserStory s : e.getUserStories()) {
                    s.setId(storyId++);
                }
            }
            return v;
        });
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"stories": [
                            {"title": "Email sign-up", "description": "As a new user, I want to sign up with my email.", "acceptanceCriteria": "- User can register with email and password"}
                          ]},
                          {"stories": [
                            {"title": "Select a plan", "description": "As a customer, I want to choose a plan.", "acceptanceCriteria": "- Plans are listed with pricing"},
                            {"title": "Enter payment details", "description": "As a customer, I want to enter payment details.", "acceptanceCriteria": "- Card details can be submitted"}
                          ]}
                        ]
                        """);

        ProjectVersionResponse response = projectService.generateUserStories(1L, 1);

        assertThat(response.status()).isEqualTo(ProjectVersionStatus.USER_STORIES_GENERATED);
        assertThat(response.userStories()).hasSize(3);
        assertThat(response.userStories().get(0).epicId()).isEqualTo(300L);
        assertThat(response.userStories().get(0).title()).isEqualTo("Email sign-up");
        assertThat(response.userStories().get(0).orderIndex()).isEqualTo(0);
        assertThat(response.userStories().get(1).epicId()).isEqualTo(301L);
        assertThat(response.userStories().get(1).title()).isEqualTo("Select a plan");
        assertThat(response.userStories().get(1).orderIndex()).isEqualTo(0);
        assertThat(response.userStories().get(2).epicId()).isEqualTo(301L);
        assertThat(response.userStories().get(2).title()).isEqualTo("Enter payment details");
        assertThat(response.userStories().get(2).orderIndex()).isEqualTo(1);
    }

    @Test
    void generateUserStoriesThrowsNotFoundWhenNoMatchingVersion() {
        projectService = newService();

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.generateUserStories(1L, 1))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void generateUserStoriesThrowsWhenEpicsNotYetGenerated() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateUserStories(1L, 1))
                .isInstanceOf(InvalidProjectVersionStatusException.class);
    }

    @Test
    void generateUserStoriesThrowsWhenEpicsListIsEmpty() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateUserStories(1L, 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateUserStoriesThrowsAiResponseParsingExceptionWhenAiReturnsInvalidJson() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        version.setId(10L);
        Epic epic1 = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        version.getEpics().add(epic1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(aiClient.complete(any())).thenReturn("not valid json");

        assertThatThrownBy(() -> projectService.generateUserStories(1L, 1))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void generateUserStoriesThrowsAiResponseParsingExceptionWhenEpicCountMismatch() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        version.setId(10L);
        Epic epic1 = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        Epic epic2 = new Epic(version, "Billing", "Covers payment processing.", 1);
        epic2.setId(301L);
        version.getEpics().addAll(List.of(epic1, epic2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"stories": [
                            {"title": "Email sign-up", "description": "As a new user, I want to sign up.", "acceptanceCriteria": "- User can register"}
                          ]}
                        ]
                        """);

        assertThatThrownBy(() -> projectService.generateUserStories(1L, 1))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void generateTasksPersistsGeneratedTasksAndTransitionsStatus() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.USER_STORIES_GENERATED);
        version.setId(10L);
        Epic epic1 = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        UserStory story1 = new UserStory(
                epic1, "Email sign-up", "As a new user, I want to sign up.", "- User can register", 0);
        story1.setId(400L);
        epic1.getUserStories().add(story1);
        Epic epic2 = new Epic(version, "Billing", "Covers payment processing.", 1);
        epic2.setId(301L);
        UserStory story2 = new UserStory(
                epic2, "Select a plan", "As a customer, I want to choose a plan.", "- Plans are listed", 0);
        story2.setId(401L);
        UserStory story3 = new UserStory(
                epic2,
                "Enter payment details",
                "As a customer, I want to enter payment details.",
                "- Card details can be submitted",
                1);
        story3.setId(402L);
        epic2.getUserStories().addAll(List.of(story2, story3));
        version.getEpics().addAll(List.of(epic1, epic2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> {
            ProjectVersion v = inv.getArgument(0);
            long taskId = 500L;
            for (Epic e : v.getEpics()) {
                for (UserStory s : e.getUserStories()) {
                    for (Task t : s.getTasks()) {
                        t.setId(taskId++);
                    }
                }
            }
            return v;
        });
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"tasks": [
                            {"title": "Add POST /register endpoint", "description": "Implement registration.", "priority": "HIGH", "effortEstimate": "M"}
                          ]}
                        ]
                        """,
                        """
                        [
                          {"tasks": [
                            {"title": "Add plan selection UI", "description": "Build plan selection screen.", "priority": "HIGH", "effortEstimate": "M"}
                          ]},
                          {"tasks": [
                            {"title": "Add payment form", "description": "Build payment entry form.", "priority": "MEDIUM", "effortEstimate": "L"},
                            {"title": "Integrate payment gateway", "description": "Connect to the payment provider API.", "priority": "HIGH", "effortEstimate": "L"}
                          ]}
                        ]
                        """);

        ProjectVersionResponse response = projectService.generateTasks(1L, 1);

        assertThat(response.status()).isEqualTo(ProjectVersionStatus.TASKS_GENERATED);
        assertThat(response.tasks()).hasSize(4);
        assertThat(response.tasks().get(0).userStoryId()).isEqualTo(400L);
        assertThat(response.tasks().get(0).title()).isEqualTo("Add POST /register endpoint");
        assertThat(response.tasks().get(0).priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.tasks().get(0).effortEstimate()).isEqualTo(TaskEffort.M);
        assertThat(response.tasks().get(0).orderIndex()).isEqualTo(0);
        assertThat(response.tasks().get(1).userStoryId()).isEqualTo(401L);
        assertThat(response.tasks().get(1).title()).isEqualTo("Add plan selection UI");
        assertThat(response.tasks().get(1).orderIndex()).isEqualTo(0);
        assertThat(response.tasks().get(2).userStoryId()).isEqualTo(402L);
        assertThat(response.tasks().get(2).title()).isEqualTo("Add payment form");
        assertThat(response.tasks().get(2).orderIndex()).isEqualTo(0);
        assertThat(response.tasks().get(3).userStoryId()).isEqualTo(402L);
        assertThat(response.tasks().get(3).title()).isEqualTo("Integrate payment gateway");
        assertThat(response.tasks().get(3).orderIndex()).isEqualTo(1);

        verify(aiClient, times(2)).complete(any());
    }

    @Test
    void generateTasksThrowsNotFoundWhenNoMatchingVersion() {
        projectService = newService();

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.generateTasks(1L, 1))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void generateTasksThrowsWhenUserStoriesNotYetGenerated() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateTasks(1L, 1))
                .isInstanceOf(InvalidProjectVersionStatusException.class);
    }

    @Test
    void generateTasksThrowsWhenEpicsListIsEmpty() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.USER_STORIES_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateTasks(1L, 1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateTasksThrowsWhenAnyEpicHasNoUserStories() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.USER_STORIES_GENERATED);
        version.setId(10L);
        Epic epic1 = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        UserStory story1 = new UserStory(
                epic1, "Email sign-up", "As a new user, I want to sign up.", "- User can register", 0);
        story1.setId(400L);
        epic1.getUserStories().add(story1);
        Epic epic2 = new Epic(version, "Billing", "Covers payment processing.", 1);
        epic2.setId(301L);
        version.getEpics().addAll(List.of(epic1, epic2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateTasks(1L, 1)).isInstanceOf(IllegalStateException.class);

        verify(aiClient, never()).complete(any());
    }

    @Test
    void generateTasksThrowsAiResponseParsingExceptionWhenAiReturnsInvalidJson() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.USER_STORIES_GENERATED);
        version.setId(10L);
        Epic epic1 = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        UserStory story1 = new UserStory(
                epic1, "Email sign-up", "As a new user, I want to sign up.", "- User can register", 0);
        story1.setId(400L);
        epic1.getUserStories().add(story1);
        version.getEpics().add(epic1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(aiClient.complete(any())).thenReturn("not valid json");

        assertThatThrownBy(() -> projectService.generateTasks(1L, 1))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void generateTasksThrowsAiResponseParsingExceptionWhenUserStoryCountMismatch() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.USER_STORIES_GENERATED);
        version.setId(10L);
        Epic epic1 = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        UserStory story1 = new UserStory(
                epic1, "Email sign-up", "As a new user, I want to sign up.", "- User can register", 0);
        story1.setId(400L);
        UserStory story2 = new UserStory(
                epic1, "Password reset", "As a user, I want to reset my password.", "- User can request reset", 1);
        story2.setId(401L);
        epic1.getUserStories().addAll(List.of(story1, story2));
        version.getEpics().add(epic1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"tasks": [
                            {"title": "Add POST /register endpoint", "description": "Implement registration.", "priority": "HIGH", "effortEstimate": "M"}
                          ]}
                        ]
                        """);

        assertThatThrownBy(() -> projectService.generateTasks(1L, 1))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void listProjectsReturnsSummariesWithLatestVersion() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        project.setCreatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        ProjectVersion v1 = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        v1.setId(10L);
        ProjectVersion v2 = new ProjectVersion(project, 2, "A refined idea", ProjectVersionStatus.AWAITING_ANSWERS);
        v2.setId(11L);

        when(projectRepository.findAll()).thenReturn(List.of(project));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(v1, v2));

        List<ProjectSummaryResponse> response = projectService.listProjects();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(0).name()).isEqualTo("Test Project");
        assertThat(response.get(0).latestVersionNumber()).isEqualTo(2);
        assertThat(response.get(0).latestStatus()).isEqualTo(ProjectVersionStatus.AWAITING_ANSWERS);
    }

    @Test
    void getProjectReturnsDetailWithOrderedVersions() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        project.setCreatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        ProjectVersion v1 = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        v1.setId(10L);
        ProjectVersion v2 = new ProjectVersion(project, 2, "A refined idea", ProjectVersionStatus.AWAITING_ANSWERS);
        v2.setId(11L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(v1, v2));

        ProjectDetailResponse response = projectService.getProject(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test Project");
        assertThat(response.versions()).hasSize(2);
        assertThat(response.versions().get(0).versionNumber()).isEqualTo(1);
        assertThat(response.versions().get(1).versionNumber()).isEqualTo(2);
        assertThat(response.versions().get(1).status()).isEqualTo(ProjectVersionStatus.AWAITING_ANSWERS);
    }

    @Test
    void getProjectThrowsNotFoundWhenMissing() {
        projectService = newService();

        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(1L)).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void listVersionsReturnsOrderedVersionSummaries() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion v1 = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        v1.setId(10L);
        ProjectVersion v2 = new ProjectVersion(project, 2, "A refined idea", ProjectVersionStatus.AWAITING_ANSWERS);
        v2.setId(11L);
        v2.setChangeDescription("Regenerated from v1");

        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(v1, v2));

        List<ProjectVersionSummaryResponse> response = projectService.listVersions(1L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).versionId()).isEqualTo(10L);
        assertThat(response.get(0).changeDescription()).isNull();
        assertThat(response.get(1).versionId()).isEqualTo(11L);
        assertThat(response.get(1).changeDescription()).isEqualTo("Regenerated from v1");
    }

    @Test
    void listVersionsThrowsNotFoundWhenProjectMissing() {
        projectService = newService();

        when(projectRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.listVersions(1L)).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void regenerateVersionForEpicsClonesRequirementsAndLeavesBaseVersionUntouched() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion base = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        base.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(base, "What is the primary user type?", 0);
        q1.setId(100L);
        q1.setAnswer(new ClarifyingAnswer(q1, "End consumers"));
        base.getClarifyingQuestions().add(q1);
        Requirement r1 = new Requirement(
                base, RequirementType.FUNCTIONAL, "User registration", "Users can sign up with email.", 0);
        r1.setId(200L);
        base.getRequirements().add(r1);
        Epic baseEpic = new Epic(base, "User onboarding", "Covers account creation.", 0);
        baseEpic.setId(300L);
        UserStory baseStory = new UserStory(
                baseEpic, "Email sign-up", "As a new user, I want to sign up.", "- User can register", 0);
        baseStory.setId(400L);
        Task baseTask = new Task(
                baseStory, "Add POST /register endpoint", "Implement registration.", TaskPriority.HIGH, TaskEffort.M, 0);
        baseTask.setId(500L);
        baseStory.getTasks().add(baseTask);
        baseEpic.getUserStories().add(baseStory);
        base.getEpics().add(baseEpic);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(base));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(base));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"title": "Different onboarding", "description": "A fresh take on onboarding."}
                        ]
                        """);

        ProjectVersionResponse response = projectService.regenerateVersion(
                1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, "Tried again"));

        assertThat(response.versionNumber()).isEqualTo(2);
        assertThat(response.changeDescription()).isEqualTo("Tried again");
        assertThat(response.status()).isEqualTo(ProjectVersionStatus.EPICS_GENERATED);
        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).answerText()).isEqualTo("End consumers");
        assertThat(response.requirements()).hasSize(1);
        assertThat(response.requirements().get(0).title()).isEqualTo("User registration");
        assertThat(response.epics()).hasSize(1);
        assertThat(response.epics().get(0).title()).isEqualTo("Different onboarding");
        assertThat(response.userStories()).isEmpty();
        assertThat(response.tasks()).isEmpty();

        assertThat(base.getStatus()).isEqualTo(ProjectVersionStatus.TASKS_GENERATED);
        assertThat(base.getEpics()).hasSize(1);
        assertThat(base.getEpics().get(0).getTitle()).isEqualTo("User onboarding");
        assertThat(base.getEpics().get(0).getUserStories()).hasSize(1);
        assertThat(base.getEpics().get(0).getUserStories().get(0).getTasks()).hasSize(1);
    }

    @Test
    void regenerateVersionForRequirementsClonesOnlyQuestionsAndAnswers() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion base = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        base.setId(10L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(base, "What is the primary user type?", 0);
        q1.setId(100L);
        q1.setAnswer(new ClarifyingAnswer(q1, "End consumers"));
        base.getClarifyingQuestions().add(q1);
        Requirement r1 = new Requirement(
                base, RequirementType.FUNCTIONAL, "User registration", "Users can sign up with email.", 0);
        r1.setId(200L);
        base.getRequirements().add(r1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(base));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(base));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"type": "FUNCTIONAL", "title": "Different requirement", "description": "A fresh take."}
                        ]
                        """);

        ProjectVersionResponse response = projectService.regenerateVersion(
                1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.REQUIREMENTS_GENERATED, null));

        assertThat(response.versionNumber()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ProjectVersionStatus.REQUIREMENTS_GENERATED);
        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).answerText()).isEqualTo("End consumers");
        assertThat(response.requirements()).hasSize(1);
        assertThat(response.requirements().get(0).title()).isEqualTo("Different requirement");
    }

    @Test
    void regenerateVersionThrowsWhenTargetStageNotYetReached() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion base = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        base.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(base));

        assertThatThrownBy(() -> projectService.regenerateVersion(
                        1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, null)))
                .isInstanceOf(RegenerationNotAllowedException.class);
    }

    @Test
    void regenerateVersionThrowsWhenTargetStageIsAwaitingAnswers() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion base = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        base.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(base));

        assertThatThrownBy(() -> projectService.regenerateVersion(
                        1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.AWAITING_ANSWERS, null)))
                .isInstanceOf(InvalidRegenerationTargetException.class);
    }

    @Test
    void regenerateVersionThrowsNotFoundWhenNoMatchingVersion() {
        projectService = newService();

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.regenerateVersion(
                        1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, null)))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void regenerateVersionThrowsAiResponseParsingExceptionWhenAiReturnsInvalidJson() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion base = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        base.setId(10L);
        Requirement r1 = new Requirement(
                base, RequirementType.FUNCTIONAL, "User registration", "Users can sign up with email.", 0);
        r1.setId(200L);
        base.getRequirements().add(r1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(base));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(base));
        when(aiClient.complete(any())).thenReturn("not valid json");

        assertThatThrownBy(() -> projectService.regenerateVersion(
                        1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, null)))
                .isInstanceOf(AiResponseParsingException.class);

        verify(projectVersionRepository, never()).save(any());
    }

    @Test
    void regenerateVersionForTasksClonesEpicsAndUserStoriesMatchedByIndexAcrossDifferingStoryCounts() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion base = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        base.setId(10L);

        Epic epic1 = new Epic(base, "User onboarding", "Covers account creation.", 0);
        epic1.setId(300L);
        UserStory epic1Story1 = new UserStory(
                epic1, "Email sign-up", "As a new user, I want to sign up.", "- User can register", 0);
        epic1Story1.setId(400L);
        epic1.getUserStories().add(epic1Story1);

        Epic epic2 = new Epic(base, "Billing", "Covers payment processing.", 1);
        epic2.setId(301L);
        UserStory epic2Story1 =
                new UserStory(epic2, "Select a plan", "As a customer, I want to choose a plan.", "- Plans listed", 0);
        epic2Story1.setId(401L);
        UserStory epic2Story2 = new UserStory(
                epic2, "Enter payment details", "As a customer, I want to pay.", "- Card details submitted", 1);
        epic2Story2.setId(402L);
        epic2.getUserStories().addAll(List.of(epic2Story1, epic2Story2));

        base.getEpics().addAll(List.of(epic1, epic2));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(base));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(base));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"tasks": [
                            {"title": "Add POST /register endpoint", "description": "Implement registration.", "priority": "HIGH", "effortEstimate": "M"}
                          ]}
                        ]
                        """,
                        """
                        [
                          {"tasks": [
                            {"title": "Add plan selection UI", "description": "Build plan selection screen.", "priority": "HIGH", "effortEstimate": "M"}
                          ]},
                          {"tasks": [
                            {"title": "Add payment form", "description": "Build payment entry form.", "priority": "MEDIUM", "effortEstimate": "L"}
                          ]}
                        ]
                        """);

        ProjectVersionResponse response = projectService.regenerateVersion(
                1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.TASKS_GENERATED, null));

        assertThat(response.versionNumber()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ProjectVersionStatus.TASKS_GENERATED);
        assertThat(response.epics()).hasSize(2);
        assertThat(response.epics().get(0).title()).isEqualTo("User onboarding");
        assertThat(response.epics().get(1).title()).isEqualTo("Billing");
        assertThat(response.userStories()).hasSize(3);
        assertThat(response.userStories()).extracting("title")
                .containsExactly("Email sign-up", "Select a plan", "Enter payment details");
        // Cloned user stories must reference the cloned epics (new ids), not the base version's original epic ids.
        assertThat(response.userStories().get(0).epicId()).isEqualTo(response.epics().get(0).id());
        assertThat(response.userStories().get(1).epicId()).isEqualTo(response.epics().get(1).id());
        assertThat(response.userStories().get(2).epicId()).isEqualTo(response.epics().get(1).id());
        assertThat(response.tasks()).hasSize(3);
        assertThat(response.tasks()).extracting("title")
                .containsExactly("Add POST /register endpoint", "Add plan selection UI", "Add payment form");

        assertThat(base.getEpics().get(1).getUserStories()).hasSize(2);
    }

    @Test
    void regenerateVersionAssignsNextVersionNumberAfterExistingVersions() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion v1 = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        v1.setId(10L);
        ProjectVersion v2 = new ProjectVersion(project, 2, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        v2.setId(11L);
        ProjectVersion v3 = new ProjectVersion(project, 3, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        v3.setId(12L);
        ClarifyingQuestion q1 = new ClarifyingQuestion(v3, "What is the primary user type?", 0);
        q1.setId(100L);
        q1.setAnswer(new ClarifyingAnswer(q1, "End consumers"));
        v3.getClarifyingQuestions().add(q1);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 3)).thenReturn(Optional.of(v3));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(v1, v2, v3));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"type": "FUNCTIONAL", "title": "Fresh requirement", "description": "A fresh take."}
                        ]
                        """);

        ProjectVersionResponse response = projectService.regenerateVersion(
                1L, 3, new RegenerateVersionRequest(ProjectVersionStatus.REQUIREMENTS_GENERATED, null));

        assertThat(response.versionNumber()).isEqualTo(4);
    }

    @Test
    void generateArchitectureRecommendationsPersistsGeneratedRecommendationsAndTransitionsStatus() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        version.setId(10L);
        Epic epic = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic.setId(300L);
        version.getEpics().add(epic);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> {
            ProjectVersion v = inv.getArgument(0);
            long recommendationId = 600L;
            for (ArchitectureRecommendation a : v.getArchitectureRecommendations()) {
                a.setId(recommendationId++);
            }
            return v;
        });
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"component": "Backend Framework", "recommendation": "Spring Boot", "reasoning": "Fits the layered requirements.", "tradeoffs": "Serverless was considered but rejected."},
                          {"component": "Database", "recommendation": "PostgreSQL", "reasoning": "Relational domain.", "tradeoffs": "MongoDB was considered but rejected."}
                        ]
                        """);

        ProjectVersionResponse response = projectService.generateArchitectureRecommendations(1L, 1);

        assertThat(response.status()).isEqualTo(ProjectVersionStatus.ARCHITECTURE_GENERATED);
        assertThat(response.architectureRecommendations()).hasSize(2);
        assertThat(response.architectureRecommendations().get(0).component()).isEqualTo("Backend Framework");
        assertThat(response.architectureRecommendations().get(0).orderIndex()).isEqualTo(0);
        assertThat(response.architectureRecommendations().get(1).component()).isEqualTo("Database");
        assertThat(response.architectureRecommendations().get(1).orderIndex()).isEqualTo(1);
    }

    @Test
    void generateArchitectureRecommendationsThrowsNotFoundWhenNoMatchingVersion() {
        projectService = newService();

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.generateArchitectureRecommendations(1L, 1))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void generateArchitectureRecommendationsThrowsWhenTasksNotYetGenerated() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.USER_STORIES_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateArchitectureRecommendations(1L, 1))
                .isInstanceOf(InvalidProjectVersionStatusException.class);
    }

    @Test
    void generateArchitectureRecommendationsThrowsWhenEpicsListIsEmpty() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> projectService.generateArchitectureRecommendations(1L, 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateArchitectureRecommendationsThrowsAiResponseParsingExceptionWhenAiReturnsInvalidJson() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        version.setId(10L);
        Epic epic = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic.setId(300L);
        version.getEpics().add(epic);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(aiClient.complete(any())).thenReturn("not valid json");

        assertThatThrownBy(() -> projectService.generateArchitectureRecommendations(1L, 1))
                .isInstanceOf(AiResponseParsingException.class);
    }

    @Test
    void regenerateVersionForArchitectureClonesTasksAndLeavesBaseVersionUntouched() {
        projectService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion base = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.ARCHITECTURE_GENERATED);
        base.setId(10L);
        Epic baseEpic = new Epic(base, "User onboarding", "Covers account creation.", 0);
        baseEpic.setId(300L);
        UserStory baseStory = new UserStory(
                baseEpic, "Email sign-up", "As a new user, I want to sign up.", "- User can register", 0);
        baseStory.setId(400L);
        Task baseTask = new Task(
                baseStory, "Add POST /register endpoint", "Implement registration.", TaskPriority.HIGH, TaskEffort.M, 0);
        baseTask.setId(500L);
        baseStory.getTasks().add(baseTask);
        baseEpic.getUserStories().add(baseStory);
        base.getEpics().add(baseEpic);
        ArchitectureRecommendation baseRecommendation = new ArchitectureRecommendation(
                base, "Database", "MySQL", "An earlier take.", "Considered PostgreSQL.", 0);
        baseRecommendation.setId(700L);
        base.getArchitectureRecommendations().add(baseRecommendation);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(base));
        when(projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(1L)).thenReturn(List.of(base));
        when(projectVersionRepository.save(any(ProjectVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiClient.complete(any()))
                .thenReturn(
                        """
                        [
                          {"component": "Backend Framework", "recommendation": "Spring Boot", "reasoning": "Fits.", "tradeoffs": "Alternative rejected."}
                        ]
                        """);

        ProjectVersionResponse response = projectService.regenerateVersion(
                1L, 1, new RegenerateVersionRequest(ProjectVersionStatus.ARCHITECTURE_GENERATED, null));

        assertThat(response.versionNumber()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ProjectVersionStatus.ARCHITECTURE_GENERATED);
        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().get(0).title()).isEqualTo("Add POST /register endpoint");
        assertThat(response.architectureRecommendations()).hasSize(1);
        assertThat(response.architectureRecommendations().get(0).component()).isEqualTo("Backend Framework");

        assertThat(base.getStatus()).isEqualTo(ProjectVersionStatus.ARCHITECTURE_GENERATED);
        assertThat(base.getArchitectureRecommendations()).hasSize(1);
        assertThat(base.getArchitectureRecommendations().get(0).getComponent()).isEqualTo("Database");
    }
}
