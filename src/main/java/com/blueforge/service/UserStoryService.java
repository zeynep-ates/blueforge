package com.blueforge.service;

import com.blueforge.dto.UpdateUserStoryRequest;
import com.blueforge.dto.UserStoryResponse;
import com.blueforge.entity.UserStory;
import com.blueforge.repository.UserStoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;

    public UserStoryService(UserStoryRepository userStoryRepository) {
        this.userStoryRepository = userStoryRepository;
    }

    @Transactional
    public UserStoryResponse updateUserStory(Long userStoryId, UpdateUserStoryRequest request) {
        UserStory userStory = userStoryRepository
                .findById(userStoryId)
                .orElseThrow(() -> new UserStoryNotFoundException(userStoryId));

        userStory.setTitle(request.title());
        userStory.setDescription(request.description());
        userStory.setAcceptanceCriteria(request.acceptanceCriteria());

        userStory = userStoryRepository.save(userStory);

        return toUserStoryResponse(userStory);
    }

    private static UserStoryResponse toUserStoryResponse(UserStory userStory) {
        return new UserStoryResponse(
                userStory.getId(),
                userStory.getEpic().getId(),
                userStory.getTitle(),
                userStory.getDescription(),
                userStory.getAcceptanceCriteria(),
                userStory.getOrderIndex());
    }
}
