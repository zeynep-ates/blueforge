package com.blueforge.service;

public class UserStoryNotFoundException extends RuntimeException {

    public UserStoryNotFoundException(Long userStoryId) {
        super("No user story found with id " + userStoryId);
    }
}
