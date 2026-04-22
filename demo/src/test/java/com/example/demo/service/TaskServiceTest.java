package com.example.demo.service;

import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.mapper.TaskMapper;
import com.example.demo.Model.*;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private UserDetailsImpl userDetails;
    private Task testTask;
    private TaskRequest taskRequest;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRoles(Collections.singleton(ERole.ROLE_USER));

        userDetails = UserDetailsImpl.build(testUser);

        taskRequest = new TaskRequest();
        taskRequest.setTitle("Test Task");
        taskRequest.setDescription("Test Description");
        taskRequest.setStatus(EStatus.TODO);
        taskRequest.setPriority(EPriority.MEDIUM);

        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setStatus(EStatus.TODO);
        testTask.setPriority(EPriority.MEDIUM);
        testTask.setUser(testUser);

        taskResponse = new TaskResponse();
        taskResponse.setId(1L);
        taskResponse.setTitle("Test Task");
        taskResponse.setDescription("Test Description");
        taskResponse.setStatus(EStatus.TODO);
        taskResponse.setPriority(EPriority.MEDIUM);
        taskResponse.setUsername("testuser");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(userDetails);
    }



    @Test
    void createTask_ShouldReturnTaskResponse_WhenValidRequest() {

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskMapper.toEntity(taskRequest)).thenReturn(testTask);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);


        TaskResponse result = taskService.createTask(taskRequest);


        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(EStatus.TODO, result.getStatus());
        assertEquals(EPriority.MEDIUM, result.getPriority());
        assertEquals("testuser", result.getUsername());
        verify(taskRepository, times(1)).save(testTask);
    }


    @Test
    void getAllTasks_ShouldReturnUserTasks_WhenUserRole() {

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByUser(testUser))
                .thenReturn(List.of(testTask));
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);


        List<TaskResponse> result = taskService.getAllTasks();


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
        verify(taskRepository, times(1)).findByUser(testUser);
    }

    @Test
    void getAllTasks_ShouldReturnAllTasks_WhenAdminRole() {
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("adminuser");
        adminUser.setPassword("password");
        adminUser.setRoles(Collections.singleton(ERole.ROLE_ADMIN));

        UserDetailsImpl adminDetails = UserDetailsImpl.build(adminUser);
        when(authentication.getPrincipal()).thenReturn(adminDetails);

        when(taskRepository.findAll()).thenReturn(List.of(testTask));
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);

        List<TaskResponse> result = taskService.getAllTasks();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).findAll();
    }


    @Test
    void getTaskById_ShouldReturnTask_WhenTaskExists() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testTask));
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);

        // Act
        TaskResponse result = taskService.getTaskById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Task", result.getTitle());
    }

    @Test
    void getTaskById_ShouldThrowException_WhenTaskNotFound() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByIdAndUser(99L, testUser))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskById(99L));
        assertEquals("Task not found", exception.getMessage());
    }



    @Test
    void updateTask_ShouldReturnUpdatedTask_WhenTaskExists() {
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated Task");
        updateRequest.setDescription("Updated Description");
        updateRequest.setStatus(EStatus.IN_PROGRESS);
        updateRequest.setPriority(EPriority.HIGH);

        TaskResponse updatedResponse = new TaskResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle("Updated Task");
        updatedResponse.setStatus(EStatus.IN_PROGRESS);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testTask));
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(taskMapper.toResponse(testTask)).thenReturn(updatedResponse);


        TaskResponse result = taskService.updateTask(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Task", result.getTitle());
        assertEquals(EStatus.IN_PROGRESS, result.getStatus());
        verify(taskRepository, times(1)).save(testTask);
    }

    @Test
    void updateTask_ShouldThrowException_WhenTaskNotFound() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByIdAndUser(99L, testUser))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateTask(99L, taskRequest));
        assertEquals("Task not found", exception.getMessage());
    }



    @Test
    void deleteTask_ShouldDeleteTask_WhenTaskExists() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testTask));

        taskService.deleteTask(1L);

        verify(taskRepository, times(1)).delete(testTask);
    }

    @Test
    void deleteTask_ShouldThrowException_WhenTaskNotFound() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByIdAndUser(99L, testUser))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.deleteTask(99L));
        assertEquals("Task not found", exception.getMessage());
    }



    @Test
    void getTasksByStatus_ShouldReturnFilteredTasks_WhenUserRole() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByUserAndStatus(testUser, EStatus.TODO))
                .thenReturn(List.of(testTask));
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);

        List<TaskResponse> result = taskService.getTasksByStatus(EStatus.TODO);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(EStatus.TODO, result.get(0).getStatus());
        verify(taskRepository, times(1))
                .findByUserAndStatus(testUser, EStatus.TODO);
    }


    @Test
    void searchTasksByTitle_ShouldReturnMatchingTasks_WhenUserRole() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByUserAndTitleContainingIgnoreCase(
                testUser, "Test"))
                .thenReturn(List.of(testTask));
        when(taskMapper.toResponse(testTask)).thenReturn(taskResponse);

        List<TaskResponse> result = taskService.searchTasksByTitle("Test");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
        verify(taskRepository, times(1))
                .findByUserAndTitleContainingIgnoreCase(testUser, "Test");
    }

    @Test
    void searchTasksByTitle_ShouldReturnEmpty_WhenNoMatchFound() {

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByUserAndTitleContainingIgnoreCase(
                testUser, "xyz"))
                .thenReturn(Collections.emptyList());


        List<TaskResponse> result = taskService.searchTasksByTitle("xyz");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}