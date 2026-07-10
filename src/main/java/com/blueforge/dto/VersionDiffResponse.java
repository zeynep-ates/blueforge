package com.blueforge.dto;

import java.util.List;

public record VersionDiffResponse(
        Long projectId,
        int fromVersionNumber,
        int toVersionNumber,
        DiffSummary summary,
        List<RequirementDiffEntry> requirements,
        List<EpicDiffEntry> epics,
        List<ArchitectureRecommendationDiffEntry> architectureRecommendations) {}
