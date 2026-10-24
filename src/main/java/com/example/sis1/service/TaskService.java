package com.example.sis1.service;

import com.example.sis1.domain.model.Task;
import com.example.sis1.domain.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Business operations on tasks. Speaks the language of the domain (Task, TaskStatus) —
 * it knows nothing about DTOs, JSON, HTTP or JPA entities.
 */
public interface TaskService {

    Task create(Task task);

    Task getById(Long id);

    Page<Task> getAll(TaskStatus status, Pageable pageable);

    Task update(Long id, Task task);

    Task updateStatus(Long id, TaskStatus status);

    void delete(Long id);
}
