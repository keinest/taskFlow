package com.taskflow.controller;

import com.taskflow.service.TeamService;
import com.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final TeamService teamService;
    private final UserService userService;

    /**
     * Page d'accueil publique.
     * ─ Si authentifié → redirige vers /dashboard (géré par DashboardController).
     * ─ Sinon → affiche home.html.
     *
     * BUG CORRIGÉ #1 : suppression de l'ancien @GetMapping("/dashboard") qui
     * entrait en conflit avec DashboardController (lui aussi mappé sur
     * {"/", "/dashboard"}), provoquant une IllegalStateException au démarrage.
     * De plus, cette ancienne méthode ne peuplait pas le modèle avec les
     * attributs requis par dashboard.html (stats, notifications, unreadCount…).
     */
    @GetMapping("/")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());

        model.addAttribute("publicTeams", teamService.getPublicTeams());
        model.addAttribute("publicTeamsCount", teamService.getPublicTeams().size());
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            try {
                model.addAttribute("user", userService.getCurrentUser());
            } catch (Exception ignored) {}
        }

        return "home";
    }
}
