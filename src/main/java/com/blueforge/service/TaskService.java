package com.blueforge.service;

import com.blueforge.dto.TaskResponse;
import com.blueforge.dto.UpdateTaskRequest;
import com.blueforge.entity.Task;
import com.blueforge.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setTitle(request.title());
        task.setDescription(request.description());

        task = taskRepository.save(task);

        return toTaskResponse(task);
    }

    private static TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getUserStory().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getEffortEstimate(),
                task.getOrderIndex());
    }
}
