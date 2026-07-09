package com.blueforge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitAnswersRequest(@NotEmpty @Valid List<AnswerRequest> answers) {}
