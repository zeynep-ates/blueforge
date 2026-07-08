package com.blueforge.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(@NotBlank String name, @NotBlank String ideaDescription) {}
