package com.blueforge.dto;

import com.blueforge.entity.ProjectVersionStatus;

public record ProjectVersionSummaryResponse(
        Long versionId, int versionNumber, ProjectVersionStatus status, String changeDescription) {}
