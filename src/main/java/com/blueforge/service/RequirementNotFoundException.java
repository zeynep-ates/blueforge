package com.blueforge.service;

public class RequirementNotFoundException extends RuntimeException {

    public RequirementNotFoundException(Long requirementId) {
        super("No requirement found with id " + requirementId);
    }
}
