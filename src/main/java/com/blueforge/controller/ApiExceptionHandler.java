package com.blueforge.controller;

import com.blueforge.ai.AiClientException;
import com.blueforge.service.AiResponseParsingException;
import com.blueforge.service.InvalidAnswersException;
import com.blueforge.service.InvalidProjectVersionStatusException;
import com.blueforge.service.EpicNotFoundException;
import com.blueforge.service.ProjectNotFoundException;
import com.blueforge.service.ProjectVersionNotFoundException;
import com.blueforge.service.RequirementNotFoundException;
import com.blueforge.service.TaskNotFoundException;
import com.blueforge.service.UserStoryNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({AiClientException.class, AiResponseParsingException.class})
    public ResponseEntity<ErrorResponse> handleAiFailure(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("The AI service failed to produce a usable response. Please try again."));
    }

    @ExceptionHandler({
        ProjectNotFoundException.class,
        ProjectVersionNotFoundException.class,
        RequirementNotFoundException.class,
        EpicNotFoundException.class,
        UserStoryNotFoundException.class,
        TaskNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    public record ErrorResponse(String message) {}
}
