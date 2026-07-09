package com.blueforge.service;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long projectId) {
        super("No project found with id " + projectId);
    }
}
