package com.taskflow.controller;

import com.taskflow.entity.*;
import com.taskflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final UserService userService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final TeamService teamService;
    private final NotificationService notificationService;

    @GetMapping("/search")
    public String searchPage(@RequestParam(value = "q", defaultValue = "") String query, Model model) {
        User current = userService.getCurrentUser();

        model.addAttribute("user", current);
        model.addAttribute("query", query);
        model.addAttribute("notifications", notificationService.getUserNotifications(current));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(current));
        model.addAttribute("page", "search");

        if (!query.isBlank()) {
            String q = query.toLowerCase().trim();

            // Search tasks
            List<Task> tasks = taskService.getTasksForUser(current).stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(q)
                            || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)))
                    .limit(15)
                    .collect(Collectors.toList());

            // Search projects
            List<Project> projects = projectService.getAll().stream()
                    .filter(p -> p.getName().toLowerCase().contains(q)
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)))
                    .limit(10)
                    .collect(Collectors.toList());

            // Search teams
            List<Team> teams = teamService.getPublicTeams().stream()
                    .filter(t -> t.getName().toLowerCase().contains(q)
                            || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)))
                    .limit(10)
                    .collect(Collectors.toList());

            model.addAttribute("tasks", tasks);
            model.addAttribute("projects", projects);
            model.addAttribute("teams", teams);
            model.addAttribute("totalResults", tasks.size() + projects.size() + teams.size());
        }

        return "search";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<?> searchApi(@RequestParam("q") String query) {
        try {
            User current = userService.getCurrentUser();
            if (query == null || query.isBlank()) {
                return ResponseEntity.ok(Map.of("results", List.of()));
            }
            String q = query.toLowerCase().trim();

            List<Map<String, Object>> results = new ArrayList<>();

            taskService.getTasksForUser(current).stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(q))
                    .limit(5)
                    .forEach(t -> results.add(Map.of(
                            "type", "task",
                            "id", t.getId(),
                            "title", t.getTitle(),
                            "url", "/tasks"
                    )));

            projectService.getAll().stream()
                    .filter(p -> p.getName().toLowerCase().contains(q))
                    .limit(3)
                    .forEach(p -> results.add(Map.of(
                            "type", "project",
                            "id", p.getId(),
                            "title", p.getName(),
                            "url", "/projects"
                    )));

            return ResponseEntity.ok(Map.of("results", results, "query", query));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
