package com.quizapp.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * R4: role-specific landing pages. Access is guarded by {@link RoleInterceptor}.
 */
@Controller
public class DashboardController {

    @GetMapping("/student/dashboard")
    public String studentDashboard(@SessionAttribute(SessionUser.SESSION_KEY) SessionUser user, Model model) {
        model.addAttribute("user", user);
        return "student/dashboard";
    }

    @GetMapping("/instructor/dashboard")
    public String instructorDashboard(@SessionAttribute(SessionUser.SESSION_KEY) SessionUser user, Model model) {
        model.addAttribute("user", user);
        return "instructor/dashboard";
    }
}
