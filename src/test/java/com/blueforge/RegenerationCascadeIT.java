package com.blueforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.ProjectVersionSummaryResponse;
import com.blueforge.dto.RegenerateVersionRequest;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.entity.ProjectVersionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Unit tests mock the repository, so cloning logic is only checked against in-memory objects the
// test itself constructed - they can't catch a cascade/orphanRemoval misconfiguration that would
// let regenerating one version accidentally mutate rows belonging to another. This regenerates
// against a real Postgres and re-fetches both versions fresh to prove the base version's row is
// untouched and the new version is a genuinely separate, persisted row.
class RegenerationCascadeIT extends AbstractProjectApiIT {

    @MockitoBean
    private AiClient aiClient;

    @Test
    void regeneratingEpicsCreatesNewPersistedVersionAndLeavesOriginalUntouched() throws Exception {
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
                        [{"title": "Different Recipe Management", "description": "A fresh take on the same requirements."}]
                        """);

        CreateProjectResponse created = postAndRead(
                "/api/projects",
                new CreateProjectRequest("IT Recipe App", "A recipe sharing app"),
                CreateProjectResponse.class);
        Long questionId = created.questions().get(0).id();
        String v1Base = "/api/projects/" + created.projectId() + "/versions/1";

        postAndRead(
                v1Base + "/answers",
                new SubmitAnswersRequest(List.of(new AnswerRequest(questionId, "Home cooks"))),
                ProjectVersionResponse.class);
        ProjectVersionResponse v1Epics = postAndRead(v1Base + "/epics", null, ProjectVersionResponse.class);
        assertThat(v1Epics.epics().get(0).title()).isEqualTo("Recipe Management");

        ProjectVersionResponse v2 = postAndRead(
                v1Base + "/regenerate",
                new RegenerateVersionRequest(ProjectVersionStatus.EPICS_GENERATED, "Trying again"),
                ProjectVersionResponse.class);
        assertThat(v2.versionNumber()).isEqualTo(2);
        assertThat(v2.changeDescription()).isEqualTo("Trying again");
        assertThat(v2.epics().get(0).title()).isEqualTo("Different Recipe Management");

        ProjectVersionResponse v1Reloaded = getAndRead(v1Base, ProjectVersionResponse.class);
        assertThat(v1Reloaded.status()).isEqualTo(ProjectVersionStatus.EPICS_GENERATED);
        assertThat(v1Reloaded.changeDescription()).isNull();
        assertThat(v1Reloaded.epics()).hasSize(1);
        assertThat(v1Reloaded.epics().get(0).title()).isEqualTo("Recipe Management");

        String v2Base = "/api/projects/" + created.projectId() + "/versions/2";
        ProjectVersionResponse v2Reloaded = getAndRead(v2Base, ProjectVersionResponse.class);
        assertThat(v2Reloaded.epics().get(0).title()).isEqualTo("Different Recipe Management");

        ProjectVersionSummaryResponse[] versions = getAndRead(
                "/api/projects/" + created.projectId() + "/versions", ProjectVersionSummaryResponse[].class);
        assertThat(versions).hasSize(2);
        assertThat(versions[0].versionNumber()).isEqualTo(1);
        assertThat(versions[1].versionNumber()).isEqualTo(2);
    }
}
