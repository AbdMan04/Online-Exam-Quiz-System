package com.quizapp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entry page. Real login/registration handling arrives with AuthController (R1-R5).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "login";
    }
}
