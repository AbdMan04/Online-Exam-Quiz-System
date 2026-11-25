package com.quizapp.web;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.service.QuizService;
import com.quizapp.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Instructor pages for R6-R13 and the R20 report. Rules live in the services; this class only maps
 * requests to service calls and shows the resulting message.
 */
@Controller
@RequestMapping("/instructor")
public class InstructorController {

    private static final String DASHBOARD = "redirect:/instructor/dashboard";
    private static final String QUIZ_FORM = "instructor/quiz-form";
    private static final String QUESTION_FORM = "instructor/question-form";

    private final QuizService quizService;
    private final ReportService reportService;

    public InstructorController(QuizService quizService, ReportService reportService) {
        this.quizService = quizService;
        this.reportService = reportService;
    }

    @ModelAttribute("user")
    public SessionUser currentUser(@SessionAttribute(SessionUser.SESSION_KEY) SessionUser user) {
        return user;
    }

    /** Errors not handled on a form (e.g. "Quiz not found") go back to the dashboard. */
    @ExceptionHandler(BusinessException.class)
    public String backToDashboard(BusinessException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
        return DASHBOARD;
    }

    @GetMapping("/dashboard")
    public String dashboard(@ModelAttribute("user") SessionUser user, Model model) {
        model.addAttribute("quizzes", quizService.listQuizzes(user.id()));
        return "instructor/dashboard";
    }

    @GetMapping("/quizzes/new")
    public String newQuiz(Model model) {
        model.addAttribute("quizForm", new QuizForm());
        return QUIZ_FORM;
    }

    @PostMapping("/quizzes")
    public String createQuiz(@ModelAttribute("user") SessionUser user, @ModelAttribute("quizForm") QuizForm form,
                             Model model, RedirectAttributes redirect) {
        try {
            Quiz quiz = quizService.createQuiz(user.id(), form.toInput());
            redirect.addFlashAttribute("message", Messages.QUIZ_CREATED);
            return quizPage(quiz.getId());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return QUIZ_FORM;
        }
    }

    @GetMapping("/quizzes/{quizId}")
    public String quizDetail(@ModelAttribute("user") SessionUser user, @PathVariable Long quizId, Model model) {
        model.addAttribute("quiz", quizService.getQuiz(quizId, user.id()));
        model.addAttribute("locked", quizService.hasAttempts(quizId));
        return "instructor/quiz-detail";
    }

    /** R20 */
    @GetMapping("/quizzes/{quizId}/report")
    public String report(@ModelAttribute("user") SessionUser user, @PathVariable Long quizId, Model model) {
        model.addAttribute("report", reportService.quizReport(quizId, user.id()));
        return "instructor/report";
    }

    @PostMapping("/quizzes/{quizId}/publish")
    public String publish(@ModelAttribute("user") SessionUser user, @PathVariable Long quizId,
                          RedirectAttributes redirect) {
        try {
            quizService.publish(quizId, user.id());
            redirect.addFlashAttribute("message", Messages.QUIZ_PUBLISHED);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return quizPage(quizId);
    }

    @GetMapping("/quizzes/{quizId}/questions/new")
    public String newQuestion(@ModelAttribute("user") SessionUser user, @PathVariable Long quizId, Model model) {
        Quiz quiz = quizService.getQuiz(quizId, user.id());
        return showQuestionForm(model, quiz, new QuestionForm(), addQuestionAction(quizId));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public String addQuestion(@ModelAttribute("user") SessionUser user, @PathVariable Long quizId,
                              @ModelAttribute("questionForm") QuestionForm form,
                              Model model, RedirectAttributes redirect) {
        try {
            quizService.addQuestion(quizId, user.id(), form.toInput());
            redirect.addFlashAttribute("message", Messages.QUESTION_ADDED);
            return quizPage(quizId);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showQuestionForm(model, quizService.getQuiz(quizId, user.id()), form, addQuestionAction(quizId));
        }
    }

    @GetMapping("/questions/{questionId}/edit")
    public String editQuestionForm(@ModelAttribute("user") SessionUser user, @PathVariable Long questionId,
                                   Model model) {
        Question question = quizService.getQuestion(questionId, user.id());
        return showQuestionForm(model, question.getQuiz(), QuestionForm.from(question), editQuestionAction(questionId));
    }

    @PostMapping("/questions/{questionId}")
    public String editQuestion(@ModelAttribute("user") SessionUser user, @PathVariable Long questionId,
                               @ModelAttribute("questionForm") QuestionForm form,
                               Model model, RedirectAttributes redirect) {
        Quiz quiz = quizService.getQuestion(questionId, user.id()).getQuiz();
        try {
            quizService.editQuestion(questionId, user.id(), form.toInput());
            redirect.addFlashAttribute("message", Messages.QUESTION_UPDATED);
            return quizPage(quiz.getId());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showQuestionForm(model, quiz, form, editQuestionAction(questionId));
        }
    }

    @PostMapping("/questions/{questionId}/delete")
    public String deleteQuestion(@ModelAttribute("user") SessionUser user, @PathVariable Long questionId,
                                 RedirectAttributes redirect) {
        Long quizId = quizService.getQuestion(questionId, user.id()).getQuiz().getId();
        try {
            quizService.deleteQuestion(questionId, user.id());
            redirect.addFlashAttribute("message", Messages.QUESTION_DELETED);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return quizPage(quizId);
    }

    private static String showQuestionForm(Model model, Quiz quiz, QuestionForm form, String action) {
        model.addAttribute("quiz", quiz);
        model.addAttribute("questionForm", form);
        model.addAttribute("formAction", action);
        return QUESTION_FORM;
    }

    private static String addQuestionAction(Long quizId) {
        return "/instructor/quizzes/" + quizId + "/questions";
    }

    private static String editQuestionAction(Long questionId) {
        return "/instructor/questions/" + questionId;
    }

    private static String quizPage(Long quizId) {
        return "redirect:/instructor/quizzes/" + quizId;
    }
}
