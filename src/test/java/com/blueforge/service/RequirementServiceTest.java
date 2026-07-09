package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.UpdateRequirementRequest;
import com.blueforge.entity.Project;
import com.blueforge.entity.ProjectVersion;
import com.blueforge.entity.ProjectVersionStatus;
import com.blueforge.entity.Requirement;
import com.blueforge.entity.RequirementType;
import com.blueforge.repository.RequirementRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    private RequirementService requirementService;

    private RequirementService newService() {
        return new RequirementService(requirementRepository);
    }

    @Test
    void updateRequirementPersistsNewTitleAndDescription() {
        requirementService = newService();

        Project project = new Project("Test Project");
        project.setId(1L);
        ProjectVersion version =
                new ProjectVersion(project, 1, "An idea", ProjectVersionStatus.REQUIREMENTS_GENERATED);
        version.setId(10L);
        Requirement requirement = new Requirement(
                version, RequirementType.FUNCTIONAL, "Original title", "Original description", 0);
        requirement.setId(200L);

        when(requirementRepository.findById(200L)).thenReturn(Optional.of(requirement));
        when(requirementRepository.save(any(Requirement.class))).thenAnswer(inv -> inv.getArgument(0));

        RequirementResponse response =
                requirementService.updateRequirement(200L, new UpdateRequirementRequest("New title", "New description"));

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.type()).isEqualTo(RequirementType.FUNCTIONAL);
        assertThat(response.orderIndex()).isEqualTo(0);
    }

    @Test
    void updateRequirementThrowsNotFoundWhenMissing() {
        requirementService = newService();

        when(requirementRepository.findById(200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        requirementService.updateRequirement(200L, new UpdateRequirementRequest("title", "description")))
                .isInstanceOf(RequirementNotFoundException.class);
    }
}
