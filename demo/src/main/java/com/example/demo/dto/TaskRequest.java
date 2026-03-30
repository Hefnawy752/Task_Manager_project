package com.example.demo.dto;

import com.example.demo.Model.Task;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    private Task.Status status = Task.Status.TODO;

    private Task.Priority priority = Task.Priority.MEDIUM;
}