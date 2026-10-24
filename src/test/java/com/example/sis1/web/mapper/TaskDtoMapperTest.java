package com.example.sis1.web.mapper;

import com.example.sis1.domain.model.Task;
import com.example.sis1.domain.model.TaskPriority;
import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.web.dto.TaskRequestDto;
import com.example.sis1.web.dto.TaskResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskDtoMapperTest {

    private final TaskDtoMapper mapper = new TaskDtoMapper();

    @Test
    void toDomain_copiesRequestFieldsAndLeavesServerManagedFieldsEmpty() {
        TaskRequestDto dto = new TaskRequestDto(
                "Title", "Description", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.of(2026, 12, 1));

        Task task = mapper.toDomain(dto);

        assertThat(task.getTitle()).isEqualTo("Title");
        assertThat(task.getDescription()).isEqualTo("Description");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(task.getId()).isNull();
        assertThat(task.getCreatedAt()).isNull();
        assertThat(task.getUpdatedAt()).isNull();
    }

    @Test
    void toDomain_keepsOptionalFieldsNullSoTheServiceCanApplyDefaults() {
        Task task = mapper.toDomain(new TaskRequestDto("Only title", null, null, null, null));

        assertThat(task.getStatus()).isNull();
        assertThat(task.getPriority()).isNull();
    }

    @Test
    void toResponse_copiesEveryField() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 1, 12, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 10, 2, 9, 30);
        Task task = Task.builder()
                .id(3L)
                .title("Title")
                .description("Description")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.LOW)
                .dueDate(LocalDate.of(2026, 11, 11))
                .createdAt(created)
                .updatedAt(updated)
                .build();

        TaskResponseDto dto = mapper.toResponse(task);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getTitle()).isEqualTo("Title");
        assertThat(dto.getDescription()).isEqualTo("Description");
        assertThat(dto.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(dto.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(dto.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(dto.getCreatedAt()).isEqualTo(created);
        assertThat(dto.getUpdatedAt()).isEqualTo(updated);
    }
}
