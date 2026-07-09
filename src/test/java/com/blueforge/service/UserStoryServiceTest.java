package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.dto.UpdateUserStoryRequest;
import com.blueforge.dto.UserStoryResponse;
import com.blueforge.entity.Epic;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.UserStory;
import com.blueforge.repository.UserStoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserStoryServiceTest {

    @Mock
    private UserStoryRepository userStoryRepository;

    private UserStoryService userStoryService;

    private UserStoryService newService() {
        return new UserStoryService(userStoryRepository);
    }

    @Test
    void updateUserStoryPersistsNewFields() {
        userStoryService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.USER_STORIES_GENERATED);
        version.setId(10L);
        Epic epic = new Epic(version, "Epic title", "Epic description", 0);
        epic.setId(300L);
        UserStory userStory =
                new UserStory(epic, "Original title", "Original description", "Original criteria", 0);
        userStory.setId(400L);

        when(userStoryRepository.findById(400L)).thenReturn(Optional.of(userStory));
        when(userStoryRepository.save(any(UserStory.class))).thenAnswer(inv -> inv.getArgument(0));

        UserStoryResponse response = userStoryService.updateUserStory(
                400L, new UpdateUserStoryRequest("New title", "New description", "New criteria"));

        assertThat(response.id()).isEqualTo(400L);
        assertThat(response.epicId()).isEqualTo(300L);
        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.acceptanceCriteria()).isEqualTo("New criteria");
        assertThat(response.orderIndex()).isEqualTo(0);
    }

    @Test
    void updateUserStoryThrowsNotFoundWhenMissing() {
        userStoryService = newService();

        when(userStoryRepository.findById(400L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userStoryService.updateUserStory(
                        400L, new UpdateUserStoryRequest("title", "description", "criteria")))
                .isInstanceOf(UserStoryNotFoundException.class);
    }
}
