package com.example.sis1.web.controller;

import com.example.sis1.domain.model.Task;
import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.service.TaskService;
import com.example.sis1.web.dto.TaskRequestDto;
import com.example.sis1.web.dto.TaskResponseDto;
import com.example.sis1.web.dto.TaskStatusUpdateDto;
import com.example.sis1.web.mapper.TaskDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Web layer: translates HTTP <-> domain. No business logic here —
 * DTO in, domain object to the service, domain object back, DTO out.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskDtoMapper dtoMapper;

    @PostMapping
    public ResponseEntity<TaskResponseDto> create(@Valid @RequestBody TaskRequestDto requestDto) {
        Task created = taskService.create(dtoMapper.toDomain(requestDto));
        TaskResponseDto body = dtoMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/tasks/" + body.getId())).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(dtoMapper.toResponse(taskService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponseDto>> getAll(
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<TaskResponseDto> page = taskService.getAll(status, pageable).map(dtoMapper::toResponse);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> update(@PathVariable Long id,
                                                   @Valid @RequestBody TaskRequestDto requestDto) {
        Task updated = taskService.update(id, dtoMapper.toDomain(requestDto));
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponseDto> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody TaskStatusUpdateDto statusDto) {
        Task updated = taskService.updateStatus(id, statusDto.getStatus());
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
