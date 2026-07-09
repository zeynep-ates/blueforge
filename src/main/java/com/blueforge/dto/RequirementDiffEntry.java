package com.blueforge.dto;

import com.blueforge.entity.ChangeType;

public record RequirementDiffEntry(ChangeType changeType, RequirementResponse before, RequirementResponse after) {}
