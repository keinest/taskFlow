package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final NotificationService notificationService;

    /**
     * BUG CORRIGÉ #1 (suite) : "/" est retiré de ce mapping.
     * HomeController gère "/" et redirige vers "/dashboard" si authentifié.
     * Conserver "/" ici créait un conflit de routes avec HomeController.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User current = userService.getCurrentUser();
        Map<String, Object> stats = taskService.getDashboardStats(current);

        model.addAttribute("user", current);
        model.addAttribute("stats", stats);
        model.addAttribute("projects", projectService.getAll());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("notifications", notificationService.getUserNotifications(current));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(current));
        model.addAttribute("page", "dashboard");
        return "dashboard";
    }
}
