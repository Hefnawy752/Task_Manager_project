package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.Model.*;
import com.example.demo.repository.*;
import com.example.demo.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final SimpMessagingTemplate messagingTemplate;

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

    // ---------- helper to convert Task to enriched TaskResponse ----------
    private TaskResponse toResponseWithIds(Task task) {
        TaskResponse response = taskMapper.toResponse(task);
        if (task.getUser() != null) {
            response.setUserId(task.getUser().getId());
        }
        if (task.getCreatedBy() != null) {
            response.setCreatedById(task.getCreatedBy().getId());
        } else {
            // fallback (should not happen)
            response.setCreatedById(task.getUser() != null ? task.getUser().getId() : null);
        }
        return response;
    }

    // ----------------------------------------------------------------------

    public List<TaskResponse> getAllTasks() {
        if (isAdmin()) {
            return taskRepository.findAll()
                    .stream().map(this::toResponseWithIds)
                    .collect(Collectors.toList());
        }
        return taskRepository.findByUser(getCurrentUser())
                .stream().map(this::toResponseWithIds)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        Task task;
        if (isAdmin()) {
            task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
        } else {
            task = taskRepository.findByIdAndUser(id, getCurrentUser())
                    .orElseThrow(() -> new RuntimeException("Task not found"));
        }
        return toResponseWithIds(task);
    }

    public TaskResponse createTask(TaskRequest request) {
        User currentUser = getCurrentUser();
        User assignedUser = currentUser;

        if (isAdmin() && request.getUserId() != null) {
            assignedUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        Task task = taskMapper.toEntity(request);
        task.setUser(assignedUser);
        task.setCreatedBy(currentUser);

        Task savedTask = taskRepository.save(task);
        TaskResponse response = toResponseWithIds(savedTask);

        messagingTemplate.convertAndSend("/topic/tasks", response);
        log.info("Task created: {}", savedTask.getId());
        return response;
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User currentUser = getCurrentUser();
        boolean isAdmin = isAdmin();
        boolean isOwner = existingTask.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("You are not authorized to update this task");
        }

        // Restrict fields for non‑creator users
        if (!isAdmin) {
            boolean createdBySelf = existingTask.getCreatedBy().getId().equals(currentUser.getId());
            if (!createdBySelf) {
                // only status allowed
                if (request.getStatus() != null) {
                    existingTask.setStatus(request.getStatus());
                } else {
                    throw new RuntimeException("You can only update the status of this task");
                }
                // do NOT update other fields
            } else {
                taskMapper.updateEntity(request, existingTask);
            }
        } else {
            // Admin – full update
            taskMapper.updateEntity(request, existingTask);
        }

        Task updatedTask = taskRepository.save(existingTask);
        TaskResponse response = toResponseWithIds(updatedTask);

        messagingTemplate.convertAndSend("/topic/tasks", response);
        log.info("Task updated: {}", updatedTask.getId());

        // Notification to owner if different from updater
        User owner = existingTask.getUser();
        if (!owner.getId().equals(currentUser.getId())) {
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
            messagingTemplate.convertAndSendToUser(
                    owner.getUsername(),
                    "/queue/notifications",
                    notification
            );
        }

        return response;
    }

    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User currentUser = getCurrentUser();
        boolean isAdmin = isAdmin();

        if (isAdmin) {
            taskRepository.delete(task);
            messagingTemplate.convertAndSend("/topic/tasks/delete", task.getId());
            log.info("Task deleted by admin: {}", id);
            return;
        }

        boolean isOwner = task.getUser().getId().equals(currentUser.getId());
        boolean createdBySelf = task.getCreatedBy().getId().equals(currentUser.getId());

        if (isOwner && createdBySelf) {
            taskRepository.delete(task);
            messagingTemplate.convertAndSend("/topic/tasks/delete", task.getId());
            log.info("Task deleted by owner: {}", id);
        } else {
            throw new RuntimeException("You cannot delete this task");
        }
    }

    public List<TaskResponse> getTasksByStatus(EStatus status) {
        if (isAdmin()) {
            return taskRepository.findByStatus(status)
                    .stream().map(this::toResponseWithIds)
                    .collect(Collectors.toList());
        }
        return taskRepository.findByUserAndStatus(getCurrentUser(), status)
                .stream().map(this::toResponseWithIds)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> searchTasksByTitle(String keyword) {
        if (isAdmin()) {
            return taskRepository.findByTitleContainingIgnoreCase(keyword)
                    .stream().map(this::toResponseWithIds)
                    .collect(Collectors.toList());
        }
        return taskRepository.findByUserAndTitleContainingIgnoreCase(
                        getCurrentUser(), keyword)
                .stream().map(this::toResponseWithIds)
                .collect(Collectors.toList());
    }
}