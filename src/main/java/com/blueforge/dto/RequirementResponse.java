package com.blueforge.dto;

import com.blueforge.entity.RequirementType;

public record RequirementResponse(Long id, RequirementType type, String title, String description, int orderIndex) {}
