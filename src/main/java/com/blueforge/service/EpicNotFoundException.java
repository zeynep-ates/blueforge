package com.blueforge.service;

public class EpicNotFoundException extends RuntimeException {

    public EpicNotFoundException(Long epicId) {
        super("No epic found with id " + epicId);
    }
}
