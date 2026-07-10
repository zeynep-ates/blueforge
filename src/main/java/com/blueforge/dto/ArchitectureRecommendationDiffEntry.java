package com.blueforge.dto;

import com.blueforge.entity.ChangeType;

public record ArchitectureRecommendationDiffEntry(
        ChangeType changeType, ArchitectureRecommendationResponse before, ArchitectureRecommendationResponse after) {}
