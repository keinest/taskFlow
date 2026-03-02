package com.taskflow.controller;

import com.taskflow.entity.Notification;
import com.taskflow.entity.User;
import com.taskflow.service.NotificationService;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getUserNotifications() {
        try {
            User current = userService.getCurrentUser();
            List<Notification> notifications = notificationService.getUserNotifications(current);
            return ResponseEntity.ok(notifications.stream()
                    .map(this::notificationToMap)
                    .toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/team-requests")
    public ResponseEntity<?> getTeamRequests() {
        try {
            User current = userService.getCurrentUser();
            List<Notification> notifications = notificationService.getUserNotifications(current);
            List<Notification> teamRequests = notifications.stream()
                    .filter(n -> "TEAM_REQUEST".equals(n.getNotificationType()))
                    .toList();
            return ResponseEntity.ok(teamRequests.stream()
                    .map(this::notificationToMap)
                    .toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markRead(id);
            return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            User current = userService.getCurrentUser();
            notificationService.markAllRead(current);
            return ResponseEntity.ok(Map.of("message", "Toutes les notifications marquées comme lues"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> notificationToMap(Notification n) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", n.getId());
        map.put("message", n.getMessage());
        map.put("type", n.getType());
        map.put("notificationType", n.getNotificationType());
        map.put("read", n.isRead());
        map.put("requestStatus", n.getRequestStatus());
        if (n.getTeam() != null) {
            map.put("teamId", n.getTeam().getId());
            map.put("teamName", n.getTeam().getName());
        }
        if (n.getTask() != null) {
            map.put("taskId", n.getTask().getId());
            map.put("taskTitle", n.getTask().getTitle());
        }
        map.put("createdAt", n.getCreatedAt().toString());
        return map;
    }
}
