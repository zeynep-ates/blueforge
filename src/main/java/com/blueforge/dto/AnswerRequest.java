package com.blueforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerRequest(@NotNull Long questionId, @NotBlank String answerText) {}
