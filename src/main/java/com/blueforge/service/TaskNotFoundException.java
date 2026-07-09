package com.blueforge.service;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long taskId) {
        super("No task found with id " + taskId);
    }
}
