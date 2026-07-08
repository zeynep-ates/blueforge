package com.blueforge.controller;

import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
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
}
