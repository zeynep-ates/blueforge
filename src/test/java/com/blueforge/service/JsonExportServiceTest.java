package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.blueforge.dto.ClarifyingQuestionResponse;
import com.blueforge.dto.ProjectDetailResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.entity.ProjectVersionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonExportServiceTest {

    @Mock
    private ProjectService projectService;

    private JsonExportService jsonExportService;

    @BeforeEach
    void setUp() {
        jsonExportService = new JsonExportService(projectService, new ObjectMapper());
    }

    @Test
    void exportsTheProjectVersionResponseAsPrettyPrintedJson() {
        when(projectService.getProject(1L))
                .thenReturn(new ProjectDetailResponse(1L, "Test Project", Instant.parse("2026-07-01T00:00:00Z"), List.of()));
        when(projectService.getProjectVersion(1L, 1))
                .thenReturn(new ProjectVersionResponse(
                        10L,
                        1L,
                        1,
                        "An idea",
                        null,
                        ProjectVersionStatus.AWAITING_ANSWERS,
                        List.of(new ClarifyingQuestionResponse(100L, "Who is the primary user?", 0, null)),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));

        ExportedJson export = jsonExportService.export(1L, 1);

        assertThat(export.filename()).isEqualTo("test-project-v1.json");
        assertThat(export.content()).contains("\"versionId\" : 10");
        assertThat(export.content()).contains("\"ideaSnapshot\" : \"An idea\"");
        assertThat(export.content()).contains("\"questionText\" : \"Who is the primary user?\"");
    }

    @Test
    void slugifiesProjectNamesWithSpecialCharacters() {
        when(projectService.getProject(1L))
                .thenReturn(new ProjectDetailResponse(1L, "  My Project: v2.0!  ", Instant.parse("2026-07-01T00:00:00Z"), List.of()));
        when(projectService.getProjectVersion(1L, 2))
                .thenReturn(new ProjectVersionResponse(
                        10L,
                        1L,
                        2,
                        "An idea",
                        null,
                        ProjectVersionStatus.AWAITING_ANSWERS,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));

        ExportedJson export = jsonExportService.export(1L, 2);

        assertThat(export.filename()).isEqualTo("my-project-v2-0-v2.json");
    }
}
