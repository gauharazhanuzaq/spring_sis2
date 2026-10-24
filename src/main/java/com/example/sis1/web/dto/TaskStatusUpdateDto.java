package com.example.sis1.web.dto;

import com.example.sis1.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateDto {

    @NotNull(message = "Status must not be null")
    private TaskStatus status;
}
