package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamRequest {

    @NotBlank(message = "Le nom de la team est obligatoire")
    private String name;

    private String description;

    private Boolean isPublic;
}
