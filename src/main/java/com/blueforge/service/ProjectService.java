package com.blueforge.service;

import com.blueforge.ai.AiClient;
import com.blueforge.dto.AnswerRequest;
import com.blueforge.dto.ArchitectureRecommendationResponse;
import com.blueforge.dto.ClarifyingQuestionResponse;
import com.blueforge.dto.CreateProjectRequest;
import com.blueforge.dto.CreateProjectResponse;
import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.ProjectDetailResponse;
import com.blueforge.dto.ProjectSummaryResponse;
import com.blueforge.dto.ProjectVersionResponse;
import com.blueforge.dto.ProjectVersionSummaryResponse;
import com.blueforge.dto.RegenerateVersionRequest;
import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.SubmitAnswersRequest;
import com.blueforge.dto.TaskResponse;
import com.blueforge.dto.UserStoryResponse;
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
import java.util.stream.IntStream;
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
    private final String epicsPromptTemplate;
    private final String userStoriesPromptTemplate;
    private final String tasksPromptTemplate;
    private final String architectureRecommendationsPromptTemplate;

    private static final List<ProjectVersionStatus> REGENERABLE_STAGES = List.of(
            ProjectVersionStatus.REQUIREMENTS_GENERATED,
            ProjectVersionStatus.EPICS_GENERATED,
            ProjectVersionStatus.USER_STORIES_GENERATED,
            ProjectVersionStatus.TASKS_GENERATED,
            ProjectVersionStatus.ARCHITECTURE_GENERATED);

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
        this.epicsPromptTemplate = loadPromptTemplate("prompts/epics.txt");
        this.userStoriesPromptTemplate = loadPromptTemplate("prompts/user-stories.txt");
        this.tasksPromptTemplate = loadPromptTemplate("prompts/tasks.txt");
        this.architectureRecommendationsPromptTemplate =
                loadPromptTemplate("prompts/architecture-recommendations.txt");
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

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listProjects() {
        return projectRepository.findAll().stream().map(this::toProjectSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProject(Long projectId) {
        Project project =
                projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<ProjectVersionSummaryResponse> versions = projectVersionRepository
                .findByProjectIdOrderByVersionNumberAsc(projectId)
                .stream()
                .map(ProjectService::toProjectVersionSummaryResponse)
                .toList();

        return new ProjectDetailResponse(project.getId(), project.getName(), project.getCreatedAt(), versions);
    }

    @Transactional(readOnly = true)
    public List<ProjectVersionSummaryResponse> listVersions(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        return projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(projectId).stream()
                .map(ProjectService::toProjectVersionSummaryResponse)
                .toList();
    }

    @Transactional
    public ProjectVersionResponse submitAnswers(Long projectId, int versionNumber, SubmitAnswersRequest request) {
        ProjectVersion version = findVersionOrThrow(projectId, versionNumber);

        if (version.getStatus() != ProjectVersionStatus.AWAITING_ANSWERS) {
            throw new InvalidProjectVersionStatusException(
                    projectId, versionNumber, ProjectVersionStatus.AWAITING_ANSWERS, version.getStatus());
        }

        applyAnswers(version, request.answers());
        applyRequirementsGeneration(version);

        version = projectVersionRepository.save(version);

        return toProjectVersionResponse(projectId, version);
    }

    @Transactional
    public ProjectVersionResponse generateEpics(Long projectId, int versionNumber) {
        ProjectVersion version = findVersionOrThrow(projectId, versionNumber);

        if (version.getStatus() != ProjectVersionStatus.REQUIREMENTS_GENERATED) {
            throw new InvalidProjectVersionStatusException(
                    projectId, versionNumber, ProjectVersionStatus.REQUIREMENTS_GENERATED, version.getStatus());
        }

        applyEpicsGeneration(version);

        version = projectVersionRepository.save(version);

        return toProjectVersionResponse(projectId, version);
    }

    @Transactional
    public ProjectVersionResponse generateUserStories(Long projectId, int versionNumber) {
        ProjectVersion version = findVersionOrThrow(projectId, versionNumber);

        if (version.getStatus() != ProjectVersionStatus.EPICS_GENERATED) {
            throw new InvalidProjectVersionStatusException(
                    projectId, versionNumber, ProjectVersionStatus.EPICS_GENERATED, version.getStatus());
        }

        applyUserStoriesGeneration(version);

        version = projectVersionRepository.save(version);

        return toProjectVersionResponse(projectId, version);
    }

    @Transactional
    public ProjectVersionResponse generateTasks(Long projectId, int versionNumber) {
        ProjectVersion version = findVersionOrThrow(projectId, versionNumber);

        if (version.getStatus() != ProjectVersionStatus.USER_STORIES_GENERATED) {
            throw new InvalidProjectVersionStatusException(
                    projectId, versionNumber, ProjectVersionStatus.USER_STORIES_GENERATED, version.getStatus());
        }

        applyTasksGeneration(version);

        version = projectVersionRepository.save(version);

        return toProjectVersionResponse(projectId, version);
    }

    @Transactional
    public ProjectVersionResponse generateArchitectureRecommendations(Long projectId, int versionNumber) {
        ProjectVersion version = findVersionOrThrow(projectId, versionNumber);

        if (version.getStatus() != ProjectVersionStatus.TASKS_GENERATED) {
            throw new InvalidProjectVersionStatusException(
                    projectId, versionNumber, ProjectVersionStatus.TASKS_GENERATED, version.getStatus());
        }

        applyArchitectureRecommendationsGeneration(version);

        version = projectVersionRepository.save(version);

        return toProjectVersionResponse(projectId, version);
    }

    private ProjectVersion findVersionOrThrow(Long projectId, int versionNumber) {
        return projectVersionRepository
                .findByProjectIdAndVersionNumber(projectId, versionNumber)
                .orElseThrow(() -> new ProjectVersionNotFoundException(projectId, versionNumber));
    }

    private void applyRequirementsGeneration(ProjectVersion version) {
        List<GeneratedRequirement> generatedRequirements = generateRequirements(version);
        for (int i = 0; i < generatedRequirements.size(); i++) {
            GeneratedRequirement generated = generatedRequirements.get(i);
            version.getRequirements()
                    .add(new Requirement(version, generated.type(), generated.title(), generated.description(), i));
        }
        version.setStatus(ProjectVersionStatus.REQUIREMENTS_GENERATED);
    }

    private void applyEpicsGeneration(ProjectVersion version) {
        if (version.getRequirements().isEmpty()) {
            throw new IllegalStateException("Version " + version.getVersionNumber() + " of project "
                    + version.getProject().getId() + " reached " + ProjectVersionStatus.REQUIREMENTS_GENERATED
                    + " with no requirements");
        }

        List<GeneratedEpic> generatedEpics = requestEpicsFromAi(version);
        for (int i = 0; i < generatedEpics.size(); i++) {
            GeneratedEpic generated = generatedEpics.get(i);
            version.getEpics().add(new Epic(version, generated.title(), generated.description(), i));
        }
        version.setStatus(ProjectVersionStatus.EPICS_GENERATED);
    }

    private void applyUserStoriesGeneration(ProjectVersion version) {
        List<Epic> epics = version.getEpics();
        if (epics.isEmpty()) {
            throw new IllegalStateException("Version " + version.getVersionNumber() + " of project "
                    + version.getProject().getId() + " reached " + ProjectVersionStatus.EPICS_GENERATED
                    + " with no epics");
        }

        List<GeneratedEpicStories> generatedEpicStories = requestUserStoriesFromAi(version);
        if (generatedEpicStories.size() != epics.size()) {
            throw new AiResponseParsingException("AI returned user stories for " + generatedEpicStories.size()
                    + " epics, expected exactly " + epics.size());
        }
        for (int i = 0; i < epics.size(); i++) {
            Epic epic = epics.get(i);
            List<GeneratedUserStory> stories = generatedEpicStories.get(i).stories();
            for (int j = 0; j < stories.size(); j++) {
                GeneratedUserStory generated = stories.get(j);
                epic.getUserStories()
                        .add(new UserStory(
                                epic, generated.title(), generated.description(), generated.acceptanceCriteria(), j));
            }
        }
        version.setStatus(ProjectVersionStatus.USER_STORIES_GENERATED);
    }

    private void applyTasksGeneration(ProjectVersion version) {
        List<Epic> epics = version.getEpics();
        if (epics.isEmpty()) {
            throw new IllegalStateException("Version " + version.getVersionNumber() + " of project "
                    + version.getProject().getId() + " reached " + ProjectVersionStatus.USER_STORIES_GENERATED
                    + " with no epics");
        }
        for (Epic epic : epics) {
            if (epic.getUserStories().isEmpty()) {
                throw new IllegalStateException("Version " + version.getVersionNumber() + " of project "
                        + version.getProject().getId() + " reached " + ProjectVersionStatus.USER_STORIES_GENERATED
                        + " with epic " + epic.getId() + " having no user stories");
            }
        }

        for (Epic epic : epics) {
            List<UserStory> stories = epic.getUserStories();
            List<GeneratedUserStoryTasks> generatedTasksByStory = requestTasksFromAi(version, epic);
            if (generatedTasksByStory.size() != stories.size()) {
                throw new AiResponseParsingException("AI returned tasks for " + generatedTasksByStory.size()
                        + " user stories in epic " + epic.getId() + ", expected exactly " + stories.size());
            }
            for (int i = 0; i < stories.size(); i++) {
                UserStory story = stories.get(i);
                List<GeneratedTask> tasks = generatedTasksByStory.get(i).tasks();
                for (int j = 0; j < tasks.size(); j++) {
                    GeneratedTask generated = tasks.get(j);
                    story.getTasks()
                            .add(new Task(
                                    story,
                                    generated.title(),
                                    generated.description(),
                                    generated.priority(),
                                    generated.effortEstimate(),
                                    j));
                }
            }
        }
        version.setStatus(ProjectVersionStatus.TASKS_GENERATED);
    }

    private void applyArchitectureRecommendationsGeneration(ProjectVersion version) {
        if (version.getEpics().isEmpty()) {
            throw new IllegalStateException("Version " + version.getVersionNumber() + " of project "
                    + version.getProject().getId() + " reached " + ProjectVersionStatus.TASKS_GENERATED
                    + " with no epics");
        }

        List<GeneratedArchitectureRecommendation> generated = requestArchitectureRecommendationsFromAi(version);
        for (int i = 0; i < generated.size(); i++) {
            GeneratedArchitectureRecommendation recommendation = generated.get(i);
            version.getArchitectureRecommendations()
                    .add(new ArchitectureRecommendation(
                            version,
                            recommendation.component(),
                            recommendation.recommendation(),
                            recommendation.reasoning(),
                            recommendation.tradeoffs(),
                            i));
        }
        version.setStatus(ProjectVersionStatus.ARCHITECTURE_GENERATED);
    }

    @Transactional
    public ProjectVersionResponse regenerateVersion(
            Long projectId, int baseVersionNumber, RegenerateVersionRequest request) {
        ProjectVersion base = findVersionOrThrow(projectId, baseVersionNumber);
        ProjectVersionStatus targetStage = request.targetStage();

        if (!REGENERABLE_STAGES.contains(targetStage)) {
            throw new InvalidRegenerationTargetException(targetStage);
        }
        if (base.getStatus().ordinal() < targetStage.ordinal()) {
            throw new RegenerationNotAllowedException(projectId, baseVersionNumber, targetStage, base.getStatus());
        }

        ProjectVersionStatus cloneStartStatus = ProjectVersionStatus.values()[targetStage.ordinal() - 1];
        ProjectVersion clone = new ProjectVersion(
                base.getProject(), nextVersionNumber(projectId), base.getIdeaSnapshot(), cloneStartStatus);
        clone.setChangeDescription(request.changeDescription());

        cloneQuestionsAndAnswers(base, clone);
        if (targetStage.ordinal() >= ProjectVersionStatus.EPICS_GENERATED.ordinal()) {
            cloneRequirements(base, clone);
        }
        if (targetStage.ordinal() >= ProjectVersionStatus.USER_STORIES_GENERATED.ordinal()) {
            cloneEpicsShell(base, clone);
        }
        if (targetStage.ordinal() >= ProjectVersionStatus.TASKS_GENERATED.ordinal()) {
            cloneUserStoriesInto(base, clone);
        }
        if (targetStage.ordinal() >= ProjectVersionStatus.ARCHITECTURE_GENERATED.ordinal()) {
            cloneTasksInto(base, clone);
        }

        switch (targetStage) {
            case REQUIREMENTS_GENERATED -> applyRequirementsGeneration(clone);
            case EPICS_GENERATED -> applyEpicsGeneration(clone);
            case USER_STORIES_GENERATED -> applyUserStoriesGeneration(clone);
            case TASKS_GENERATED -> applyTasksGeneration(clone);
            case ARCHITECTURE_GENERATED -> applyArchitectureRecommendationsGeneration(clone);
            default -> throw new InvalidRegenerationTargetException(targetStage);
        }

        clone = projectVersionRepository.save(clone);

        return toProjectVersionResponse(projectId, clone);
    }

    private int nextVersionNumber(Long projectId) {
        return findLatestVersion(projectId).getVersionNumber() + 1;
    }

    private ProjectVersion findLatestVersion(Long projectId) {
        List<ProjectVersion> versions = projectVersionRepository.findByProjectIdOrderByVersionNumberAsc(projectId);
        return versions.get(versions.size() - 1);
    }

    private static void cloneQuestionsAndAnswers(ProjectVersion base, ProjectVersion target) {
        for (ClarifyingQuestion question : base.getClarifyingQuestions()) {
            ClarifyingQuestion clonedQuestion =
                    new ClarifyingQuestion(target, question.getQuestionText(), question.getOrderIndex());
            if (question.getAnswer() != null) {
                clonedQuestion.setAnswer(new ClarifyingAnswer(clonedQuestion, question.getAnswer().getAnswerText()));
            }
            target.getClarifyingQuestions().add(clonedQuestion);
        }
    }

    private static void cloneRequirements(ProjectVersion base, ProjectVersion target) {
        for (Requirement requirement : base.getRequirements()) {
            target.getRequirements()
                    .add(new Requirement(
                            target,
                            requirement.getType(),
                            requirement.getTitle(),
                            requirement.getDescription(),
                            requirement.getOrderIndex()));
        }
    }

    private static void cloneEpicsShell(ProjectVersion base, ProjectVersion target) {
        for (Epic epic : base.getEpics()) {
            target.getEpics().add(new Epic(target, epic.getTitle(), epic.getDescription(), epic.getOrderIndex()));
        }
    }

    // Requires cloneEpicsShell to have already populated target's epics in the same order as base's.
    private static void cloneUserStoriesInto(ProjectVersion base, ProjectVersion target) {
        List<Epic> baseEpics = base.getEpics();
        List<Epic> targetEpics = target.getEpics();
        for (int i = 0; i < baseEpics.size(); i++) {
            Epic targetEpic = targetEpics.get(i);
            for (UserStory story : baseEpics.get(i).getUserStories()) {
                targetEpic.getUserStories()
                        .add(new UserStory(
                                targetEpic,
                                story.getTitle(),
                                story.getDescription(),
                                story.getAcceptanceCriteria(),
                                story.getOrderIndex()));
            }
        }
    }

    // Requires cloneUserStoriesInto to have already populated target's user stories in the same order as base's.
    private static void cloneTasksInto(ProjectVersion base, ProjectVersion target) {
        List<Epic> baseEpics = base.getEpics();
        List<Epic> targetEpics = target.getEpics();
        for (int i = 0; i < baseEpics.size(); i++) {
            List<UserStory> baseStories = baseEpics.get(i).getUserStories();
            List<UserStory> targetStories = targetEpics.get(i).getUserStories();
            for (int j = 0; j < baseStories.size(); j++) {
                UserStory targetStory = targetStories.get(j);
                for (Task task : baseStories.get(j).getTasks()) {
                    targetStory.getTasks()
                            .add(new Task(
                                    targetStory,
                                    task.getTitle(),
                                    task.getDescription(),
                                    task.getPriority(),
                                    task.getEffortEstimate(),
                                    task.getOrderIndex()));
                }
            }
        }
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
                toRequirementResponses(version),
                toEpicResponses(version),
                toUserStoryResponses(version),
                toTaskResponses(version),
                toArchitectureRecommendationResponses(version));
    }

    private ProjectSummaryResponse toProjectSummaryResponse(Project project) {
        ProjectVersion latest = findLatestVersion(project.getId());
        return new ProjectSummaryResponse(
                project.getId(), project.getName(), project.getCreatedAt(), latest.getVersionNumber(), latest.getStatus());
    }

    private static ProjectVersionSummaryResponse toProjectVersionSummaryResponse(ProjectVersion version) {
        return new ProjectVersionSummaryResponse(
                version.getId(), version.getVersionNumber(), version.getStatus(), version.getChangeDescription());
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

    private static List<EpicResponse> toEpicResponses(ProjectVersion version) {
        return version.getEpics().stream()
                .map(e -> new EpicResponse(e.getId(), e.getTitle(), e.getDescription(), e.getOrderIndex()))
                .toList();
    }

    private static List<UserStoryResponse> toUserStoryResponses(ProjectVersion version) {
        return version.getEpics().stream()
                .flatMap(e -> e.getUserStories().stream())
                .map(s -> new UserStoryResponse(
                        s.getId(),
                        s.getEpic().getId(),
                        s.getTitle(),
                        s.getDescription(),
                        s.getAcceptanceCriteria(),
                        s.getOrderIndex()))
                .toList();
    }

    private static List<TaskResponse> toTaskResponses(ProjectVersion version) {
        return version.getEpics().stream()
                .flatMap(e -> e.getUserStories().stream())
                .flatMap(s -> s.getTasks().stream())
                .map(t -> new TaskResponse(
                        t.getId(),
                        t.getUserStory().getId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getPriority(),
                        t.getEffortEstimate(),
                        t.getOrderIndex()))
                .toList();
    }

    private static List<ArchitectureRecommendationResponse> toArchitectureRecommendationResponses(
            ProjectVersion version) {
        return version.getArchitectureRecommendations().stream()
                .map(a -> new ArchitectureRecommendationResponse(
                        a.getId(), a.getComponent(), a.getRecommendation(), a.getReasoning(), a.getTradeoffs(),
                        a.getOrderIndex()))
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

    private List<GeneratedEpic> requestEpicsFromAi(ProjectVersion version) {
        String prompt = epicsPromptTemplate
                .replace("{{ideaDescription}}", version.getIdeaSnapshot())
                .replace("{{requirements}}", formatRequirements(version));
        String rawResponse = aiClient.complete(prompt);
        try {
            return objectMapper.readValue(rawResponse, new TypeReference<List<GeneratedEpic>>() {});
        } catch (JsonProcessingException e) {
            throw new AiResponseParsingException(
                    "AI returned a response that could not be parsed as a JSON array of epics", e);
        }
    }

    private static String formatRequirements(ProjectVersion version) {
        return version.getRequirements().stream()
                .map(r -> "[" + r.getType() + "] " + r.getTitle() + ": " + r.getDescription())
                .collect(Collectors.joining("\n"));
    }

    private List<GeneratedEpicStories> requestUserStoriesFromAi(ProjectVersion version) {
        String prompt = userStoriesPromptTemplate
                .replace("{{ideaDescription}}", version.getIdeaSnapshot())
                .replace("{{epics}}", formatEpics(version));
        String rawResponse = aiClient.complete(prompt);
        try {
            return objectMapper.readValue(rawResponse, new TypeReference<List<GeneratedEpicStories>>() {});
        } catch (JsonProcessingException e) {
            throw new AiResponseParsingException(
                    "AI returned a response that could not be parsed as a JSON array of epic user stories", e);
        }
    }

    private static String formatEpics(ProjectVersion version) {
        List<Epic> epics = version.getEpics();
        return IntStream.range(0, epics.size())
                .mapToObj(i -> (i + 1) + ". " + epics.get(i).getTitle() + ": " + epics.get(i).getDescription())
                .collect(Collectors.joining("\n"));
    }

    private List<GeneratedUserStoryTasks> requestTasksFromAi(ProjectVersion version, Epic epic) {
        String prompt = tasksPromptTemplate
                .replace("{{ideaDescription}}", version.getIdeaSnapshot())
                .replace("{{epic}}", epic.getTitle() + ": " + epic.getDescription())
                .replace("{{userStories}}", formatUserStories(epic));
        String rawResponse = aiClient.complete(prompt);
        try {
            return objectMapper.readValue(rawResponse, new TypeReference<List<GeneratedUserStoryTasks>>() {});
        } catch (JsonProcessingException e) {
            throw new AiResponseParsingException(
                    "AI returned a response that could not be parsed as a JSON array of user story tasks", e);
        }
    }

    private static String formatUserStories(Epic epic) {
        List<UserStory> stories = epic.getUserStories();
        return IntStream.range(0, stories.size())
                .mapToObj(i -> (i + 1) + ". " + stories.get(i).getTitle() + ": " + stories.get(i).getDescription())
                .collect(Collectors.joining("\n"));
    }

    private List<GeneratedArchitectureRecommendation> requestArchitectureRecommendationsFromAi(
            ProjectVersion version) {
        String prompt = architectureRecommendationsPromptTemplate
                .replace("{{ideaDescription}}", version.getIdeaSnapshot())
                .replace("{{requirements}}", formatRequirements(version))
                .replace("{{epics}}", formatEpics(version));
        String rawResponse = aiClient.complete(prompt);
        try {
            return objectMapper.readValue(rawResponse, new TypeReference<List<GeneratedArchitectureRecommendation>>() {});
        } catch (JsonProcessingException e) {
            throw new AiResponseParsingException(
                    "AI returned a response that could not be parsed as a JSON array of architecture recommendations",
                    e);
        }
    }

    private static String loadPromptTemplate(String classpathLocation) {
        try {
            return new ClassPathResource(classpathLocation).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt template: " + classpathLocation, e);
        }
    }

    private record GeneratedRequirement(RequirementType type, String title, String description) {}

    private record GeneratedEpic(String title, String description) {}

    private record GeneratedUserStory(String title, String description, String acceptanceCriteria) {}

    private record GeneratedEpicStories(List<GeneratedUserStory> stories) {}

    private record GeneratedTask(
            String title, String description, TaskPriority priority, TaskEffort effortEstimate) {}

    private record GeneratedUserStoryTasks(List<GeneratedTask> tasks) {}

    private record GeneratedArchitectureRecommendation(
            String component, String recommendation, String reasoning, String tradeoffs) {}
}
