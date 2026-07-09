package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.dto.TaskResponse;
import com.blueforge.dto.UpdateTaskRequest;
import com.blueforge.entity.Epic;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.Task;
import com.blueforge.entity.TaskEffort;
import com.blueforge.entity.TaskPriority;
import com.blueforge.entity.UserStory;
import com.blueforge.repository.TaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    private TaskService newService() {
        return new TaskService(taskRepository);
    }

    @Test
    void updateTaskPersistsNewTitleAndDescription() {
        taskService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.TASKS_GENERATED);
        version.setId(10L);
        Epic epic = new Epic(version, "Epic title", "Epic description", 0);
        epic.setId(300L);
        UserStory userStory = new UserStory(epic, "Story title", "Story description", "Criteria", 0);
        userStory.setId(400L);
        Task task = new Task(
                userStory, "Original title", "Original description", TaskPriority.HIGH, TaskEffort.M, 0);
        task.setId(500L);

        when(taskRepository.findById(500L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response =
                taskService.updateTask(500L, new UpdateTaskRequest("New title", "New description"));

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.userStoryId()).isEqualTo(400L);
        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.effortEstimate()).isEqualTo(TaskEffort.M);
        assertThat(response.orderIndex()).isEqualTo(0);
    }

    @Test
    void updateTaskThrowsNotFoundWhenMissing() {
        taskService = newService();

        when(taskRepository.findById(500L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(500L, new UpdateTaskRequest("title", "description")))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
