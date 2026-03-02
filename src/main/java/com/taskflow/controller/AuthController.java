package com.taskflow.controller;

import com.taskflow.dto.RegisterRequest;
import com.taskflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        Model model) {
        if (error != null) model.addAttribute("error", "Identifiants invalides. Vérifiez vos informations.");
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest req,
                           BindingResult result,
                           Model model,
                           RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.register(req);
            flash.addFlashAttribute("success", "Compte créé ! Connectez-vous avec vos identifiants.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
