package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.entity.ClarifyingQuestion;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.repository.ProjectRepository;
import com.blueforge.repository.ProjectVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
