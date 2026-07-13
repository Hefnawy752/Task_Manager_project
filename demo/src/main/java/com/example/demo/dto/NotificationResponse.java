package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String message;
    private Long taskId;
    private String taskTitle;
    private String updatedBy;
    private LocalDateTime timestamp;
}