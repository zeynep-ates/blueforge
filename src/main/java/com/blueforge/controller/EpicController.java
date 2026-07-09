package com.blueforge.controller;

import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.UpdateEpicRequest;
import com.blueforge.service.EpicService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/epics")
public class EpicController {

    private final EpicService epicService;

    public EpicController(EpicService epicService) {
        this.epicService = epicService;
    }

    @PatchMapping("/{epicId}")
    public ResponseEntity<EpicResponse> updateEpic(
            @PathVariable Long epicId, @Valid @RequestBody UpdateEpicRequest request) {
        return ResponseEntity.ok(epicService.updateEpic(epicId, request));
    }
}
