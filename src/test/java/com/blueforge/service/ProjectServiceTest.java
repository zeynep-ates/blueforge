package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.entity.ClarifyingQuestion;
import com.blueforge.entity.Epic;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.Requirement;
import com.blueforge.entity.RequirementType;
import com.blueforge.entity.UserStory;
import com.blueforge.repository.ProjectRepository;
import com.blueforge.repository.ProjectVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
