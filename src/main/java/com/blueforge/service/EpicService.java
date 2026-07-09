package com.blueforge.service;

import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.UpdateEpicRequest;
import com.blueforge.entity.Epic;
import com.blueforge.repository.EpicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EpicService {

    private final EpicRepository epicRepository;

    public EpicService(EpicRepository epicRepository) {
        this.epicRepository = epicRepository;
    }

    @Transactional
    public EpicResponse updateEpic(Long epicId, UpdateEpicRequest request) {
        Epic epic = epicRepository.findById(epicId).orElseThrow(() -> new EpicNotFoundException(epicId));

        epic.setTitle(request.title());
        epic.setDescription(request.description());

        epic = epicRepository.save(epic);

        return toEpicResponse(epic);
    }

    private static EpicResponse toEpicResponse(Epic epic) {
        return new EpicResponse(epic.getId(), epic.getTitle(), epic.getDescription(), epic.getOrderIndex());
    }
}
