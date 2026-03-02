package com.taskflow.controller;

import com.taskflow.entity.User;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        return ResponseEntity.ok(
            userService.getAllUsers().stream()
                .map(this::userToMap)
                .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(userToMap(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> userToMap(User user) {
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "fullName", user.getFullName() != null ? user.getFullName() : user.getUsername(),
            "email", user.getEmail(),
            "avatarColor", user.getAvatarColor(),
            "createdAt", user.getCreatedAt().toString()
        );
    }
}
