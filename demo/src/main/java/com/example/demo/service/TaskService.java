package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.Model.*;
import com.example.demo.repository.*;
import com.example.demo.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final SimpMessagingTemplate messagingTemplate;  // ← add this

    private UserDetailsImpl getCurrentUserDetails() {
        return (UserDetailsImpl) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
    }

    private User getCurrentUser() {
        return userRepository.findByUsername(getCurrentUserDetails().getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isAdmin() {
        return getCurrentUserDetails().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public List<TaskResponse> getAllTasks() {
        if (isAdmin()) {
            return taskRepository.findAll()
                    .stream().map(taskMapper::toResponse)
                    .collect(Collectors.toList());
        }
        return taskRepository.findByUser(getCurrentUser())
                .stream().map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        if (isAdmin()) {
            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            return taskMapper.toResponse(task);
        }
        Task task = taskRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        return taskMapper.toResponse(task);
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        task.setUser(getCurrentUser());
        TaskResponse response = taskMapper.toResponse(taskRepository.save(task));

        // Phase 1 — broadcast to all subscribers
        messagingTemplate.convertAndSend("/topic/tasks", response);

        return response;
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        // Fetch existing task (without authentication check yet)
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Check permissions: user can edit if they own it or are admin
        User currentUser = getCurrentUser();
        boolean isOwner = existingTask.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = isAdmin();

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You are not authorized to update this task");
        }

        // Update fields
        taskMapper.updateEntity(request, existingTask);
        Task updatedTask = taskRepository.save(existingTask);
        TaskResponse response = taskMapper.toResponse(updatedTask);

        // Phase 1 – broadcast to all subscribers
        messagingTemplate.convertAndSend("/topic/tasks", response);

        // Phase 2 – send notification to owner if someone else updated it
        User owner = existingTask.getUser();
        if (!owner.getId().equals(currentUser.getId())) {
            // Build notification message
            String notificationMessage = String.format(
                    "Your task '%s' was updated by %s",
                    updatedTask.getTitle(),
                    currentUser.getUsername()
            );
            NotificationResponse notification = new NotificationResponse(
                    notificationMessage,
                    updatedTask.getId(),
                    updatedTask.getTitle(),
                    currentUser.getUsername(),
                    LocalDateTime.now()
            );

            // Send to the owner's personal queue
            messagingTemplate.convertAndSendToUser(
                    owner.getUsername(),
                    "/queue/notifications",
                    notification
            );
        }

        return response;
    }

    public void deleteTask(Long id) {
        Task task;
        if (isAdmin()) {
            task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
        } else {
            task = taskRepository.findByIdAndUser(id, getCurrentUser())
                    .orElseThrow(() -> new RuntimeException("Task not found"));
        }
        taskRepository.delete(task);

        // Phase 1 — broadcast deletion id to all subscribers
        messagingTemplate.convertAndSend("/topic/tasks/delete", task.getId());
    }

    public List<TaskResponse> getTasksByStatus(EStatus status) {
        if (isAdmin()) {
            return taskRepository.findByStatus(status)
                    .stream().map(taskMapper::toResponse)
                    .collect(Collectors.toList());
        }
        return taskRepository.findByUserAndStatus(getCurrentUser(), status)
                .stream().map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> searchTasksByTitle(String keyword) {
        if (isAdmin()) {
            return taskRepository.findByTitleContainingIgnoreCase(keyword)
                    .stream().map(taskMapper::toResponse)
                    .collect(Collectors.toList());
        }
        return taskRepository.findByUserAndTitleContainingIgnoreCase(
                        getCurrentUser(), keyword)
                .stream().map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }
}