package com.blueforge.controller;

import com.blueforge.ai.AiClientException;
import com.blueforge.service.AiResponseParsingException;
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

    public record ErrorResponse(String message) {}
}
