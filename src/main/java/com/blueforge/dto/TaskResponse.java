package com.blueforge.dto;

import com.blueforge.entity.TaskEffort;
import com.blueforge.entity.TaskPriority;

public record TaskResponse(
        Long id,
        Long userStoryId,
        String title,
        String description,
        TaskPriority priority,
        TaskEffort effortEstimate,
        int orderIndex) {}
