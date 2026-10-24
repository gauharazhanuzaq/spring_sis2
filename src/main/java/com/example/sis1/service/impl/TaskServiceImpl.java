package com.example.sis1.service.impl;

import com.example.sis1.domain.exception.ResourceConflictException;
import com.example.sis1.domain.exception.ResourceNotFoundException;
import com.example.sis1.domain.model.Task;
import com.example.sis1.domain.model.TaskPriority;
import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.persistence.entity.TaskEntity;
import com.example.sis1.persistence.mapper.TaskEntityMapper;
import com.example.sis1.persistence.repository.TaskRepository;
import com.example.sis1.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskEntityMapper entityMapper;

    @Override
    public Task create(Task task) {
        if (taskRepository.existsByTitleIgnoreCase(task.getTitle())) {
            throw titleAlreadyExists(task.getTitle());
        }
        Task toSave = task.toBuilder()
                .status(task.getStatus() != null ? task.getStatus() : TaskStatus.NEW)
                .priority(task.getPriority() != null ? task.getPriority() : TaskPriority.MEDIUM)
                .build();

        TaskEntity saved = taskRepository.save(entityMapper.toEntity(toSave));
        return entityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Task getById(Long id) {
        return entityMapper.toDomain(findEntityOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Task> getAll(TaskStatus status, Pageable pageable) {
        Page<TaskEntity> page = (status != null)
                ? taskRepository.findByStatus(status, pageable)
                : taskRepository.findAll(pageable);
        return page.map(entityMapper::toDomain);
    }

    @Override
    public Task update(Long id, Task incoming) {
        TaskEntity entity = findEntityOrThrow(id);
        if (taskRepository.existsByTitleIgnoreCaseAndIdNot(incoming.getTitle(), id)) {
            throw titleAlreadyExists(incoming.getTitle());
        }

        Task merged = entityMapper.toDomain(entity).toBuilder()
                .title(incoming.getTitle())
                .description(incoming.getDescription())
                .status(incoming.getStatus() != null ? incoming.getStatus() : entity.getStatus())
                .priority(incoming.getPriority() != null ? incoming.getPriority() : entity.getPriority())
                .dueDate(incoming.getDueDate())
                .build();

        entityMapper.updateEntity(merged, entity);
        return entityMapper.toDomain(taskRepository.save(entity));
    }

    @Override
    public Task updateStatus(Long id, TaskStatus status) {
        TaskEntity entity = findEntityOrThrow(id);
        entity.setStatus(status);
        return entityMapper.toDomain(taskRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        taskRepository.delete(findEntityOrThrow(id));
    }

    private TaskEntity findEntityOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    private ResourceConflictException titleAlreadyExists(String title) {
        return new ResourceConflictException("A task with title '" + title + "' already exists");
    }
}
