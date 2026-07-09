package com.blueforge.dto;

import com.blueforge.entity.ProjectVersionStatus;
import java.util.List;

public record ProjectVersionResponse(
        Long versionId,
        Long projectId,
        int versionNumber,
        String ideaSnapshot,
        String changeDescription,
        ProjectVersionStatus status,
        List<ClarifyingQuestionResponse> questions,
        List<RequirementResponse> requirements,
        List<EpicResponse> epics) {}
