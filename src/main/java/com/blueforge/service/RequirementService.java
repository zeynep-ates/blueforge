package com.blueforge.service;

import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.UpdateRequirementRequest;
import com.blueforge.entity.Requirement;
import com.blueforge.repository.RequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementService {

    private final RequirementRepository requirementRepository;

    public RequirementService(RequirementRepository requirementRepository) {
        this.requirementRepository = requirementRepository;
    }

    @Transactional
    public RequirementResponse updateRequirement(Long requirementId, UpdateRequirementRequest request) {
        Requirement requirement = requirementRepository
                .findById(requirementId)
                .orElseThrow(() -> new RequirementNotFoundException(requirementId));

        requirement.setTitle(request.title());
        requirement.setDescription(request.description());

        requirement = requirementRepository.save(requirement);

        return toRequirementResponse(requirement);
    }

    private static RequirementResponse toRequirementResponse(Requirement requirement) {
        return new RequirementResponse(
                requirement.getId(),
                requirement.getType(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getOrderIndex());
    }
}
