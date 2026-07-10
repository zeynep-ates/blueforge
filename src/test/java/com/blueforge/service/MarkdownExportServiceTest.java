package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
import com.blueforge.repository.ProjectVersionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarkdownExportServiceTest {

    @Mock
    private ProjectVersionRepository projectVersionRepository;

    private MarkdownExportService markdownExportService;

    @BeforeEach
    void setUp() {
        markdownExportService = new MarkdownExportService(projectVersionRepository);
    }

    private Project project(String name) {
        Project project = new Project(name);
        project.setId(1L);
        return project;
    }

    @Test
    void exportsIdeaAndQuestionsWhenNothingElseGeneratedYet() {
        Project project = project("Test Project");
        ProjectVersion version = new ProjectVersion(project, 1, "An idea for an app", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);
        ClarifyingQuestion question = new ClarifyingQuestion(version, "Who is the primary user?", 0);
        question.setId(100L);
        version.getClarifyingQuestions().add(question);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        ExportedMarkdown export = markdownExportService.export(1L, 1);

        assertThat(export.filename()).isEqualTo("test-project-v1.md");
        assertThat(export.content()).contains("# Test Project");
        assertThat(export.content()).contains("**Version:** 1");
        assertThat(export.content()).contains("## Idea", "An idea for an app");
        assertThat(export.content()).contains("Who is the primary user?", "_Not yet answered._");
        assertThat(export.content()).doesNotContain("## Requirements", "## Roadmap");
    }

    @Test
    void exportsAnsweredQuestionAndRequirementsGroupedByType() {
        Project project = project("Test Project");
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        version.setId(10L);
        ClarifyingQuestion question = new ClarifyingQuestion(version, "Who is the primary user?", 0);
        question.setId(100L);
        question.setAnswer(new ClarifyingAnswer(question, "End consumers"));
        version.getClarifyingQuestions().add(question);

        Requirement functional = new Requirement(version, RequirementType.FUNCTIONAL, "Sign up", "Users can register.", 0);
        functional.setId(200L);
        Requirement nonFunctional =
                new Requirement(version, RequirementType.NON_FUNCTIONAL, "Fast", "Responds within 200ms.", 1);
        nonFunctional.setId(201L);
        version.getRequirements().addAll(java.util.List.of(functional, nonFunctional));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        ExportedMarkdown export = markdownExportService.export(1L, 1);

        assertThat(export.content()).contains("End consumers");
        assertThat(export.content()).contains("### Functional", "Sign up", "Users can register.");
        assertThat(export.content()).contains("### Non-Functional", "Fast", "Responds within 200ms.");
        assertThat(export.content().indexOf("### Functional"))
                .isLessThan(export.content().indexOf("### Non-Functional"));
    }

    @Test
    void exportsFullRoadmapHierarchyWithTasks() {
        Project project = project("Test Project");
        ProjectVersion version = new ProjectVersion(project, 2, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        version.setId(11L);

        Epic epic = new Epic(version, "User onboarding", "Covers account creation.", 0);
        epic.setId(300L);
        UserStory userStory =
                new UserStory(epic, "Email sign-up", "As a new user, I want to sign up.", "- Can register", 0);
        userStory.setId(400L);
        Task task = new Task(
                userStory, "Add POST /register", "Implement the endpoint.", TaskPriority.HIGH, TaskEffort.M, 0);
        task.setId(500L);
        userStory.getTasks().add(task);
        epic.getUserStories().add(userStory);
        version.getEpics().add(epic);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 2)).thenReturn(Optional.of(version));

        ExportedMarkdown export = markdownExportService.export(1L, 2);

        assertThat(export.filename()).isEqualTo("test-project-v2.md");
        assertThat(export.content())
                .contains(
                        "## Roadmap",
                        "### User onboarding",
                        "#### Email sign-up",
                        "**Acceptance Criteria:**",
                        "- Can register",
                        "**Tasks:**",
                        "`[HIGH / M]` Add POST /register — Implement the endpoint.");
    }

    @Test
    void slugifiesProjectNamesWithSpecialCharacters() {
        Project project = project("  My Project: v2.0!  ");
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.AWAITING_ANSWERS);
        version.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        ExportedMarkdown export = markdownExportService.export(1L, 1);

        assertThat(export.filename()).isEqualTo("my-project-v2-0-v1.md");
    }

    @Test
    void exportsArchitectureRecommendations() {
        Project project = project("Test Project");
        ProjectVersion version =
                new ProjectVersion(project, 3, "An idea", ProjectVersionStatus.ARCHITECTURE_GENERATED);
        version.setId(12L);

        ArchitectureRecommendation recommendation = new ArchitectureRecommendation(
                version,
                "Database",
                "PostgreSQL",
                "The domain is relational.",
                "MongoDB was considered but rejected.",
                0);
        recommendation.setId(600L);
        version.getArchitectureRecommendations().add(recommendation);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 3)).thenReturn(Optional.of(version));

        ExportedMarkdown export = markdownExportService.export(1L, 3);

        assertThat(export.content())
                .contains(
                        "## Architecture Recommendations",
                        "### Database",
                        "**Recommendation:** PostgreSQL",
                        "**Reasoning:** The domain is relational.",
                        "**Trade-offs:** MongoDB was considered but rejected.");
    }

    @Test
    void throwsNotFoundWhenVersionMissing() {
        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> markdownExportService.export(1L, 1))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }
}
