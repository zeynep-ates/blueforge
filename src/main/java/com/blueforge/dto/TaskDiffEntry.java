package com.blueforge.dto;

import com.blueforge.entity.ChangeType;

public record TaskDiffEntry(ChangeType changeType, TaskResponse before, TaskResponse after) {}
