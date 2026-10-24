package com.example.sis1.service;

import com.example.sis1.domain.exception.ResourceConflictException;
import com.example.sis1.domain.exception.ResourceNotFoundException;
import com.example.sis1.domain.model.Task;
import com.example.sis1.domain.model.TaskPriority;
import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.persistence.entity.TaskEntity;
import com.example.sis1.persistence.mapper.TaskEntityMapper;
import com.example.sis1.persistence.repository.TaskRepository;
import com.example.sis1.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Mock
    private TaskRepository taskRepository;

    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(taskRepository, new TaskEntityMapper());
    }


    @Test
    void create_appliesDefaultsAndReturnsSavedTask() {
        Task input = Task.builder().title("Write report").build();
        given(taskRepository.existsByTitleIgnoreCase("Write report")).willReturn(false);
        given(taskRepository.save(any(TaskEntity.class))).willAnswer(invocation -> {
            TaskEntity toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });

        Task result = taskService.create(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Write report");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM);

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void create_keepsExplicitStatusAndPriority() {
        Task input = Task.builder()
                .title("Urgent fix")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .build();
        given(taskRepository.save(any(TaskEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.create(input);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void create_duplicateTitle_throwsConflictAndDoesNotSave() {
        Task input = Task.builder().title("Write report").build();
        given(taskRepository.existsByTitleIgnoreCase("Write report")).willReturn(true);

        assertThatThrownBy(() -> taskService.create(input))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");

        verify(taskRepository, never()).save(any(TaskEntity.class));
    }


    @Test
    void getById_existingTask_returnsDomainObject() {
        given(taskRepository.findById(1L)).willReturn(Optional.of(entity(1L, "Write report")));

        Task result = taskService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Write report");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED);
    }

    @Test
    void getById_missingTask_throwsNotFound() {
        given(taskRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task with id 99 not found");
    }


    @Test
    void getAll_withoutStatus_usesFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        given(taskRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(entity(1L, "A"), entity(2L, "B")), pageable, 2));

        Page<Task> result = taskService.getAll(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Task::getTitle).containsExactly("A", "B");
        verify(taskRepository, never()).findByStatus(any(TaskStatus.class), any(Pageable.class));
    }

    @Test
    void getAll_withStatus_usesFindByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        TaskEntity done = entity(3L, "Finished");
        done.setStatus(TaskStatus.DONE);
        given(taskRepository.findByStatus(TaskStatus.DONE, pageable))
                .willReturn(new PageImpl<>(List.of(done), pageable, 1));

        Page<Task> result = taskService.getAll(TaskStatus.DONE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.DONE);
        verify(taskRepository, never()).findAll(any(Pageable.class));
    }


    @Test
    void update_changesEditableFieldsAndKeepsIdAndCreatedAt() {
        TaskEntity existing = entity(1L, "Old title");
        given(taskRepository.findById(1L)).willReturn(Optional.of(existing));
        given(taskRepository.existsByTitleIgnoreCaseAndIdNot("New title", 1L)).willReturn(false);
        given(taskRepository.save(any(TaskEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        Task incoming = Task.builder()
                .title("New title")
                .description("New description")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(LocalDate.of(2026, 12, 31))
                .build();

        Task result = taskService.update(1L, incoming);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.getCreatedAt()).isEqualTo(CREATED);
    }

    @Test
    void update_withoutPriority_keepsCurrentPriority() {
        TaskEntity existing = entity(1L, "Old title");
        existing.setPriority(TaskPriority.HIGH);
        given(taskRepository.findById(1L)).willReturn(Optional.of(existing));
        given(taskRepository.save(any(TaskEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.update(1L, Task.builder().title("Another title").build());

        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.NEW);
    }

    @Test
    void update_missingTask_throwsNotFoundAndNeverChecksTitleOrSaves() {
        given(taskRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(5L, Task.builder().title("Whatever").build()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).existsByTitleIgnoreCaseAndIdNot(anyString(), anyLong());
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    @Test
    void update_titleUsedByAnotherTask_throwsConflictAndDoesNotSave() {
        given(taskRepository.findById(2L)).willReturn(Optional.of(entity(2L, "Second")));
        given(taskRepository.existsByTitleIgnoreCaseAndIdNot("First", 2L)).willReturn(true);

        assertThatThrownBy(() -> taskService.update(2L, Task.builder().title("First").build()))
                .isInstanceOf(ResourceConflictException.class);

        verify(taskRepository, never()).save(any(TaskEntity.class));
    }


    @Test
    void updateStatus_changesOnlyTheStatus() {
        given(taskRepository.findById(1L)).willReturn(Optional.of(entity(1L, "Write report")));
        given(taskRepository.save(any(TaskEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.updateStatus(1L, TaskStatus.DONE);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(result.getTitle()).isEqualTo("Write report");
    }

    @Test
    void updateStatus_missingTask_throwsNotFound() {
        given(taskRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(7L, TaskStatus.DONE))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any(TaskEntity.class));
    }


    @Test
    void delete_existingTask_deletesIt() {
        TaskEntity existing = entity(1L, "Write report");
        given(taskRepository.findById(1L)).willReturn(Optional.of(existing));

        taskService.delete(1L);

        verify(taskRepository).delete(existing);
    }

    @Test
    void delete_missingTask_throwsNotFoundAndDeletesNothing() {
        given(taskRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(9L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any(TaskEntity.class));
    }


    private TaskEntity entity(Long id, String title) {
        return TaskEntity.builder()
                .id(id)
                .title(title)
                .description("description")
                .status(TaskStatus.NEW)
                .priority(TaskPriority.MEDIUM)
                .createdAt(CREATED)
                .updatedAt(CREATED)
                .build();
    }
}
