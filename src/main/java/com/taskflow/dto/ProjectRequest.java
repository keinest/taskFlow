package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectRequest {
    @NotBlank(message = "Le nom du projet est requis")
    private String name;
    private String description;
    private String color;
}
