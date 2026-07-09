package com.blueforge.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blueforge.dto.TaskResponse;
import com.blueforge.dto.UpdateTaskRequest;
import com.blueforge.entity.TaskEffort;
import com.blueforge.entity.TaskPriority;
import com.blueforge.service.TaskNotFoundException;
import com.blueforge.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void updateTaskReturnsOkWithBody() throws Exception {
        when(taskService.updateTask(eq(500L), any()))
                .thenReturn(new TaskResponse(
                        500L, 400L, "Updated title", "Updated description", TaskPriority.HIGH, TaskEffort.M, 0));

        mockMvc.perform(patch("/api/tasks/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTaskRequest("Updated title", "Updated description"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.title", is("Updated title")))
                .andExpect(jsonPath("$.description", is("Updated description")));
    }

    @Test
    void updateTaskReturnsBadRequestWhenTitleBlank() throws Exception {
        mockMvc.perform(patch("/api/tasks/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTaskRequest("", "description"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTaskReturnsNotFoundWhenMissing() throws Exception {
        when(taskService.updateTask(eq(500L), any())).thenThrow(new TaskNotFoundException(500L));

        mockMvc.perform(patch("/api/tasks/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTaskRequest("title", "description"))))
                .andExpect(status().isNotFound());
    }
}
