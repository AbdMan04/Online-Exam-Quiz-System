package com.quizapp.web;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.User;
import com.quizapp.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpSession session, Model model) {
        try {
            SessionUser user = SessionUser.from(authService.login(email, password));
            session.setAttribute(SessionUser.SESSION_KEY, user);
            return "redirect:" + user.dashboardPath();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                           Model model, RedirectAttributes redirect) {
        try {
            User user = authService.register(name, email, password);
            redirect.addFlashAttribute("message", Messages.REGISTER_OK);
            redirect.addFlashAttribute("email", user.getEmail());
            return "redirect:/login";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
