package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.entity.Project;
import com.taskflow.entity.User;
import com.taskflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> getAll() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    public Project findById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projet introuvable"));
    }

    @Transactional
    public Project create(ProjectRequest req, User owner) {
        Project p = Project.builder()
                .name(req.getName())
                .description(req.getDescription())
                .color(req.getColor() != null ? req.getColor() : "#6366f1")
                .owner(owner)
                .build();
        return projectRepository.save(p);
    }

    @Transactional
    public Project update(Long id, ProjectRequest req) {
        Project p = findById(id);
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        if (req.getColor() != null) p.setColor(req.getColor());
        return projectRepository.save(p);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.deleteById(id);
    }
}
