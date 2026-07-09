package com.blueforge.controller;

import com.blueforge.ai.AiClientException;
import com.blueforge.service.AiResponseParsingException;
import com.blueforge.service.InvalidAnswersException;
import com.blueforge.service.InvalidProjectVersionStatusException;
import com.blueforge.service.ProjectVersionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({AiClientException.class, AiResponseParsingException.class})
    public ResponseEntity<ErrorResponse> handleAiFailure(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("The AI service failed to produce a usable response. Please try again."));
    }

    @ExceptionHandler(ProjectVersionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProjectVersionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidAnswersException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAnswers(InvalidAnswersException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidProjectVersionStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatus(InvalidProjectVersionStatusException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    public record ErrorResponse(String message) {}
}
