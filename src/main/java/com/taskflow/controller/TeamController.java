package com.taskflow.controller;

import com.taskflow.dto.TeamRequest;
import com.taskflow.entity.Notification;
import com.taskflow.entity.Team;
import com.taskflow.entity.User;
import com.taskflow.service.NotificationService;
import com.taskflow.service.TeamService;
import com.taskflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/teams")
    public String teamsPage(Model model) {
        User current = userService.getCurrentUser();
        model.addAttribute("user", current);
        model.addAttribute("userTeams", teamService.getUserTeams(current));
        model.addAttribute("publicTeams", teamService.getPublicTeams());
        model.addAttribute("suggestions", teamService.getRandomSuggestions(5));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(current));
        model.addAttribute("page", "teams");
        return "teams";
    }

    @GetMapping("/teams/{id}")
    public String teamDetail(@PathVariable Long id, Model model) {
        User current = userService.getCurrentUser();
        Team team = teamService.findById(id);
        
        model.addAttribute("user", current);
        model.addAttribute("team", team);
        model.addAttribute("isMember", team.isMember(current));
        model.addAttribute("isCreator", team.isCreator(current));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(current));
        model.addAttribute("page", "teams");
        return "team-detail";
    }

    @PostMapping("/api/teams")
    @ResponseBody
    public ResponseEntity<?> createTeam(@Valid @RequestBody TeamRequest req) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.create(req.getName(), req.getDescription(), req.getIsPublic(), current);
            return ResponseEntity.ok(teamToMap(team, true, true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/teams/{id}")
    @ResponseBody
    public ResponseEntity<?> updateTeam(@PathVariable Long id, @Valid @RequestBody TeamRequest req) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(id);
            
            if (!team.isCreator(current)) {
                return ResponseEntity.status(403).body(Map.of("error", "Non autorisé"));
            }
            
            Team updated = teamService.update(id, req.getName(), req.getDescription(), req.getIsPublic());
            return ResponseEntity.ok(teamToMap(updated, true, true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/teams/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(id);
            
            if (!team.isCreator(current)) {
                return ResponseEntity.status(403).body(Map.of("error", "Non autorisé"));
            }
            
            teamService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Team supprimée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/teams/{id}/request-join")
    @ResponseBody
    public ResponseEntity<?> requestJoin(@PathVariable Long id) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(id);

            if (team.isMember(current)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vous êtes déjà membre de cette team"));
            }

            Notification notification = Notification.builder()
                    .user(team.getCreator())
                    .message(current.getUsername() + " demande à rejoindre la team " + team.getName())
                    .type("INFO")
                    .notificationType("TEAM_REQUEST")
                    .requestStatus("PENDING")
                    .team(team)
                    .read(false)
                    .build();

            notificationService.save(notification);
            return ResponseEntity.ok(Map.of("message", "Demande envoyée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/teams/{id}/join-requests")
    @ResponseBody
    public ResponseEntity<?> getJoinRequests(@PathVariable Long id) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(id);

            if (!team.isCreator(current)) {
                return ResponseEntity.status(403).body(Map.of("error", "Non autorisé"));
            }

            List<Notification> requests = notificationService.getUserNotifications(team.getCreator())
                    .stream()
                    .filter(n -> "TEAM_REQUEST".equals(n.getNotificationType())
                            && "PENDING".equals(n.getRequestStatus())
                            && n.getTeam() != null
                            && n.getTeam().getId().equals(id))
                    .toList();

            return ResponseEntity.ok(requests.stream()
                    .map(n -> {
                        /*
                         * BUG CORRIGÉ #3 : n.getUser() retourne le créateur (le destinataire
                         * de la notification), PAS le demandeur.
                         * La notification est créée avec user=creator pour qu'il la reçoive,
                         * et le nom du demandeur est encodé en début de message
                         * sous la forme "{username} demande à rejoindre...".
                         * On extrait le username et on récupère l'utilisateur correspondant.
                         */
                        String requesterUsername = n.getMessage().split(" demande")[0].trim();
                        User requester = userService.findByUsername(requesterUsername);
                        return Map.of(
                                "notificationId", n.getId(),
                                "userId",   requester.getId(),
                                "username", requester.getUsername(),
                                "fullName", requester.getFullName() != null
                                            ? requester.getFullName() : requester.getUsername(),
                                "avatarColor", requester.getAvatarColor(),
                                "message",  n.getMessage(),
                                "createdAt", n.getCreatedAt().toString()
                        );
                    })
                    .toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/teams/{teamId}/approve-request/{notifId}")
    @ResponseBody
    public ResponseEntity<?> approveJoinRequest(@PathVariable Long teamId, @PathVariable Long notifId) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(teamId);

            if (!team.isCreator(current)) {
                return ResponseEntity.status(403).body(Map.of("error", "Non autorisé"));
            }

            Notification notif = notificationService.findById(notifId);

            /*
             * BUG CORRIGÉ #3 (suite) : notif.getUser() = créateur, pas le demandeur.
             * On extrait le username du demandeur depuis le message de la notification.
             */
            String requesterUsername = notif.getMessage().split(" demande")[0].trim();
            User requester = userService.findByUsername(requesterUsername);

            teamService.addMember(teamId, requester);
            notif.setRequestStatus("ACCEPTED");
            notif.setRead(true);
            notificationService.save(notif);

            Notification responseNotif = Notification.builder()
                    .user(requester)
                    .message("Votre demande pour rejoindre " + team.getName() + " a été acceptée")
                    .type("SUCCESS")
                    .notificationType("TEAM_REQUEST")
                    .requestStatus("ACCEPTED")
                    .team(team)
                    .read(false)
                    .build();
            notificationService.save(responseNotif);

            return ResponseEntity.ok(Map.of("message", "Demande approuvée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/teams/{teamId}/reject-request/{notifId}")
    @ResponseBody
    public ResponseEntity<?> rejectJoinRequest(@PathVariable Long teamId, @PathVariable Long notifId) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(teamId);
            
            if (!team.isCreator(current)) {
                return ResponseEntity.status(403).body(Map.of("error", "Non autorisé"));
            }
            
            Notification notif = notificationService.findById(notifId);
            notif.setRequestStatus("REJECTED");
            notif.setRead(true);
            notificationService.save(notif);
            
            // Notifier l'utilisateur que sa demande a été rejetée
            Notification responseNotif = Notification.builder()
                    .user(notif.getUser())
                    .message("Votre demande pour rejoindre " + team.getName() + " a été rejetée")
                    .type("WARNING")
                    .notificationType("TEAM_REQUEST")
                    .requestStatus("REJECTED")
                    .team(team)
                    .read(false)
                    .build();
            notificationService.save(responseNotif);
            
            return ResponseEntity.ok(Map.of("message", "Demande rejetée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/teams/{id}/invite/{userId}")
    @ResponseBody
    public ResponseEntity<?> inviteUser(@PathVariable Long id, @PathVariable Long userId) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(id);
            
            if (!team.isCreator(current)) {
                return ResponseEntity.status(403).body(Map.of("error", "Non autorisé"));
            }
            
            User invitee = userService.findById(userId);
            
            // Créer une notification d'invitation
            Notification notification = Notification.builder()
                    .user(invitee)
                    .message(current.getUsername() + " vous invite à rejoindre la team " + team.getName())
                    .type("INFO")
                    .notificationType("TEAM_INVITATION")
                    .requestStatus("PENDING")
                    .team(team)
                    .read(false)
                    .build();
            
            notificationService.save(notification);
            return ResponseEntity.ok(Map.of("message", "Invitation envoyée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/teams/{id}/leave")
    @ResponseBody
    public ResponseEntity<?> leaveTeam(@PathVariable Long id) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(id);

            if (team.isCreator(current)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le créateur ne peut pas quitter sa propre team. Supprimez-la à la place."));
            }
            if (!team.isMember(current)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vous n'êtes pas membre de cette team"));
            }

            teamService.removeMember(id, current);
            return ResponseEntity.ok(Map.of("message", "Vous avez quitté la team"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/teams/{teamId}/members/{userId}")
    @ResponseBody
    public ResponseEntity<?> removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(teamId);

            if (!team.isCreator(current)) {
                return ResponseEntity.status(403).body(Map.of("error", "Seul le créateur peut retirer des membres"));
            }

            User member = userService.findById(userId);
            teamService.removeMember(teamId, member);
            return ResponseEntity.ok(Map.of("message", "Membre retiré"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/teams/{id}/accept-invitation")
    @ResponseBody
    public ResponseEntity<?> acceptInvitation(@PathVariable Long id) {
        try {
            User current = userService.getCurrentUser();
            Team team = teamService.findById(id);
            teamService.addMember(id, current);
            return ResponseEntity.ok(Map.of("message", "Invitation acceptée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> teamToMap(Team team, boolean isMember, boolean isCreator) {
        return Map.of(
                "id", team.getId(),
                "name", team.getName(),
                "description", team.getDescription() != null ? team.getDescription() : "",
                "isPublic", team.getIsPublic(),
                "color", team.getColor(),
                "memberCount", team.getMemberCount(),
                "isMember", isMember,
                "isCreator", isCreator,
                "creatorName", team.getCreator().getUsername(),
                "createdAt", team.getCreatedAt().toString()
        );
    }
}
