package com.blueforge.service;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.ClarifyingQuestionResponse;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.entity.ClarifyingQuestion;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.repository.ProjectRepository;
import com.blueforge.repository.ProjectVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final String clarifyingQuestionsPromptTemplate;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectVersionRepository projectVersionRepository,
            AiClient aiClient,
            ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.projectVersionRepository = projectVersionRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.clarifyingQuestionsPromptTemplate = loadPromptTemplate("prompts/clarifying-questions.txt");
    }

    @Transactional
    public CreateProjectResponse createProject(CreateProjectRequest request) {
        Project project = projectRepository.save(new Project(request.name()));

        List<String> questionTexts = generateClarifyingQuestions(request.ideaDescription());

        ProjectVersion version =
                new ProjectVersion(project, 1, request.ideaDescription(), ProjectVersionStatus.AWAITING_ANSWERS);
        for (int i = 0; i < questionTexts.size(); i++) {
            version.getClarifyingQuestions().add(new ClarifyingQuestion(version, questionTexts.get(i), i));
        }
        version = projectVersionRepository.save(version);

        return new CreateProjectResponse(project.getId(), version.getId(), toQuestionResponses(version));
    }

    @Transactional(readOnly = true)
    public ProjectVersionResponse getProjectVersion(Long projectId, int versionNumber) {
        ProjectVersion version = projectVersionRepository
                .findByProjectIdAndVersionNumber(projectId, versionNumber)
                .orElseThrow(() -> new ProjectVersionNotFoundException(projectId, versionNumber));

        return new ProjectVersionResponse(
                version.getId(),
                projectId,
                version.getVersionNumber(),
                version.getIdeaSnapshot(),
                version.getChangeDescription(),
                version.getStatus(),
                toQuestionResponses(version));
    }

    private static List<ClarifyingQuestionResponse> toQuestionResponses(ProjectVersion version) {
        return version.getClarifyingQuestions().stream()
                .map(q -> new ClarifyingQuestionResponse(q.getId(), q.getQuestionText(), q.getOrderIndex()))
                .toList();
    }

    private List<String> generateClarifyingQuestions(String ideaDescription) {
        String prompt = clarifyingQuestionsPromptTemplate.replace("{{ideaDescription}}", ideaDescription);
        String rawResponse = aiClient.complete(prompt);
        try {
            return objectMapper.readValue(rawResponse, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new AiResponseParsingException(
                    "AI returned a response that could not be parsed as a JSON array of questions", e);
        }
    }

    private static String loadPromptTemplate(String classpathLocation) {
        try {
            return new ClassPathResource(classpathLocation).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt template: " + classpathLocation, e);
        }
    }
}
