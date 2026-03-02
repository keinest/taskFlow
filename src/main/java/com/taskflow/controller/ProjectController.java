package com.taskflow.controller;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.entity.*;
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
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/projects")
    public String projectsPage(Model model) {
        User current = userService.getCurrentUser();
        model.addAttribute("user", current);
        model.addAttribute("projects", projectService.getAll());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(current));
        model.addAttribute("page", "projects");
        return "projects";
    }

    @PostMapping("/api/projects")
    @ResponseBody
    public ResponseEntity<?> create(@Valid @RequestBody ProjectRequest req) {
        try {
            User current = userService.getCurrentUser();
            Project p = projectService.create(req, current);
            return ResponseEntity.ok(projectToMap(p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/projects/{id}")
    @ResponseBody
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        try {
            Project p = projectService.update(id, req);
            return ResponseEntity.ok(projectToMap(p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/projects/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            projectService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/projects")
    @ResponseBody
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(projectService.getAll().stream().map(this::projectToMap).toList());
    }

    private Map<String, Object> projectToMap(Project p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("color", p.getColor());
        m.put("taskCount", p.getTaskCount());
        m.put("doneCount", p.getDoneCount());
        m.put("progressPercent", p.getProgressPercent());
        if (p.getOwner() != null) {
            m.put("ownerName", p.getOwner().getFullName());
            m.put("ownerInitials", p.getOwner().getInitials());
            m.put("ownerColor", p.getOwner().getAvatarColor());
        }
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return m;
    }
}
