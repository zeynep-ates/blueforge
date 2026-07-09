package com.blueforge.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStoryRequest(
        @NotBlank String title, @NotBlank String description, @NotBlank String acceptanceCriteria) {}
