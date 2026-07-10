package com.blueforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.entity.ProjectVersionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// The unit tests in ProjectServiceTest mock ProjectVersionRepository entirely, so they can't catch
// a real cascade/orphan-removal misconfiguration. This drives the whole pipeline through real HTTP
// calls against a real Postgres (only AiClient is faked) and re-fetches with a fresh GET at the end
// to prove the final state was actually persisted, not just returned in-memory by the last call.
class ProjectPipelineIT extends AbstractProjectApiIT {

    @MockitoBean
    private AiClient aiClient;

    @Test
    void fullPipelineGeneratesAndPersistsThroughArchitectureRecommendations() throws Exception {
        when(aiClient.complete(any()))
                .thenReturn(
                        "[\"Who is the primary user?\"]",
                        """
                        [{"type": "FUNCTIONAL", "title": "Recipe upload", "description": "Users can upload a recipe."}]
                        """,
                        """
                        [{"title": "Recipe Management", "description": "Covers creating and browsing recipes."}]
                        """,
                        """
                        [{"stories": [{"title": "Upload a recipe", "description": "As a home cook, I want to upload a recipe.", "acceptanceCriteria": "- Title required"}]}]
                        """,
                        """
                        [{"tasks": [{"title": "Add POST /recipes", "description": "Implement the endpoint.", "priority": "HIGH", "effortEstimate": "M"}]}]
                        """,
                        """
                        [{"component": "Backend Framework", "recommendation": "Spring Boot", "reasoning": "Fits the requirements.", "tradeoffs": "Serverless was considered but rejected."}]
                        """);

        CreateProjectResponse created = postAndRead(
                "/api/projects",
                new CreateProjectRequest("IT Recipe App", "A recipe sharing app"),
                CreateProjectResponse.class);
        assertThat(created.questions()).hasSize(1);
        Long questionId = created.questions().get(0).id();
        String versionBase = "/api/projects/" + created.projectId() + "/versions/1";

        ProjectVersionResponse afterAnswers = postAndRead(
                versionBase + "/answers",
                new SubmitAnswersRequest(List.of(new AnswerRequest(questionId, "Home cooks"))),
                ProjectVersionResponse.class);
        assertThat(afterAnswers.status()).isEqualTo(ProjectVersionStatus.REQUIREMENTS_GENERATED);
        assertThat(afterAnswers.requirements()).hasSize(1);

        ProjectVersionResponse afterEpics = postAndRead(versionBase + "/epics", null, ProjectVersionResponse.class);
        assertThat(afterEpics.epics()).hasSize(1);

        ProjectVersionResponse afterStories =
                postAndRead(versionBase + "/user-stories", null, ProjectVersionResponse.class);
        assertThat(afterStories.userStories()).hasSize(1);

        ProjectVersionResponse afterTasks = postAndRead(versionBase + "/tasks", null, ProjectVersionResponse.class);
        assertThat(afterTasks.tasks()).hasSize(1);

        ProjectVersionResponse afterArchitecture =
                postAndRead(versionBase + "/architecture-recommendations", null, ProjectVersionResponse.class);
        assertThat(afterArchitecture.status()).isEqualTo(ProjectVersionStatus.ARCHITECTURE_GENERATED);
        assertThat(afterArchitecture.architectureRecommendations()).hasSize(1);

        ProjectVersionResponse reloaded = getAndRead(versionBase, ProjectVersionResponse.class);
        assertThat(reloaded.status()).isEqualTo(ProjectVersionStatus.ARCHITECTURE_GENERATED);
        assertThat(reloaded.requirements()).hasSize(1);
        assertThat(reloaded.epics()).hasSize(1);
        assertThat(reloaded.userStories()).hasSize(1);
        assertThat(reloaded.tasks()).hasSize(1);
        assertThat(reloaded.architectureRecommendations()).hasSize(1);
        assertThat(reloaded.architectureRecommendations().get(0).component()).isEqualTo("Backend Framework");
    }
}
