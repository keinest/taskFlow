package com.taskflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    // TODO, IN_PROGRESS, DONE
    @Builder.Default
    @Column(nullable = false)
    private String status = "TODO";

    // LOW, MEDIUM, HIGH, CRITICAL
    @Builder.Default
    @Column(nullable = false)
    private String priority = "MEDIUM";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Builder.Default
    private Integer position = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // ─── Utilitaires visuels ───────────────────────────────

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && !"DONE".equals(status);
    }

    public boolean isDueSoon() {
        if (dueDate == null || "DONE".equals(status)) return false;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        return days >= 0 && days <= 2;
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public String getPriorityBadgeClass() {
        return switch (priority) {
            case "CRITICAL" -> "badge-critical";
            case "HIGH"     -> "badge-high";
            case "MEDIUM"   -> "badge-medium";
            default         -> "badge-low";
        };
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case "DONE"        -> "badge-done";
            case "IN_PROGRESS" -> "badge-progress";
            default            -> "badge-todo";
        };
    }

    public String getStatusLabel() {
        return switch (status) {
            case "DONE"        -> "Terminée";
            case "IN_PROGRESS" -> "En cours";
            default            -> "À faire";
        };
    }

    public String getPriorityLabel() {
        return switch (priority) {
            case "CRITICAL" -> "Critique";
            case "HIGH"     -> "Haute";
            case "MEDIUM"   -> "Moyenne";
            default         -> "Basse";
        };
    }
}
