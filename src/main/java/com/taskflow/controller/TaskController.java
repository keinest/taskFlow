package com.taskflow.controller;

import com.taskflow.dto.TaskRequest;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final CommentService commentService;

    // === Page tâches ======================================
    @GetMapping("/tasks")
    public String tasksPage(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String search,
            Model model) {

        User current = userService.getCurrentUser();
        List<Task> tasks = taskService.getFiltered(status, priority, projectId, assigneeId, search);

        // Convertir en Maps simples pour éviter la sérialisation des proxies Hibernate en JS inline
        List<Map<String, Object>> usersSimple = userService.getAllUsers().stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("fullName", u.getFullName() != null ? u.getFullName() : u.getUsername());
                    return m;
                }).toList();

        List<Map<String, Object>> projectsSimple = projectService.getAll().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("color", p.getColor());
                    return m;
                }).toList();

        model.addAttribute("user", current);
        model.addAttribute("tasks", tasks);
        model.addAttribute("projects", projectService.getAll());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("usersSimple", usersSimple);
        model.addAttribute("projectsSimple", projectsSimple);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(current));
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterPriority", priority);
        model.addAttribute("filterProjectId", projectId);
        model.addAttribute("filterAssigneeId", assigneeId);
        model.addAttribute("search", search);
        model.addAttribute("page", "tasks");
        return "tasks";
    }

    // === API REST =========================================

    @PostMapping("/api/tasks")
    @ResponseBody
    public ResponseEntity<?> create(@Valid @RequestBody TaskRequest req) {
        try {
            User current = userService.getCurrentUser();
            Task task = taskService.create(req, current);
            return ResponseEntity.ok(taskToMap(task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        try {
            User current = userService.getCurrentUser();
            Task task = taskService.update(id, req, current);
            return ResponseEntity.ok(taskToMap(task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            taskService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/api/tasks/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            User current = userService.getCurrentUser();
            Task task = taskService.updateStatus(id, body.get("status"), current);
            return ResponseEntity.ok(taskToMap(task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/api/tasks/{id}/reorder")
    @ResponseBody
    public ResponseEntity<?> reorder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String newStatus = (String) body.get("status");
            int newPosition = (Integer) body.get("position");
            taskService.reorder(id, newStatus, newPosition);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<?> getTask(@PathVariable Long id) {
        try {
            Task task = taskService.findById(id);
            Map<String, Object> result = taskToMap(task);
            // Ajouter commentaires
            List<Map<String, Object>> comments = commentService.getByTask(id).stream().map(c -> {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", c.getId());
                cm.put("content", c.getContent());
                cm.put("authorName", c.getUser().getFullName() != null ? c.getUser().getFullName() : c.getUser().getUsername());
                cm.put("authorInitials", c.getUser().getInitials());
                cm.put("authorColor", c.getUser().getAvatarColor());
                cm.put("createdAt", c.getCreatedAt().toString());
                return cm;
            }).toList();
            result.put("comments", comments);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // === Utilitaire sérialisation =========================

    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", task.getId());
        m.put("title", task.getTitle());
        m.put("description", task.getDescription());
        m.put("status", task.getStatus());
        m.put("statusLabel", task.getStatusLabel());
        m.put("priority", task.getPriority());
        m.put("priorityLabel", task.getPriorityLabel());
        m.put("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : null);
        m.put("overdue", task.isOverdue());
        m.put("dueSoon", task.isDueSoon());
        m.put("position", task.getPosition());
        m.put("priorityBadgeClass", task.getPriorityBadgeClass());
        m.put("statusBadgeClass", task.getStatusBadgeClass());

        if (task.getProject() != null) {
            m.put("projectId", task.getProject().getId());
            m.put("projectName", task.getProject().getName());
            m.put("projectColor", task.getProject().getColor());
        }
        if (task.getAssignedTo() != null) {
            m.put("assigneeId", task.getAssignedTo().getId());
            m.put("assigneeName", task.getAssignedTo().getFullName() != null
                    ? task.getAssignedTo().getFullName() : task.getAssignedTo().getUsername());
            m.put("assigneeInitials", task.getAssignedTo().getInitials());
            m.put("assigneeColor", task.getAssignedTo().getAvatarColor());
        }
        m.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
        m.put("commentCount", task.getComments().size());
        return m;
    }
}
