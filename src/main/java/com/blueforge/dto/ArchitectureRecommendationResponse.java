package com.blueforge.dto;

public record ArchitectureRecommendationResponse(
        Long id, String component, String recommendation, String reasoning, String tradeoffs, int orderIndex) {}
