package com.blueforge.dto;

import com.blueforge.entity.ProjectVersionStatus;
import java.time.Instant;

public record ProjectSummaryResponse(
        Long id, String name, Instant createdAt, int latestVersionNumber, ProjectVersionStatus latestStatus) {}
