package com.blueforge.service;

import com.blueforge.entity.ProjectVersionStatus;

public class InvalidRegenerationTargetException extends RuntimeException {

    public InvalidRegenerationTargetException(ProjectVersionStatus targetStage) {
        super(targetStage + " is not a regenerable stage; must be one of " + ProjectVersionStatus.REQUIREMENTS_GENERATED
                + ", " + ProjectVersionStatus.EPICS_GENERATED + ", " + ProjectVersionStatus.USER_STORIES_GENERATED
                + ", " + ProjectVersionStatus.TASKS_GENERATED);
    }
}
