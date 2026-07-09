package com.blueforge.controller;

import com.blueforge.dto.UpdateUserStoryRequest;
import com.blueforge.dto.UserStoryResponse;
import com.blueforge.service.UserStoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-stories")
public class UserStoryController {

    private final UserStoryService userStoryService;

    public UserStoryController(UserStoryService userStoryService) {
        this.userStoryService = userStoryService;
    }

    @PatchMapping("/{userStoryId}")
    public ResponseEntity<UserStoryResponse> updateUserStory(
            @PathVariable Long userStoryId, @Valid @RequestBody UpdateUserStoryRequest request) {
        return ResponseEntity.ok(userStoryService.updateUserStory(userStoryId, request));
    }
}
