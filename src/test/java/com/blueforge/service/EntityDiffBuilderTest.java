package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.blueforge.dto.EpicDiffEntry;
import com.blueforge.dto.RequirementDiffEntry;
import com.blueforge.dto.TaskDiffEntry;
import com.blueforge.dto.UserStoryDiffEntry;
import com.blueforge.entity.ChangeType;
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
import java.util.List;
import org.junit.jupiter.api.Test;

class EntityDiffBuilderTest {

    private final EntityDiffBuilder diffBuilder = new EntityDiffBuilder();

    private ProjectVersion version() {
        Project project = new Project("Test Project");
        project.setId(1L);
        return new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
    }

    @Test
    void classifiesRequirementAsAddedWhenBeforeIsNull() {
        Requirement after = new Requirement(version(), RequirementType.FUNCTIONAL, "Title", "Description", 0);
        after.setId(200L);

        RequirementDiffEntry entry = diffBuilder.buildRequirementDiff(new MatchedPair<>(null, after));

        assertThat(entry.changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(entry.before()).isNull();
        assertThat(entry.after().title()).isEqualTo("Title");
    }

    @Test
    void classifiesRequirementAsRemovedWhenAfterIsNull() {
        Requirement before = new Requirement(version(), RequirementType.FUNCTIONAL, "Title", "Description", 0);
        before.setId(200L);

        RequirementDiffEntry entry = diffBuilder.buildRequirementDiff(new MatchedPair<>(before, null));

        assertThat(entry.changeType()).isEqualTo(ChangeType.REMOVED);
        assertThat(entry.after()).isNull();
        assertThat(entry.before().title()).isEqualTo("Title");
    }

    @Test
    void classifiesRequirementAsUnchangedWhenAllComparedFieldsMatch() {
        Requirement before = new Requirement(version(), RequirementType.FUNCTIONAL, "Title", "Description", 0);
        before.setId(200L);
        Requirement after = new Requirement(version(), RequirementType.FUNCTIONAL, "Title", "Description", 0);
        after.setId(300L);

        RequirementDiffEntry entry = diffBuilder.buildRequirementDiff(new MatchedPair<>(before, after));

        assertThat(entry.changeType()).isEqualTo(ChangeType.UNCHANGED);
    }

    @Test
    void classifiesRequirementAsModifiedWhenDescriptionDiffers() {
        Requirement before = new Requirement(version(), RequirementType.FUNCTIONAL, "Title", "Old description", 0);
        before.setId(200L);
        Requirement after = new Requirement(version(), RequirementType.FUNCTIONAL, "Title", "New description", 0);
        after.setId(300L);

        RequirementDiffEntry entry = diffBuilder.buildRequirementDiff(new MatchedPair<>(before, after));

        assertThat(entry.changeType()).isEqualTo(ChangeType.MODIFIED);
    }

    @Test
    void classifiesRequirementAsModifiedWhenTypeDiffersEvenIfTextIsIdentical() {
        Requirement before = new Requirement(version(), RequirementType.FUNCTIONAL, "Title", "Description", 0);
        before.setId(200L);
        Requirement after = new Requirement(version(), RequirementType.NON_FUNCTIONAL, "Title", "Description", 0);
        after.setId(300L);

        RequirementDiffEntry entry = diffBuilder.buildRequirementDiff(new MatchedPair<>(before, after));

        assertThat(entry.changeType()).isEqualTo(ChangeType.MODIFIED);
    }

    @Test
    void classifiesEpicAndAttachesGivenUserStories() {
        ProjectVersion version = version();
        Epic before = new Epic(version, "Epic title", "Epic description", 0);
        before.setId(300L);
        Epic after = new Epic(version, "Epic title", "Changed description", 0);
        after.setId(301L);

        UserStoryDiffEntry childEntry = new UserStoryDiffEntry(ChangeType.UNCHANGED, null, null, List.of());

        EpicDiffEntry entry = diffBuilder.buildEpicDiff(new MatchedPair<>(before, after), List.of(childEntry));

        assertThat(entry.changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(entry.userStories()).containsExactly(childEntry);
    }

    @Test
    void classifiesUserStoryConsideringAcceptanceCriteria() {
        ProjectVersion version = version();
        Epic epic = new Epic(version, "Epic title", "Epic description", 0);
        epic.setId(300L);
        UserStory before = new UserStory(epic, "Story", "Description", "- old criteria", 0);
        before.setId(400L);
        UserStory after = new UserStory(epic, "Story", "Description", "- new criteria", 0);
        after.setId(401L);

        UserStoryDiffEntry entry = diffBuilder.buildUserStoryDiff(new MatchedPair<>(before, after), List.of());

        assertThat(entry.changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(entry.tasks()).isEmpty();
    }

    @Test
    void classifiesTaskConsideringPriorityAndEffort() {
        ProjectVersion version = version();
        Epic epic = new Epic(version, "Epic title", "Epic description", 0);
        epic.setId(300L);
        UserStory story = new UserStory(epic, "Story", "Description", "- criteria", 0);
        story.setId(400L);
        Task before = new Task(story, "Task", "Description", TaskPriority.LOW, TaskEffort.S, 0);
        before.setId(500L);
        Task after = new Task(story, "Task", "Description", TaskPriority.HIGH, TaskEffort.S, 0);
        after.setId(501L);

        TaskDiffEntry entry = diffBuilder.buildTaskDiff(new MatchedPair<>(before, after));

        assertThat(entry.changeType()).isEqualTo(ChangeType.MODIFIED);
    }

    @Test
    void taskUnchangedWhenAllComparedFieldsMatch() {
        ProjectVersion version = version();
        Epic epic = new Epic(version, "Epic title", "Epic description", 0);
        epic.setId(300L);
        UserStory story = new UserStory(epic, "Story", "Description", "- criteria", 0);
        story.setId(400L);
        Task before = new Task(story, "Task", "Description", TaskPriority.LOW, TaskEffort.S, 0);
        before.setId(500L);
        Task after = new Task(story, "Task", "Description", TaskPriority.LOW, TaskEffort.S, 0);
        after.setId(501L);

        TaskDiffEntry entry = diffBuilder.buildTaskDiff(new MatchedPair<>(before, after));

        assertThat(entry.changeType()).isEqualTo(ChangeType.UNCHANGED);
    }
}
