package com.taskflow.service;

import com.taskflow.entity.Team;
import com.taskflow.entity.User;
import com.taskflow.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    public List<Team> getAll() {
        return teamRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Team> getPublicTeams() {
        return teamRepository.findByIsPublicTrue();
    }

    public List<Team> getRandomSuggestions(int limit) {
        List<Team> publicTeams = new java.util.ArrayList<>(
            teamRepository.findPublicTeamsForSuggestions(
                org.springframework.data.domain.PageRequest.of(0, Math.max(limit * 3, 20))
            ) // Correction : suppression de .getContent()
        );
        java.util.Collections.shuffle(publicTeams);
        return publicTeams.stream().limit(limit).toList();
    }

    public List<Team> getUserTeams(User user) {
        return teamRepository.findTeamsByUserId(user.getId());
    }

    public List<Team> getTeamsByCreator(User creator) {
        return teamRepository.findByCreatorId(creator.getId());
    }

    public Team findById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team introuvable (ID: " + id + ")"));
    }

    @Transactional
    public Team create(String name, String description, Boolean isPublic, User creator) {
        Team team = Team.builder()
                .name(name)
                .description(description)
                .creator(creator)
                .isPublic(isPublic != null ? isPublic : true)
                .color(getRandomColor())
                .build();
        return teamRepository.save(team);
    }

    @Transactional
    public Team update(Long id, String name, String description, Boolean isPublic) {
        Team team = findById(id);
        if (name != null) team.setName(name);
        if (description != null) team.setDescription(description);
        if (isPublic != null) team.setIsPublic(isPublic);
        return teamRepository.save(team);
    }

    @Transactional
    public void addMember(Long teamId, User user) {
        Team team = findById(teamId);
        if (!team.getMembers().contains(user)) {
            team.getMembers().add(user);
            teamRepository.save(team);
        }
    }

    @Transactional
    public void removeMember(Long teamId, User user) {
        Team team = findById(teamId);
        team.getMembers().remove(user);
        teamRepository.save(team);
    }

    @Transactional
    public void delete(Long id) {
        teamRepository.deleteById(id);
    }

    private String getRandomColor() {
        String[] colors = {"#6366f1", "#ec4899", "#f59e0b", "#10b981", "#3b82f6", "#8b5cf6", "#06b6d4", "#f97316"};
        return colors[(int) (Math.random() * colors.length)];
    }
}