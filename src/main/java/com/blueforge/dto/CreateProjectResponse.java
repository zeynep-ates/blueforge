package com.blueforge.dto;

import java.util.List;

public record CreateProjectResponse(Long projectId, Long versionId, List<ClarifyingQuestionResponse> questions) {}
