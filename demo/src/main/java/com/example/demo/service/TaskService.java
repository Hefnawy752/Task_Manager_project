package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.Model.*;
import com.example.demo.repository.*;
import com.example.demo.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

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
        return taskMapper.toResponse(taskRepository.save(task));
    }


    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task;
        if (isAdmin()) {
            task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
        } else {
            task = taskRepository.findByIdAndUser(id, getCurrentUser())
                    .orElseThrow(() -> new RuntimeException("Task not found"));
        }
        taskMapper.updateEntity(request, task);
        return taskMapper.toResponse(taskRepository.save(task));
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