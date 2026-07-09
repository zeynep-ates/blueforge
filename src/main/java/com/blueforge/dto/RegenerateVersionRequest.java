package com.blueforge.dto;

import com.blueforge.entity.ProjectVersionStatus;
import jakarta.validation.constraints.NotNull;

public record RegenerateVersionRequest(@NotNull ProjectVersionStatus targetStage, String changeDescription) {}
