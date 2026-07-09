package com.blueforge.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEpicRequest(@NotBlank String title, @NotBlank String description) {}
