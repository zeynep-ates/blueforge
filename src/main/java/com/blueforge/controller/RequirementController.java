package com.blueforge.controller;

import com.blueforge.dto.RequirementResponse;
import com.blueforge.dto.UpdateRequirementRequest;
import com.blueforge.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PatchMapping("/{requirementId}")
    public ResponseEntity<RequirementResponse> updateRequirement(
            @PathVariable Long requirementId, @Valid @RequestBody UpdateRequirementRequest request) {
        return ResponseEntity.ok(requirementService.updateRequirement(requirementId, request));
    }
}
