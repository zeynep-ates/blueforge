package com.blueforge.service;

import com.blueforge.dto.ArchitectureRecommendationDiffEntry;
import com.blueforge.dto.DiffSummary;
import com.blueforge.dto.EpicDiffEntry;
import com.blueforge.dto.RequirementDiffEntry;
import com.blueforge.dto.TaskDiffEntry;
import com.blueforge.dto.UserStoryDiffEntry;
import com.blueforge.dto.VersionDiffResponse;
import com.blueforge.entity.ChangeType;
import com.blueforge.entity.Epic;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.Task;
import com.blueforge.entity.UserStory;
import com.blueforge.repository.ProjectVersionRepository;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionDiffService {

    private final ProjectVersionRepository projectVersionRepository;
    private final VersionEntityMatcher matcher;
    private final EntityDiffBuilder diffBuilder;

    public VersionDiffService(
            ProjectVersionRepository projectVersionRepository,
            VersionEntityMatcher matcher,
            EntityDiffBuilder diffBuilder) {
        this.projectVersionRepository = projectVersionRepository;
        this.matcher = matcher;
        this.diffBuilder = diffBuilder;
    }

    @Transactional(readOnly = true)
    public VersionDiffResponse diff(Long projectId, int fromVersionNumber, int toVersionNumber) {
        ProjectVersion from = findVersionOrThrow(projectId, fromVersionNumber);
        ProjectVersion to = findVersionOrThrow(projectId, toVersionNumber);

        List<RequirementDiffEntry> requirements = matcher.match(from.getRequirements(), to.getRequirements()).stream()
                .map(diffBuilder::buildRequirementDiff)
                .toList();

        List<EpicDiffEntry> epics = matcher.match(from.getEpics(), to.getEpics()).stream()
                .map(this::buildEpicDiff)
                .toList();

        List<ArchitectureRecommendationDiffEntry> architectureRecommendations = matcher
                .match(from.getArchitectureRecommendations(), to.getArchitectureRecommendations())
                .stream()
                .map(diffBuilder::buildArchitectureRecommendationDiff)
                .toList();

        return new VersionDiffResponse(
                projectId,
                fromVersionNumber,
                toVersionNumber,
                summarize(requirements, epics, architectureRecommendations),
                requirements,
                epics,
                architectureRecommendations);
    }

    private EpicDiffEntry buildEpicDiff(MatchedPair<Epic> pair) {
        List<UserStory> beforeStories = pair.before() != null ? pair.before().getUserStories() : List.of();
        List<UserStory> afterStories = pair.after() != null ? pair.after().getUserStories() : List.of();
        List<UserStoryDiffEntry> userStories = matcher.match(beforeStories, afterStories).stream()
                .map(this::buildUserStoryDiff)
                .toList();
        return diffBuilder.buildEpicDiff(pair, userStories);
    }

    private UserStoryDiffEntry buildUserStoryDiff(MatchedPair<UserStory> pair) {
        List<Task> beforeTasks = pair.before() != null ? pair.before().getTasks() : List.of();
        List<Task> afterTasks = pair.after() != null ? pair.after().getTasks() : List.of();
        List<TaskDiffEntry> tasks = matcher.match(beforeTasks, afterTasks).stream()
                .map(diffBuilder::buildTaskDiff)
                .toList();
        return diffBuilder.buildUserStoryDiff(pair, tasks);
    }

    private ProjectVersion findVersionOrThrow(Long projectId, int versionNumber) {
        return projectVersionRepository
                .findByProjectIdAndVersionNumber(projectId, versionNumber)
                .orElseThrow(() -> new ProjectVersionNotFoundException(projectId, versionNumber));
    }

    private static DiffSummary summarize(
            List<RequirementDiffEntry> requirements,
            List<EpicDiffEntry> epics,
            List<ArchitectureRecommendationDiffEntry> architectureRecommendations) {
        int added = 0;
        int removed = 0;
        int modified = 0;
        int unchanged = 0;

        for (ChangeType type : allChangeTypes(requirements, epics, architectureRecommendations).toList()) {
            switch (type) {
                case ADDED -> added++;
                case REMOVED -> removed++;
                case MODIFIED -> modified++;
                case UNCHANGED -> unchanged++;
            }
        }

        return new DiffSummary(added, removed, modified, unchanged);
    }

    private static Stream<ChangeType> allChangeTypes(
            List<RequirementDiffEntry> requirements,
            List<EpicDiffEntry> epics,
            List<ArchitectureRecommendationDiffEntry> architectureRecommendations) {
        return Stream.concat(
                Stream.concat(
                        requirements.stream().map(RequirementDiffEntry::changeType),
                        epics.stream().flatMap(VersionDiffService::epicChangeTypes)),
                architectureRecommendations.stream().map(ArchitectureRecommendationDiffEntry::changeType));
    }

    private static Stream<ChangeType> epicChangeTypes(EpicDiffEntry epic) {
        return Stream.concat(
                Stream.of(epic.changeType()), epic.userStories().stream().flatMap(VersionDiffService::userStoryChangeTypes));
    }

    private static Stream<ChangeType> userStoryChangeTypes(UserStoryDiffEntry userStory) {
        return Stream.concat(Stream.of(userStory.changeType()), userStory.tasks().stream().map(TaskDiffEntry::changeType));
    }
}
