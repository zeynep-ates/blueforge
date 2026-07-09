package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.blueforge.dto.DiffSummary;
import com.blueforge.dto.EpicDiffEntry;
import com.blueforge.dto.RequirementDiffEntry;
import com.blueforge.dto.TaskDiffEntry;
import com.blueforge.dto.UserStoryDiffEntry;
import com.blueforge.dto.VersionDiffResponse;
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
import com.blueforge.repository.ProjectVersionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionDiffServiceTest {

    @Mock
    private ProjectVersionRepository projectVersionRepository;

    private VersionDiffService versionDiffService;

    @BeforeEach
    void setUp() {
        versionDiffService =
                new VersionDiffService(projectVersionRepository, new PositionalEntityMatcher(), new EntityDiffBuilder());
    }

    private Project project() {
        Project project = new Project("Test Project");
        project.setId(1L);
        return project;
    }

    @Test
    void diffsRequirementsAtTopLevel() {
        Project project = project();
        ProjectVersion from = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        from.setId(10L);
        Requirement r1 = new Requirement(from, RequirementType.FUNCTIONAL, "Same", "Same description", 0);
        r1.setId(200L);
        Requirement r2 = new Requirement(from, RequirementType.FUNCTIONAL, "Removed req", "Description", 1);
        r2.setId(201L);
        from.getRequirements().addAll(List.of(r1, r2));

        ProjectVersion to = new ProjectVersion(project, 2, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        to.setId(11L);
        Requirement r1Clone = new Requirement(to, RequirementType.FUNCTIONAL, "Same", "Same description", 0);
        r1Clone.setId(300L);
        Requirement r3 = new Requirement(to, RequirementType.FUNCTIONAL, "Added req", "Description", 1);
        r3.setId(301L);
        to.getRequirements().addAll(List.of(r1Clone, r3));

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(from));
        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 2)).thenReturn(Optional.of(to));

        VersionDiffResponse response = versionDiffService.diff(1L, 1, 2);

        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.fromVersionNumber()).isEqualTo(1);
        assertThat(response.toVersionNumber()).isEqualTo(2);
        assertThat(response.requirements()).hasSize(2);
        assertThat(response.requirements().get(0).changeType()).isEqualTo(ChangeType.UNCHANGED);
        assertThat(response.requirements().get(1).changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(response.requirements().get(1).before().title()).isEqualTo("Removed req");
        assertThat(response.requirements().get(1).after().title()).isEqualTo("Added req");
        assertThat(response.summary()).isEqualTo(new DiffSummary(0, 0, 1, 1));
    }

    @Test
    void cascadesAddedEpicToAllDescendants() {
        Project project = project();
        ProjectVersion from = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        from.setId(10L);

        ProjectVersion to = new ProjectVersion(project, 2, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        to.setId(11L);
        Epic newEpic = new Epic(to, "New epic", "Description", 0);
        newEpic.setId(300L);
        UserStory newStory = new UserStory(newEpic, "New story", "Description", "- criteria", 0);
        newStory.setId(400L);
        Task newTask = new Task(newStory, "New task", "Description", TaskPriority.HIGH, TaskEffort.M, 0);
        newStory.getTasks().add(newTask);
        newEpic.getUserStories().add(newStory);
        to.getEpics().add(newEpic);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(from));
        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 2)).thenReturn(Optional.of(to));

        VersionDiffResponse response = versionDiffService.diff(1L, 1, 2);

        assertThat(response.epics()).hasSize(1);
        EpicDiffEntry epicDiff = response.epics().get(0);
        assertThat(epicDiff.changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(epicDiff.before()).isNull();
        assertThat(epicDiff.userStories()).hasSize(1);
        UserStoryDiffEntry storyDiff = epicDiff.userStories().get(0);
        assertThat(storyDiff.changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(storyDiff.tasks()).hasSize(1);
        TaskDiffEntry taskDiff = storyDiff.tasks().get(0);
        assertThat(taskDiff.changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(taskDiff.before()).isNull();
        assertThat(taskDiff.after().title()).isEqualTo("New task");

        assertThat(response.summary()).isEqualTo(new DiffSummary(3, 0, 0, 0));
    }

    @Test
    void cascadesRemovedEpicToAllDescendants() {
        Project project = project();
        ProjectVersion from = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        from.setId(10L);
        Epic oldEpic = new Epic(from, "Old epic", "Description", 0);
        oldEpic.setId(300L);
        UserStory oldStory = new UserStory(oldEpic, "Old story", "Description", "- criteria", 0);
        oldStory.setId(400L);
        oldEpic.getUserStories().add(oldStory);
        from.getEpics().add(oldEpic);

        ProjectVersion to = new ProjectVersion(project, 2, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        to.setId(11L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(from));
        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 2)).thenReturn(Optional.of(to));

        VersionDiffResponse response = versionDiffService.diff(1L, 1, 2);

        EpicDiffEntry epicDiff = response.epics().get(0);
        assertThat(epicDiff.changeType()).isEqualTo(ChangeType.REMOVED);
        assertThat(epicDiff.after()).isNull();
        assertThat(epicDiff.userStories()).hasSize(1);
        assertThat(epicDiff.userStories().get(0).changeType()).isEqualTo(ChangeType.REMOVED);
        assertThat(epicDiff.userStories().get(0).tasks()).isEmpty();

        assertThat(response.summary()).isEqualTo(new DiffSummary(0, 2, 0, 0));
    }

    @Test
    void comparingAVersionToItselfIsAllUnchanged() {
        Project project = project();
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        version.setId(10L);
        Requirement requirement = new Requirement(version, RequirementType.FUNCTIONAL, "Title", "Description", 0);
        requirement.setId(200L);
        version.getRequirements().add(requirement);
        Epic epic = new Epic(version, "Epic", "Description", 0);
        epic.setId(300L);
        version.getEpics().add(epic);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));

        VersionDiffResponse response = versionDiffService.diff(1L, 1, 1);

        assertThat(response.summary()).isEqualTo(new DiffSummary(0, 0, 0, 2));
    }

    @Test
    void throwsNotFoundWhenFromVersionMissing() {
        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> versionDiffService.diff(1L, 1, 2))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void throwsNotFoundWhenToVersionMissing() {
        Project project = project();
        ProjectVersion from = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        from.setId(10L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(from));
        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> versionDiffService.diff(1L, 1, 2))
                .isInstanceOf(ProjectVersionNotFoundException.class);
    }

    @Test
    void requirementDiffEntryOrderIndexIsExposedOnResponses() {
        Project project = project();
        ProjectVersion from = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        from.setId(10L);
        Requirement requirement = new Requirement(from, RequirementType.FUNCTIONAL, "Title", "Description", 0);
        requirement.setId(200L);
        from.getRequirements().add(requirement);

        ProjectVersion to = new ProjectVersion(project, 2, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        to.setId(11L);

        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(from));
        when(projectVersionRepository.findByProjectIdAndVersionNumber(1L, 2)).thenReturn(Optional.of(to));

        VersionDiffResponse response = versionDiffService.diff(1L, 1, 2);

        RequirementDiffEntry entry = response.requirements().get(0);
        assertThat(entry.changeType()).isEqualTo(ChangeType.REMOVED);
        assertThat(entry.before().orderIndex()).isEqualTo(0);
    }
}
