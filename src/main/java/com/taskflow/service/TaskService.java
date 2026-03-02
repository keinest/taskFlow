package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;

    public List<Task> getAll() {
        return taskRepository.findAll();
    }

    public List<Task> getFiltered(String status, String priority, Long projectId, Long assigneeId, String search) {
        String s = (status != null && !status.isBlank()) ? status : null;
        String p = (priority != null && !priority.isBlank()) ? priority : null;
        Long proj = (projectId != null && projectId > 0) ? projectId : null;
        Long assig = (assigneeId != null && assigneeId > 0) ? assigneeId : null;
        String q = (search != null && !search.isBlank()) ? search : null;
        return taskRepository.findWithFilters(s, p, proj, assig, q);
    }

    public Task findById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Tâche introuvable : " + id));
    }

    @Transactional
    public Task create(TaskRequest req, User creator) {
        Task task = new Task();
        applyRequest(task, req);
        task.setCreatedBy(creator);

        // Position en fin de colonne
        List<Task> same = taskRepository.findByStatusOrderByPositionAsc(task.getStatus());
        task.setPosition(same.isEmpty() ? 0 : same.get(same.size() - 1).getPosition() + 1);

        task = taskRepository.save(task);

        // Notification pour l'assigné
        if (task.getAssignedTo() != null && !task.getAssignedTo().getId().equals(creator.getId())) {
            notify(task.getAssignedTo(), "📋 Nouvelle tâche assignée : \"" + task.getTitle() + "\"", "INFO", task);
        }

        return task;
    }

    @Transactional
    public Task update(Long id, TaskRequest req, User updater) {
        Task task = findById(id);
        User prevAssignee = task.getAssignedTo();
        String prevStatus = task.getStatus();

        applyRequest(task, req);
        task = taskRepository.save(task);

        // Notifier si nouvelle assignation
        if (task.getAssignedTo() != null) {
            boolean reassigned = prevAssignee == null || !prevAssignee.getId().equals(task.getAssignedTo().getId());
            if (reassigned && !task.getAssignedTo().getId().equals(updater.getId())) {
                notify(task.getAssignedTo(), "📋 Tâche re-assignée : \"" + task.getTitle() + "\"", "INFO", task);
            }
        }

        // Notifier créateur si tâche terminée
        if ("DONE".equals(task.getStatus()) && !prevStatus.equals("DONE")) {
            if (!task.getCreatedBy().getId().equals(updater.getId())) {
                notify(task.getCreatedBy(), "✅ Tâche terminée : \"" + task.getTitle() + "\"", "SUCCESS", task);
            }
        }

        return task;
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.deleteById(id);
    }

    @Transactional
    public Task updateStatus(Long id, String newStatus, User user) {
        Task task = findById(id);
        task.setStatus(newStatus.toUpperCase());
        return taskRepository.save(task);
    }

    @Transactional
    public void reorder(Long taskId, String newStatus, int newPosition) {
        Task task = findById(taskId);
        task.setStatus(newStatus.toUpperCase());
        task.setPosition(newPosition);
        taskRepository.save(task);
    }

    // ─── Statistiques Dashboard ────────────────────────────

    public Map<String, Object> getDashboardStats(User user) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTasks", taskRepository.count());
        stats.put("doneTasks", taskRepository.countByStatus("DONE"));
        stats.put("inProgressTasks", taskRepository.countByStatus("IN_PROGRESS"));
        stats.put("todoTasks", taskRepository.countByStatus("TODO"));
        stats.put("overdueTasks", taskRepository.countOverdue());
        stats.put("myTasks", taskRepository.countByAssignedTo(user));

        // Répartition par priorité
        List<Task> all = taskRepository.findAll();
        Map<String, Long> byPriority = new LinkedHashMap<>();
        byPriority.put("CRITICAL", all.stream().filter(t -> "CRITICAL".equals(t.getPriority())).count());
        byPriority.put("HIGH",     all.stream().filter(t -> "HIGH".equals(t.getPriority())).count());
        byPriority.put("MEDIUM",   all.stream().filter(t -> "MEDIUM".equals(t.getPriority())).count());
        byPriority.put("LOW",      all.stream().filter(t -> "LOW".equals(t.getPriority())).count());
        stats.put("byPriority", byPriority);

        // Répartition par statut pour donut
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("TODO",        (long) stats.get("todoTasks"));
        byStatus.put("IN_PROGRESS", (long) stats.get("inProgressTasks"));
        byStatus.put("DONE",        (long) stats.get("doneTasks"));
        stats.put("byStatus", byStatus);

        // Tâches récentes
        stats.put("recentTasks", taskRepository.findRecentTasks(PageRequest.of(0, 8)));

        // Tâches en retard
        stats.put("overdue", taskRepository.findOverdueTasks(LocalDate.now()));

        // Tâches à venir (2 jours)
        stats.put("dueSoon", taskRepository.findDueSoonTasks(LocalDate.now(), LocalDate.now().plusDays(2)));

        return stats;
    }

    // ─── Privé ────────────────────────────────────────────

    private void applyRequest(Task task, TaskRequest req) {
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());

        if (req.getProjectId() != null) {
            task.setProject(projectRepository.findById(req.getProjectId()).orElse(null));
        } else {
            task.setProject(null);
        }

        if (req.getAssignedToId() != null) {
            task.setAssignedTo(userRepository.findById(req.getAssignedToId()).orElse(null));
        } else {
            task.setAssignedTo(null);
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            task.setStatus(req.getStatus().toUpperCase());
        }
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            task.setPriority(req.getPriority().toUpperCase());
        }

        if (req.getDueDate() != null && !req.getDueDate().isBlank()) {
            task.setDueDate(LocalDate.parse(req.getDueDate()));
        } else {
            task.setDueDate(null);
        }
    }

    private void notify(User user, String message, String type, Task task) {
        notificationRepository.save(Notification.builder()
                .user(user).message(message).type(type).task(task).build());
    }

    public List<Task> getTasksForUser(User user) {
        return taskRepository.findAll().stream()
                .filter(t -> (t.getAssignedTo() != null && t.getAssignedTo().getId().equals(user.getId()))
                        || (t.getProject() != null && t.getProject().getOwner() != null
                            && t.getProject().getOwner().getId().equals(user.getId())))
                .collect(java.util.stream.Collectors.toList());
    }
}
