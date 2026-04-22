package com.example.demo.dto;

import com.example.demo.Model.EPriority;
import com.example.demo.Model.EStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private EStatus status;
    private EPriority priority;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}