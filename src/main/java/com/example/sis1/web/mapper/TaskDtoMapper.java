package com.example.sis1.web.mapper;

import com.example.sis1.domain.model.Task;
import com.example.sis1.web.dto.TaskRequestDto;
import com.example.sis1.web.dto.TaskResponseDto;
import org.springframework.stereotype.Component;


@Component
public class TaskDtoMapper {

    public Task toDomain(TaskRequestDto dto) {
        return Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .priority(dto.getPriority())
                .dueDate(dto.getDueDate())
                .build();
    }

    public TaskResponseDto toResponse(Task task) {
        return TaskResponseDto.builder()
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
}
