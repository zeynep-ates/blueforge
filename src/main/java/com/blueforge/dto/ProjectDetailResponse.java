package com.blueforge.dto;

import java.time.Instant;
import java.util.List;

public record ProjectDetailResponse(
        Long id, String name, Instant createdAt, List<ProjectVersionSummaryResponse> versions) {}
