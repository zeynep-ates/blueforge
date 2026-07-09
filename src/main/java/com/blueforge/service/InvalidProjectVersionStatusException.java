package com.blueforge.service;

import com.blueforge.entity.ProjectVersionStatus;

public class InvalidProjectVersionStatusException extends RuntimeException {

    public InvalidProjectVersionStatusException(
            Long projectId,
            int versionNumber,
            ProjectVersionStatus expectedStatus,
            ProjectVersionStatus actualStatus) {
        super("Version " + versionNumber + " of project " + projectId
                + " must have status " + expectedStatus + " (current status: " + actualStatus + ")");
    }
}
