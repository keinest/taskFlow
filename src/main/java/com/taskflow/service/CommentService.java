package com.taskflow.service;

import com.taskflow.entity.*;
import com.taskflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;

    public List<Comment> getByTask(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    @Transactional
    public Comment add(Long taskId, String content, User author) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tâche introuvable"));

        Comment comment = Comment.builder()
                .task(task).user(author).content(content).build();
        comment = commentRepository.save(comment);

        // Notifier l'assigné et le créateur (sauf si c'est l'auteur)
        notifyIfDifferent(task.getAssignedTo(), author, task,
                "💬 " + author.getFullName() + " a commenté : \"" + task.getTitle() + "\"");
        if (task.getCreatedBy() != null
                && !task.getCreatedBy().getId().equals(author.getId())
                && (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(task.getCreatedBy().getId()))) {
            notifyIfDifferent(task.getCreatedBy(), author, task,
                    "💬 " + author.getFullName() + " a commenté : \"" + task.getTitle() + "\"");
        }

        return comment;
    }

    @Transactional
    public void delete(Long commentId, User requester) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Commentaire introuvable"));
        if (!c.getUser().getId().equals(requester.getId()) && !"ADMIN".equals(requester.getRole())) {
            throw new SecurityException("Non autorisé");
        }
        commentRepository.delete(c);
    }

    private void notifyIfDifferent(User target, User author, Task task, String message) {
        if (target != null && !target.getId().equals(author.getId())) {
            notificationRepository.save(Notification.builder()
                    .user(target).message(message).type("INFO").task(task).build());
        }
    }
}
