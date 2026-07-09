package com.blueforge.service;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.ClarifyingQuestionResponse;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.entity.ClarifyingAnswer;
import com.blueforge.entity.ClarifyingQuestion;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.Requirement;
import com.blueforge.entity.RequirementType;
import com.blueforge.repository.ProjectRepository;
import com.blueforge.repository.ProjectVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final String requirementsPromptTemplate;

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
        this.requirementsPromptTemplate = loadPromptTemplate("prompts/requirements.txt");
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

        return toProjectVersionResponse(projectId, version);
    }

    @Transactional
    public ProjectVersionResponse submitAnswers(Long projectId, int versionNumber, SubmitAnswersRequest request) {
        ProjectVersion version = projectVersionRepository
                .findByProjectIdAndVersionNumber(projectId, versionNumber)
                .orElseThrow(() -> new ProjectVersionNotFoundException(projectId, versionNumber));

        if (version.getStatus() != ProjectVersionStatus.AWAITING_ANSWERS) {
            throw new InvalidProjectVersionStatusException(
                    projectId, versionNumber, ProjectVersionStatus.AWAITING_ANSWERS, version.getStatus());
        }

        applyAnswers(version, request.answers());

        List<GeneratedRequirement> generatedRequirements = generateRequirements(version);
        for (int i = 0; i < generatedRequirements.size(); i++) {
            GeneratedRequirement generated = generatedRequirements.get(i);
            version.getRequirements()
                    .add(new Requirement(version, generated.type(), generated.title(), generated.description(), i));
        }
        version.setStatus(ProjectVersionStatus.REQUIREMENTS_GENERATED);

        version = projectVersionRepository.save(version);

        return toProjectVersionResponse(projectId, version);
    }

    private void applyAnswers(ProjectVersion version, List<AnswerRequest> answers) {
        Map<Long, ClarifyingQuestion> questionsById = version.getClarifyingQuestions().stream()
                .collect(Collectors.toMap(ClarifyingQuestion::getId, Function.identity()));

        if (answers.size() != questionsById.size()) {
            throw new InvalidAnswersException("Expected exactly one answer for each of the "
                    + questionsById.size() + " clarifying questions, got " + answers.size());
        }

        Set<Long> answeredQuestionIds = new HashSet<>();
        for (AnswerRequest answerRequest : answers) {
            ClarifyingQuestion question = questionsById.get(answerRequest.questionId());
            if (question == null) {
                throw new InvalidAnswersException(
                        "Question " + answerRequest.questionId() + " does not belong to this project version");
            }
            if (!answeredQuestionIds.add(answerRequest.questionId())) {
                throw new InvalidAnswersException(
                        "Duplicate answer submitted for question " + answerRequest.questionId());
            }
            question.setAnswer(new ClarifyingAnswer(question, answerRequest.answerText()));
        }
    }

    private static ProjectVersionResponse toProjectVersionResponse(Long projectId, ProjectVersion version) {
        return new ProjectVersionResponse(
                version.getId(),
                projectId,
                version.getVersionNumber(),
                version.getIdeaSnapshot(),
                version.getChangeDescription(),
                version.getStatus(),
                toQuestionResponses(version),
                toRequirementResponses(version));
    }

    private static List<ClarifyingQuestionResponse> toQuestionResponses(ProjectVersion version) {
        return version.getClarifyingQuestions().stream()
                .map(q -> new ClarifyingQuestionResponse(
                        q.getId(),
                        q.getQuestionText(),
                        q.getOrderIndex(),
                        q.getAnswer() != null ? q.getAnswer().getAnswerText() : null))
                .toList();
    }

    private static List<RequirementResponse> toRequirementResponses(ProjectVersion version) {
        return version.getRequirements().stream()
                .map(r -> new RequirementResponse(
                        r.getId(), r.getType(), r.getTitle(), r.getDescription(), r.getOrderIndex()))
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

    private List<GeneratedRequirement> generateRequirements(ProjectVersion version) {
        String prompt = requirementsPromptTemplate
                .replace("{{ideaDescription}}", version.getIdeaSnapshot())
                .replace("{{clarifyingQuestionsAndAnswers}}", formatQuestionsAndAnswers(version));
        String rawResponse = aiClient.complete(prompt);
        try {
            return objectMapper.readValue(rawResponse, new TypeReference<List<GeneratedRequirement>>() {});
        } catch (JsonProcessingException e) {
            throw new AiResponseParsingException(
                    "AI returned a response that could not be parsed as a JSON array of requirements", e);
        }
    }

    private static String formatQuestionsAndAnswers(ProjectVersion version) {
        return version.getClarifyingQuestions().stream()
                .map(q -> "Q: " + q.getQuestionText() + "\nA: " + q.getAnswer().getAnswerText())
                .collect(Collectors.joining("\n\n"));
    }

    private static String loadPromptTemplate(String classpathLocation) {
        try {
            return new ClassPathResource(classpathLocation).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt template: " + classpathLocation, e);
        }
    }

    private record GeneratedRequirement(RequirementType type, String title, String description) {}
}
