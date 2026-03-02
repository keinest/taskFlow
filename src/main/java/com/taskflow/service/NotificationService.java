package com.taskflow.service;

import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }

    @Transactional
    public void markRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
    }

    @Transactional
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    // Vérification quotidienne des tâches en retard (chaque heure)
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void checkOverdueTasks() {
        List<Task> overdue = taskRepository.findOverdueTasks(LocalDate.now());
        for (Task task : overdue) {
            if (task.getAssignedTo() != null) {
                // Eviter les doublons : vérifier si notification déjà envoyée aujourd'hui
                boolean alreadySent = notificationRepository
                        .findByUserAndReadFalseOrderByCreatedAtDesc(task.getAssignedTo())
                        .stream()
                        .anyMatch(n -> n.getTask() != null
                                && n.getTask().getId().equals(task.getId())
                                && n.getMessage().contains("retard"));
                if (!alreadySent) {
                    notificationRepository.save(Notification.builder()
                            .user(task.getAssignedTo())
                            .message("⚠️ Tâche en retard : \"" + task.getTitle() + "\"")
                            .type("WARNING").task(task).build());
                }
            }
        }
    }
}
