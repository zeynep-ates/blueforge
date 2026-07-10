package com.blueforge.controller;

import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectDetailResponse;
import com.blueforge.dto.ProjectSummaryResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.ProjectVersionSummaryResponse;
import com.blueforge.dto.RegenerateVersionRequest;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.dto.VersionDiffResponse;
import com.blueforge.service.ExportedMarkdown;
import com.blueforge.service.MarkdownExportService;
import com.blueforge.service.ProjectService;
import com.blueforge.service.UnsupportedExportFormatException;
import com.blueforge.service.VersionDiffService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final VersionDiffService versionDiffService;
    private final MarkdownExportService markdownExportService;

    public ProjectController(
            ProjectService projectService,
            VersionDiffService versionDiffService,
            MarkdownExportService markdownExportService) {
        this.projectService = projectService;
        this.versionDiffService = versionDiffService;
        this.markdownExportService = markdownExportService;
    }

    @PostMapping
    public ResponseEntity<CreateProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    @GetMapping("/{projectId}/versions/{versionNumber}")
    public ResponseEntity<ProjectVersionResponse> getProjectVersion(
            @PathVariable Long projectId, @PathVariable int versionNumber) {
        return ResponseEntity.ok(projectService.getProjectVersion(projectId, versionNumber));
    }

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> listProjects() {
        return ResponseEntity.ok(projectService.listProjects());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailResponse> getProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @GetMapping("/{projectId}/versions")
    public ResponseEntity<List<ProjectVersionSummaryResponse>> listVersions(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.listVersions(projectId));
    }

    @PostMapping("/{projectId}/versions/{versionNumber}/answers")
    public ResponseEntity<ProjectVersionResponse> submitAnswers(
            @PathVariable Long projectId,
            @PathVariable int versionNumber,
            @Valid @RequestBody SubmitAnswersRequest request) {
        return ResponseEntity.ok(projectService.submitAnswers(projectId, versionNumber, request));
    }

    @PostMapping("/{projectId}/versions/{versionNumber}/epics")
    public ResponseEntity<ProjectVersionResponse> generateEpics(
            @PathVariable Long projectId, @PathVariable int versionNumber) {
        return ResponseEntity.ok(projectService.generateEpics(projectId, versionNumber));
    }

    @PostMapping("/{projectId}/versions/{versionNumber}/user-stories")
    public ResponseEntity<ProjectVersionResponse> generateUserStories(
            @PathVariable Long projectId, @PathVariable int versionNumber) {
        return ResponseEntity.ok(projectService.generateUserStories(projectId, versionNumber));
    }

    @PostMapping("/{projectId}/versions/{versionNumber}/tasks")
    public ResponseEntity<ProjectVersionResponse> generateTasks(
            @PathVariable Long projectId, @PathVariable int versionNumber) {
        return ResponseEntity.ok(projectService.generateTasks(projectId, versionNumber));
    }

    @PostMapping("/{projectId}/versions/{versionNumber}/regenerate")
    public ResponseEntity<ProjectVersionResponse> regenerateVersion(
            @PathVariable Long projectId,
            @PathVariable int versionNumber,
            @Valid @RequestBody RegenerateVersionRequest request) {
        return ResponseEntity.ok(projectService.regenerateVersion(projectId, versionNumber, request));
    }

    @GetMapping("/{projectId}/versions/{fromVersion}/diff/{toVersion}")
    public ResponseEntity<VersionDiffResponse> getVersionDiff(
            @PathVariable Long projectId, @PathVariable int fromVersion, @PathVariable int toVersion) {
        return ResponseEntity.ok(versionDiffService.diff(projectId, fromVersion, toVersion));
    }

    @GetMapping("/{projectId}/versions/{versionNumber}/export")
    public ResponseEntity<String> exportVersion(
            @PathVariable Long projectId,
            @PathVariable int versionNumber,
            @RequestParam(defaultValue = "markdown") String format) {
        if (!"markdown".equalsIgnoreCase(format)) {
            throw new UnsupportedExportFormatException(format);
        }

        ExportedMarkdown export = markdownExportService.export(projectId, versionNumber);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .body(export.content());
    }
}
