package com.example.demo.service;

import com.example.demo.dto.*;
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

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findByUser(getCurrentUser())
                .stream().map(TaskResponse::new)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        return new TaskResponse(task);
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setUser(getCurrentUser());
        return new TaskResponse(taskRepository.save(task));
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        return new TaskResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        Task task = taskRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        taskRepository.delete(task);
    }

    public List<TaskResponse> getTasksByStatus(Task.Status status) {
        return taskRepository.findByUserAndStatus(getCurrentUser(), status)
                .stream().map(TaskResponse::new)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> searchTasksByTitle(String keyword) {
        return taskRepository.findByUserAndTitleContainingIgnoreCase(
                        getCurrentUser(), keyword)
                .stream().map(TaskResponse::new)
                .collect(Collectors.toList());
    }
}