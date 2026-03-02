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
