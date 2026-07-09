package com.blueforge.dto;

import com.blueforge.entity.ChangeType;
import java.util.List;

public record UserStoryDiffEntry(
        ChangeType changeType, UserStoryResponse before, UserStoryResponse after, List<TaskDiffEntry> tasks) {}
