package com.example.sis1.persistence.repository;

import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.persistence.entity.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Page<TaskEntity> findByStatus(TaskStatus status, Pageable pageable);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);
}
