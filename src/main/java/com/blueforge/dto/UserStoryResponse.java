package com.blueforge.dto;

public record UserStoryResponse(
        Long id, Long epicId, String title, String description, String acceptanceCriteria, int orderIndex) {}
