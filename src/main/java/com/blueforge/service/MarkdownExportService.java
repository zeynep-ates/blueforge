package com.blueforge.service;

import com.blueforge.entity.ArchitectureRecommendation;
import com.blueforge.entity.ClarifyingAnswer;
import com.blueforge.entity.ClarifyingQuestion;
import com.blueforge.entity.Epic;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.Requirement;
import com.blueforge.entity.RequirementType;
import com.blueforge.entity.Task;
import com.blueforge.entity.UserStory;
import com.blueforge.repository.ProjectVersionRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkdownExportService {

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final List<RequirementType> REQUIREMENT_TYPE_ORDER =
            List.of(RequirementType.FUNCTIONAL, RequirementType.NON_FUNCTIONAL, RequirementType.CONSTRAINT, RequirementType.ASSUMPTION);

    private final ProjectVersionRepository projectVersionRepository;

    public MarkdownExportService(ProjectVersionRepository projectVersionRepository) {
        this.projectVersionRepository = projectVersionRepository;
    }

    @Transactional(readOnly = true)
    public ExportedMarkdown export(Long projectId, int versionNumber) {
        ProjectVersion version = projectVersionRepository
                .findByProjectIdAndVersionNumber(projectId, versionNumber)
                .orElseThrow(() -> new ProjectVersionNotFoundException(projectId, versionNumber));
        Project project = version.getProject();

        String content = render(project, version);
        String filename = slugify(project.getName()) + "-v" + versionNumber + ".md";
        return new ExportedMarkdown(filename, content);
    }

    private String render(Project project, ProjectVersion version) {
        StringBuilder md = new StringBuilder();

        md.append("# ").append(project.getName()).append("\n\n");
        md.append("**Version:** ").append(version.getVersionNumber()).append("\n\n");
        md.append("**Status:** ").append(version.getStatus()).append("\n\n");
        if (version.getChangeDescription() != null && !version.getChangeDescription().isBlank()) {
            md.append("**Change description:** ")
                    .append(version.getChangeDescription())
                    .append("\n\n");
        }

        md.append("## Idea\n\n").append(version.getIdeaSnapshot()).append("\n\n");

        renderClarifyingQuestions(md, version.getClarifyingQuestions());
        renderRequirements(md, version.getRequirements());
        renderRoadmap(md, version.getEpics());
        renderArchitectureRecommendations(md, version.getArchitectureRecommendations());

        return md.toString();
    }

    private void renderClarifyingQuestions(StringBuilder md, List<ClarifyingQuestion> questions) {
        if (questions.isEmpty()) {
            return;
        }

        md.append("## Clarifying Questions\n\n");
        int number = 1;
        for (ClarifyingQuestion question : questions) {
            ClarifyingAnswer answer = question.getAnswer();
            md.append(number++).append(". **").append(question.getQuestionText()).append("**\n");
            md.append("   ").append(answer != null ? answer.getAnswerText() : "_Not yet answered._").append("\n\n");
        }
    }

    private void renderRequirements(StringBuilder md, List<Requirement> requirements) {
        if (requirements.isEmpty()) {
            return;
        }

        md.append("## Requirements\n\n");
        for (RequirementType type : REQUIREMENT_TYPE_ORDER) {
            List<Requirement> ofType = requirements.stream()
                    .filter(requirement -> requirement.getType() == type)
                    .toList();
            if (ofType.isEmpty()) {
                continue;
            }

            md.append("### ").append(displayName(type)).append("\n\n");
            for (Requirement requirement : ofType) {
                md.append("- **")
                        .append(requirement.getTitle())
                        .append("** — ")
                        .append(requirement.getDescription())
                        .append("\n");
            }
            md.append("\n");
        }
    }

    private void renderRoadmap(StringBuilder md, List<Epic> epics) {
        if (epics.isEmpty()) {
            return;
        }

        md.append("## Roadmap\n\n");
        for (Epic epic : epics) {
            md.append("### ").append(epic.getTitle()).append("\n\n");
            md.append(epic.getDescription()).append("\n\n");

            for (UserStory userStory : epic.getUserStories()) {
                md.append("#### ").append(userStory.getTitle()).append("\n\n");
                md.append(userStory.getDescription()).append("\n\n");
                md.append("**Acceptance Criteria:**\n\n").append(userStory.getAcceptanceCriteria()).append("\n\n");

                if (!userStory.getTasks().isEmpty()) {
                    md.append("**Tasks:**\n\n");
                    for (Task task : userStory.getTasks()) {
                        md.append("- `[")
                                .append(task.getPriority())
                                .append(" / ")
                                .append(task.getEffortEstimate())
                                .append("]` ")
                                .append(task.getTitle())
                                .append(" — ")
                                .append(task.getDescription())
                                .append("\n");
                    }
                    md.append("\n");
                }
            }
        }
    }

    private void renderArchitectureRecommendations(StringBuilder md, List<ArchitectureRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return;
        }

        md.append("## Architecture Recommendations\n\n");
        for (ArchitectureRecommendation recommendation : recommendations) {
            md.append("### ").append(recommendation.getComponent()).append("\n\n");
            md.append("**Recommendation:** ")
                    .append(recommendation.getRecommendation())
                    .append("\n\n");
            md.append("**Reasoning:** ").append(recommendation.getReasoning()).append("\n\n");
            md.append("**Trade-offs:** ").append(recommendation.getTradeoffs()).append("\n\n");
        }
    }

    private static String displayName(RequirementType type) {
        return switch (type) {
            case FUNCTIONAL -> "Functional";
            case NON_FUNCTIONAL -> "Non-Functional";
            case CONSTRAINT -> "Constraints";
            case ASSUMPTION -> "Assumptions";
        };
    }

    private static String slugify(String name) {
        String slug = NON_SLUG_CHARS.matcher(name.toLowerCase()).replaceAll("-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "project" : slug;
    }
}
