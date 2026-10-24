package com.example.sis1.web.controller;

import com.example.sis1.domain.exception.ResourceConflictException;
import com.example.sis1.domain.exception.ResourceNotFoundException;
import com.example.sis1.domain.model.Task;
import com.example.sis1.domain.model.TaskPriority;
import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.service.TaskService;
import com.example.sis1.web.mapper.TaskDtoMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: only the controller, the @RestControllerAdvice, Jackson and Bean Validation
 * are loaded. The service is a Mockito mock, so no database and no business logic are involved.
 */
@WebMvcTest(TaskController.class)
@Import(TaskDtoMapper.class)
@ActiveProfiles("test")
class TaskControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    // ------------------------------------------------------------------ POST

    @Test
    void create_validRequest_returns201WithLocationHeaderAndBody() throws Exception {
        given(taskService.create(any(Task.class)))
                .willReturn(task(1L, "Prepare report", TaskStatus.NEW, TaskPriority.HIGH));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Prepare report", "description": "weeks 5-8", "priority": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Prepare report"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        // the DTO was translated into a domain object before reaching the service
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskService).create(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Prepare report");
        assertThat(captor.getValue().getDescription()).isEqualTo("weeks 5-8");
        assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void create_invalidFields_returns400WithFieldErrorsInUnifiedFormat() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "a", "dueDate": "2020-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/tasks"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("title", "dueDate")));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Broken JSON\", \"priority\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(taskService);
    }

    @Test
    void create_unknownEnumValue_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Valid title", "priority": "URGENT"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void create_duplicateTitle_returns409() throws Exception {
        given(taskService.create(any(Task.class)))
                .willThrow(new ResourceConflictException("A task with title 'Prepare report' already exists"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Prepare report"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A task with title 'Prepare report' already exists"))
                .andExpect(jsonPath("$.path").value("/api/tasks"));
    }

    // ------------------------------------------------------------------- GET

    @Test
    void getById_existingTask_returns200() throws Exception {
        given(taskService.getById(1L)).willReturn(task(1L, "Prepare report", TaskStatus.NEW, TaskPriority.MEDIUM));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Prepare report"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getById_missingTask_returns404InUnifiedFormat() throws Exception {
        given(taskService.getById(99L)).willThrow(new ResourceNotFoundException("Task with id 99 not found"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task with id 99 not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/99"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void getById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("should be of type Long")));

        verifyNoInteractions(taskService);
    }

    @Test
    void getAll_returnsPageOfTasks() throws Exception {
        List<Task> tasks = List.of(
                task(1L, "First", TaskStatus.NEW, TaskPriority.LOW),
                task(2L, "Second", TaskStatus.DONE, TaskPriority.HIGH));
        given(taskService.getAll(isNull(), any(Pageable.class))).willReturn(new PageImpl<>(tasks));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("First"))
                .andExpect(jsonPath("$.content[1].status").value("DONE"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAll_passesStatusFilterAndPagingParametersToService() throws Exception {
        given(taskService.getAll(eq(TaskStatus.DONE), any(Pageable.class))).willReturn(new PageImpl<Task>(List.of()));

        mockMvc.perform(get("/api/tasks")
                        .param("status", "DONE")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).getAll(eq(TaskStatus.DONE), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    // ------------------------------------------------------------------- PUT

    @Test
    void update_validRequest_returns200() throws Exception {
        given(taskService.update(eq(1L), any(Task.class)))
                .willReturn(task(1L, "Updated title", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Updated title", "status": "IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void update_invalidBody_returns400() throws Exception {
        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "ab"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        verifyNoInteractions(taskService);
    }

    @Test
    void update_missingTask_returns404() throws Exception {
        given(taskService.update(eq(99L), any(Task.class)))
                .willThrow(new ResourceNotFoundException("Task with id 99 not found"));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Ghost task"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/tasks/99"));
    }

    @Test
    void update_duplicateTitle_returns409() throws Exception {
        given(taskService.update(eq(2L), any(Task.class)))
                .willThrow(new ResourceConflictException("A task with title 'First' already exists"));

        mockMvc.perform(put("/api/tasks/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "First"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ----------------------------------------------------------------- PATCH

    @Test
    void updateStatus_validRequest_returns200() throws Exception {
        given(taskService.updateStatus(1L, TaskStatus.DONE))
                .willReturn(task(1L, "Prepare report", TaskStatus.DONE, TaskPriority.MEDIUM));

        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void updateStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verifyNoInteractions(taskService);
    }

    @Test
    void updateStatus_unknownStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FINISHED"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void updateStatus_missingTask_returns404() throws Exception {
        given(taskService.updateStatus(99L, TaskStatus.DONE))
                .willThrow(new ResourceNotFoundException("Task with id 99 not found"));

        mockMvc.perform(patch("/api/tasks/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DONE"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- DELETE

    @Test
    void delete_existingTask_returns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }

    @Test
    void delete_missingTask_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Task with id 99 not found"))
                .given(taskService).delete(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task with id 99 not found"));
    }

    // --------------------------------------------------------------- helpers

    private Task task(Long id, String title, TaskStatus status, TaskPriority priority) {
        return Task.builder()
                .id(id)
                .title(title)
                .status(status)
                .priority(priority)
                .dueDate(LocalDate.of(2026, 12, 31))
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
