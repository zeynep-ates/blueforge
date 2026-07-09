package com.blueforge.service;

import com.blueforge.entity.ProjectVersionStatus;

public class ProjectVersionNotAwaitingAnswersException extends RuntimeException {

    public ProjectVersionNotAwaitingAnswersException(
            Long projectId, int versionNumber, ProjectVersionStatus currentStatus) {
        super("Version " + versionNumber + " of project " + projectId
                + " is not awaiting answers (current status: " + currentStatus + ")");
    }
}
