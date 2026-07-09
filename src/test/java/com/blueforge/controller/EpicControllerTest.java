package com.blueforge.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blueforge.dto.EpicResponse;
import com.blueforge.dto.UpdateEpicRequest;
import com.blueforge.service.EpicNotFoundException;
import com.blueforge.service.EpicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EpicController.class)
class EpicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EpicService epicService;

    @Test
    void updateEpicReturnsOkWithBody() throws Exception {
        when(epicService.updateEpic(eq(300L), any()))
                .thenReturn(new EpicResponse(300L, "Updated title", "Updated description", 0));

        mockMvc.perform(patch("/api/epics/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateEpicRequest("Updated title", "Updated description"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(300)))
                .andExpect(jsonPath("$.title", is("Updated title")))
                .andExpect(jsonPath("$.description", is("Updated description")));
    }

    @Test
    void updateEpicReturnsBadRequestWhenTitleBlank() throws Exception {
        mockMvc.perform(patch("/api/epics/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateEpicRequest("", "description"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEpicReturnsNotFoundWhenMissing() throws Exception {
        when(epicService.updateEpic(eq(300L), any())).thenThrow(new EpicNotFoundException(300L));

        mockMvc.perform(patch("/api/epics/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateEpicRequest("title", "description"))))
                .andExpect(status().isNotFound());
    }
}
