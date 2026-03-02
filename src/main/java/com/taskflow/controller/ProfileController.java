package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.service.NotificationService;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final NotificationService notificationService;

    @GetMapping("/profile")
    public String profilePage(Model model) {
        User current = userService.getCurrentUser();
        Map<String, Object> stats = taskService.getDashboardStats(current);

        model.addAttribute("user", current);
        model.addAttribute("stats", stats);
        model.addAttribute("projects", projectService.getAll());
        model.addAttribute("notifications", notificationService.getUserNotifications(current));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(current));
        model.addAttribute("page", "profile");
        return "profile";
    }

    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        try {
            User current = userService.getCurrentUser();

            String fullName = body.get("fullName");
            String email = body.get("email");
            String avatarColor = body.get("avatarColor");

            if (fullName != null && !fullName.isBlank()) current.setFullName(fullName);
            if (email != null && !email.isBlank()) current.setEmail(email);
            if (avatarColor != null && !avatarColor.isBlank()) current.setAvatarColor(avatarColor);

            User saved = userService.save(current);
            return ResponseEntity.ok(Map.of(
                "message", "Profil mis à jour avec succès",
                "fullName", saved.getFullName() != null ? saved.getFullName() : "",
                "email", saved.getEmail(),
                "avatarColor", saved.getAvatarColor(),
                "initials", saved.getInitials()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/profile/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        try {
            User current = userService.getCurrentUser();
            String currentPw = body.get("currentPassword");
            String newPw = body.get("newPassword");

            if (newPw == null || newPw.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe doit contenir au moins 6 caractères"));
            }

            userService.changePassword(current, currentPw, newPw);
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
