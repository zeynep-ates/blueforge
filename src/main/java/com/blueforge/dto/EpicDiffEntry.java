package com.blueforge.dto;

import com.blueforge.entity.ChangeType;
import java.util.List;

public record EpicDiffEntry(
        ChangeType changeType, EpicResponse before, EpicResponse after, List<UserStoryDiffEntry> userStories) {}
