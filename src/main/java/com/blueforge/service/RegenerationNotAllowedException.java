package com.blueforge.service;

import com.blueforge.entity.ProjectVersionStatus;

public class RegenerationNotAllowedException extends RuntimeException {

    public RegenerationNotAllowedException(
            Long projectId, int versionNumber, ProjectVersionStatus targetStage, ProjectVersionStatus actualStatus) {
        super("Version " + versionNumber + " of project " + projectId + " has not yet reached " + targetStage
                + " (current status: " + actualStatus + "); that stage cannot be regenerated yet");
    }
}
