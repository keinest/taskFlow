package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.service.CommentService;
import com.taskflow.service.NotificationService;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentNotificationController {

    private final CommentService commentService;
    private final NotificationService notificationService;
    private final UserService userService;

    // ─── Commentaires ─────────────────────────────────────

    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long taskId,
                                        @RequestBody Map<String, String> body) {
        try {
            User current = userService.getCurrentUser();
            String content = body.get("content");
            if (content == null || content.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Le commentaire ne peut pas être vide"));
            var comment = commentService.add(taskId, content.trim(), current);
            return ResponseEntity.ok(Map.of(
                    "id", comment.getId(),
                    "content", comment.getContent(),
                    "authorName", comment.getUser().getFullName() != null
                            ? comment.getUser().getFullName() : comment.getUser().getUsername(),
                    "authorInitials", comment.getUser().getInitials(),
                    "authorColor", comment.getUser().getAvatarColor(),
                    "createdAt", comment.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        try {
            User current = userService.getCurrentUser();
            commentService.delete(id, current);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Non autorisé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


}
