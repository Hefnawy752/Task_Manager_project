package com.example.demo.repository;

import com.example.demo.Model.EStatus;
import com.example.demo.Model.Task;
import com.example.demo.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // User queries
    List<Task> findByUser(User user);
    List<Task> findByUserAndStatus(User user, EStatus status);
    List<Task> findByUserAndTitleContainingIgnoreCase(User user, String title);
    Optional<Task> findByIdAndUser(Long id, User user);

    // Admin queries
    List<Task> findByStatus(EStatus status);
    List<Task> findByTitleContainingIgnoreCase(String title);
}