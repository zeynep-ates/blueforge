package com.blueforge.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blueforge.dto.UpdateUserStoryRequest;
import com.blueforge.dto.UserStoryResponse;
import com.blueforge.service.UserStoryNotFoundException;
import com.blueforge.service.UserStoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserStoryController.class)
class UserStoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserStoryService userStoryService;

    @Test
    void updateUserStoryReturnsOkWithBody() throws Exception {
        when(userStoryService.updateUserStory(eq(400L), any()))
                .thenReturn(new UserStoryResponse(
                        400L, 300L, "Updated title", "Updated description", "Updated criteria", 0));

        mockMvc.perform(patch("/api/user-stories/400")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStoryRequest(
                                "Updated title", "Updated description", "Updated criteria"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(400)))
                .andExpect(jsonPath("$.title", is("Updated title")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.acceptanceCriteria", is("Updated criteria")));
    }

    @Test
    void updateUserStoryReturnsBadRequestWhenAcceptanceCriteriaBlank() throws Exception {
        mockMvc.perform(patch("/api/user-stories/400")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateUserStoryRequest("title", "description", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserStoryReturnsNotFoundWhenMissing() throws Exception {
        when(userStoryService.updateUserStory(eq(400L), any())).thenThrow(new UserStoryNotFoundException(400L));

        mockMvc.perform(patch("/api/user-stories/400")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateUserStoryRequest("title", "description", "criteria"))))
                .andExpect(status().isNotFound());
    }
}
