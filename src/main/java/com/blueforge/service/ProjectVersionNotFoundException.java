package com.blueforge.service;

public class ProjectVersionNotFoundException extends RuntimeException {

    public ProjectVersionNotFoundException(Long projectId, int versionNumber) {
        super("No version " + versionNumber + " found for project " + projectId);
    }
}
