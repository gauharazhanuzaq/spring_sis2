package com.example.sis1.persistence.mapper;

import com.example.sis1.domain.model.Task;
import com.example.sis1.domain.model.TaskPriority;
import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskEntityMapperTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 1, 12, 0);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 10, 2, 9, 30);

    private final TaskEntityMapper mapper = new TaskEntityMapper();

    @Test
    void toDomain_copiesEveryField() {
        TaskEntity entity = TaskEntity.builder()
                .id(5L)
                .title("Title")
                .description("Description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 12, 1))
                .createdAt(CREATED)
                .updatedAt(UPDATED)
                .build();

        Task task = mapper.toDomain(entity);

        assertThat(task.getId()).isEqualTo(5L);
        assertThat(task.getTitle()).isEqualTo("Title");
        assertThat(task.getDescription()).isEqualTo("Description");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(task.getCreatedAt()).isEqualTo(CREATED);
        assertThat(task.getUpdatedAt()).isEqualTo(UPDATED);
    }

    @Test
    void toEntity_copiesEveryField() {
        Task task = Task.builder()
                .id(7L)
                .title("Title")
                .description("Description")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.LOW)
                .dueDate(LocalDate.of(2026, 11, 11))
                .createdAt(CREATED)
                .updatedAt(UPDATED)
                .build();

        TaskEntity entity = mapper.toEntity(task);

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getDescription()).isEqualTo("Description");
        assertThat(entity.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(entity.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(entity.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED);
    }

    @Test
    void updateEntity_overwritesEditableFieldsButNotIdOrAuditFields() {
        TaskEntity entity = TaskEntity.builder()
                .id(1L)
                .title("Old")
                .description("Old description")
                .status(TaskStatus.NEW)
                .priority(TaskPriority.LOW)
                .createdAt(CREATED)
                .updatedAt(UPDATED)
                .build();
        Task changes = Task.builder()
                .id(999L)
                .title("New")
                .description("New description")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2027, 1, 1))
                .createdAt(LocalDateTime.of(2000, 1, 1, 0, 0))
                .build();

        mapper.updateEntity(changes, entity);

        assertThat(entity.getTitle()).isEqualTo("New");
        assertThat(entity.getDescription()).isEqualTo("New description");
        assertThat(entity.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(entity.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(entity.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        // untouched
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED);
    }
}
