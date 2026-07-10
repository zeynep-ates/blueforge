package com.blueforge.service;

import com.blueforge.dto.ProjectVersionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JsonExportService {

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");

    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public JsonExportService(ProjectService projectService, ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ExportedJson export(Long projectId, int versionNumber) {
        String projectName = projectService.getProject(projectId).name();
        ProjectVersionResponse version = projectService.getProjectVersion(projectId, versionNumber);

        String content;
        try {
            content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(version);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize project version to JSON", e);
        }

        String filename = slugify(projectName) + "-v" + versionNumber + ".json";
        return new ExportedJson(filename, content);
    }

    private static String slugify(String name) {
        String slug = NON_SLUG_CHARS.matcher(name.toLowerCase()).replaceAll("-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "project" : slug;
    }
}
