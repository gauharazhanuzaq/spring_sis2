package com.example.sis1.persistence.mapper;

import com.example.sis1.domain.model.Task;
import com.example.sis1.persistence.entity.TaskEntity;
import org.springframework.stereotype.Component;


@Component
public class TaskEntityMapper {

    public Task toDomain(TaskEntity entity) {
        return Task.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .dueDate(entity.getDueDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TaskEntity toEntity(Task task) {
        return TaskEntity.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public void updateEntity(Task task, TaskEntity entity) {
        entity.setTitle(task.getTitle());
        entity.setDescription(task.getDescription());
        entity.setStatus(task.getStatus());
        entity.setPriority(task.getPriority());
        entity.setDueDate(task.getDueDate());
    }
}
