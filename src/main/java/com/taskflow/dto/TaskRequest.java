package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskRequest {
    @NotBlank(message = "Le titre est requis")
    @Size(min = 2, max = 200)
    private String title;
    private String description;
    private Long projectId;
    private Long assignedToId;
    private String status;
    private String priority;
    private String dueDate; 
}
