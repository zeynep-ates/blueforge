package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.UpdateEpicRequest;
import com.blueforge.entity.Epic;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.repository.EpicRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EpicServiceTest {

    @Mock
    private EpicRepository epicRepository;

    private EpicService epicService;

    private EpicService newService() {
        return new EpicService(epicRepository);
    }

    @Test
    void updateEpicPersistsNewTitleAndDescription() {
        epicService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version = new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.EPICS_GENERATED);
        version.setId(10L);
        Epic epic = new Epic(version, "Original title", "Original description", 0);
        epic.setId(300L);

        when(epicRepository.findById(300L)).thenReturn(Optional.of(epic));
        when(epicRepository.save(any(Epic.class))).thenAnswer(inv -> inv.getArgument(0));

        EpicResponse response = epicService.updateEpic(300L, new UpdateEpicRequest("New title", "New description"));

        assertThat(response.id()).isEqualTo(300L);
        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.orderIndex()).isEqualTo(0);
    }

    @Test
    void updateEpicThrowsNotFoundWhenMissing() {
        epicService = newService();

        when(epicRepository.findById(300L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> epicService.updateEpic(300L, new UpdateEpicRequest("title", "description")))
                .isInstanceOf(EpicNotFoundException.class);
    }
}
