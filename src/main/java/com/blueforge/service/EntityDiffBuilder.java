package com.blueforge.service;

import com.blueforge.dto.ArchitectureRecommendationDiffEntry;
import com.blueforge.dto.ArchitectureRecommendationResponse;
import com.blueforge.dto.EpicDiffEntry;
import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.RequirementDiffEntry;
import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.TaskDiffEntry;
import com.blueforge.dto.TaskResponse;
import com.blueforge.dto.UserStoryDiffEntry;
import com.blueforge.dto.UserStoryResponse;
import com.blueforge.entity.ArchitectureRecommendation;
import com.blueforge.entity.ChangeType;
import com.blueforge.entity.Epic;
import com.blueforge.entity.Requirement;
import com.blueforge.entity.Task;
import com.blueforge.entity.UserStory;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import org.springframework.stereotype.Component;

// Classifies a single matched pair and builds its diff entry. Has no knowledge of parent/child relationships
// between entity types; VersionDiffService drives the tree walk and passes already-built children in.
@Component
public class EntityDiffBuilder {

    public RequirementDiffEntry buildRequirementDiff(MatchedPair<Requirement> pair) {
        ChangeType changeType = classify(pair, EntityDiffBuilder::requirementFieldsEqual);
        return new RequirementDiffEntry(changeType, toResponse(pair.before()), toResponse(pair.after()));
    }

    public EpicDiffEntry buildEpicDiff(MatchedPair<Epic> pair, List<UserStoryDiffEntry> userStories) {
        ChangeType changeType = classify(pair, EntityDiffBuilder::epicFieldsEqual);
        return new EpicDiffEntry(changeType, toResponse(pair.before()), toResponse(pair.after()), userStories);
    }

    public UserStoryDiffEntry buildUserStoryDiff(MatchedPair<UserStory> pair, List<TaskDiffEntry> tasks) {
        ChangeType changeType = classify(pair, EntityDiffBuilder::userStoryFieldsEqual);
        return new UserStoryDiffEntry(changeType, toResponse(pair.before()), toResponse(pair.after()), tasks);
    }

    public TaskDiffEntry buildTaskDiff(MatchedPair<Task> pair) {
        ChangeType changeType = classify(pair, EntityDiffBuilder::taskFieldsEqual);
        return new TaskDiffEntry(changeType, toResponse(pair.before()), toResponse(pair.after()));
    }

    public ArchitectureRecommendationDiffEntry buildArchitectureRecommendationDiff(
            MatchedPair<ArchitectureRecommendation> pair) {
        ChangeType changeType = classify(pair, EntityDiffBuilder::architectureRecommendationFieldsEqual);
        return new ArchitectureRecommendationDiffEntry(changeType, toResponse(pair.before()), toResponse(pair.after()));
    }

    private static <T> ChangeType classify(MatchedPair<T> pair, BiPredicate<T, T> fieldsEqual) {
        if (pair.before() == null) {
            return ChangeType.ADDED;
        }
        if (pair.after() == null) {
            return ChangeType.REMOVED;
        }
        return fieldsEqual.test(pair.before(), pair.after()) ? ChangeType.UNCHANGED : ChangeType.MODIFIED;
    }

    private static boolean requirementFieldsEqual(Requirement a, Requirement b) {
        return Objects.equals(a.getType(), b.getType())
                && Objects.equals(a.getTitle(), b.getTitle())
                && Objects.equals(a.getDescription(), b.getDescription());
    }

    private static boolean epicFieldsEqual(Epic a, Epic b) {
        return Objects.equals(a.getTitle(), b.getTitle()) && Objects.equals(a.getDescription(), b.getDescription());
    }

    private static boolean userStoryFieldsEqual(UserStory a, UserStory b) {
        return Objects.equals(a.getTitle(), b.getTitle())
                && Objects.equals(a.getDescription(), b.getDescription())
                && Objects.equals(a.getAcceptanceCriteria(), b.getAcceptanceCriteria());
    }

    private static boolean taskFieldsEqual(Task a, Task b) {
        return Objects.equals(a.getTitle(), b.getTitle())
                && Objects.equals(a.getDescription(), b.getDescription())
                && Objects.equals(a.getPriority(), b.getPriority())
                && Objects.equals(a.getEffortEstimate(), b.getEffortEstimate());
    }

    private static boolean architectureRecommendationFieldsEqual(
            ArchitectureRecommendation a, ArchitectureRecommendation b) {
        return Objects.equals(a.getComponent(), b.getComponent())
                && Objects.equals(a.getRecommendation(), b.getRecommendation())
                && Objects.equals(a.getReasoning(), b.getReasoning())
                && Objects.equals(a.getTradeoffs(), b.getTradeoffs());
    }

    private static RequirementResponse toResponse(Requirement r) {
        return r == null
                ? null
                : new RequirementResponse(r.getId(), r.getType(), r.getTitle(), r.getDescription(), r.getOrderIndex());
    }

    private static EpicResponse toResponse(Epic e) {
        return e == null ? null : new EpicResponse(e.getId(), e.getTitle(), e.getDescription(), e.getOrderIndex());
    }

    private static UserStoryResponse toResponse(UserStory s) {
        return s == null
                ? null
                : new UserStoryResponse(
                        s.getId(),
                        s.getEpic().getId(),
                        s.getTitle(),
                        s.getDescription(),
                        s.getAcceptanceCriteria(),
                        s.getOrderIndex());
    }

    private static TaskResponse toResponse(Task t) {
        return t == null
                ? null
                : new TaskResponse(
                        t.getId(),
                        t.getUserStory().getId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getPriority(),
                        t.getEffortEstimate(),
                        t.getOrderIndex());
    }

    private static ArchitectureRecommendationResponse toResponse(ArchitectureRecommendation a) {
        return a == null
                ? null
                : new ArchitectureRecommendationResponse(
                        a.getId(), a.getComponent(), a.getRecommendation(), a.getReasoning(), a.getTradeoffs(),
                        a.getOrderIndex());
    }
}
