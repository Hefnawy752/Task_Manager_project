package com.example.demo.dto;

import com.example.demo.Model.EStatus;
import com.example.demo.Model.EPriority;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    private EStatus status = EStatus.TODO;
    private EPriority priority = EPriority.MEDIUM;
}