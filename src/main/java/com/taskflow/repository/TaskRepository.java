package com.taskflow.repository;

import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedToOrderByPositionAsc(User user);

    List<Task> findByStatusOrderByPositionAsc(String status);

    List<Task> findByProjectIdOrderByPositionAsc(Long projectId);

    @Query("SELECT t FROM Task t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:projectId IS NULL OR t.project.id = :projectId) AND " +
           "(:assigneeId IS NULL OR t.assignedTo.id = :assigneeId) AND " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.position ASC, t.createdAt DESC")
    List<Task> findWithFilters(@Param("status") String status,
                               @Param("priority") String priority,
                               @Param("projectId") Long projectId,
                               @Param("assigneeId") Long assigneeId,
                               @Param("search") String search);

    @Query("SELECT t FROM Task t WHERE t.dueDate <= :date AND t.status != 'DONE'")
    List<Task> findOverdueTasks(@Param("date") LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.dueDate BETWEEN :today AND :soon AND t.status != 'DONE'")
    List<Task> findDueSoonTasks(@Param("today") LocalDate today, @Param("soon") LocalDate soon);

    long countByStatus(String status);

    long countByAssignedTo(User user);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate < CURRENT_DATE AND t.status != 'DONE'")
    long countOverdue();

    @Query("SELECT t FROM Task t ORDER BY t.createdAt DESC")
    List<Task> findRecentTasks(org.springframework.data.domain.Pageable pageable);
}
