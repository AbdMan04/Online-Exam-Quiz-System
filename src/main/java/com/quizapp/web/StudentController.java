package com.quizapp.web;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Attempt;
import com.quizapp.service.AttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Map;

/**
 * Student pages for R14-R17. Every rule lives in {@link AttemptService}.
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private static final String DASHBOARD = "redirect:/student/dashboard";

    private final AttemptService attemptService;

    public StudentController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @ModelAttribute("user")
    public SessionUser currentUser(@SessionAttribute(SessionUser.SESSION_KEY) SessionUser user) {
        return user;
    }

    /** Errors such as "Quiz not available" or "No attempts remaining" are shown on the dashboard. */
    @ExceptionHandler(BusinessException.class)
    public String backToDashboard(BusinessException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
        return DASHBOARD;
    }

    @GetMapping("/dashboard")
    public String dashboard(@ModelAttribute("user") SessionUser user, Model model) {
        model.addAttribute("quizzes", attemptService.listAvailableQuizzes(user.id()));
        return "student/dashboard";
    }

    @PostMapping("/quizzes/{quizId}/start")
    public String start(@ModelAttribute("user") SessionUser user, @PathVariable Long quizId) {
        Attempt attempt = attemptService.start(quizId, user.id());
        return attemptPage(attempt.getId());
    }

    @GetMapping("/attempts/{attemptId}")
    public String attempt(@ModelAttribute("user") SessionUser user, @PathVariable Long attemptId, Model model) {
        Attempt attempt = attemptService.getAttempt(attemptId, user.id());
        model.addAttribute("attempt", attempt);
        model.addAttribute("quiz", attempt.getQuiz());
        model.addAttribute("selected", attempt.getSelectedOptionIds());
        model.addAttribute("remainingSeconds", attemptService.remainingSeconds(attempt));
        return "student/attempt";
    }

    /** Called by the page each time an option is chosen (R17: answers are saved as they are given). */
    @PostMapping("/attempts/{attemptId}/answers")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveAnswer(@ModelAttribute("user") SessionUser user,
                                                          @PathVariable Long attemptId,
                                                          @RequestParam Long questionId,
                                                          @RequestParam Long optionId) {
        try {
            attemptService.submitAnswer(attemptId, user.id(), questionId, optionId);
            return ResponseEntity.ok(Map.of("status", "saved"));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public String submit(@ModelAttribute("user") SessionUser user, @PathVariable Long attemptId,
                         RedirectAttributes redirect) {
        attemptService.submit(attemptId, user.id());
        redirect.addFlashAttribute("message", Messages.ATTEMPT_SUBMITTED);
        return attemptPage(attemptId);
    }

    private static String attemptPage(Long attemptId) {
        return "redirect:/student/attempts/" + attemptId;
    }
}
