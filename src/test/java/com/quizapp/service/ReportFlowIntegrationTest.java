package com.quizapp.service;

import com.quizapp.dto.GradeResult;
import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.dto.QuizReport;
import com.quizapp.model.Attempt;
import com.quizapp.model.LetterGrade;
import com.quizapp.model.Outcome;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@Tag("integration")
@DisplayName("R18-R20 end to end on the H2 database")
class ReportFlowIntegrationTest {

    @Autowired
    private QuizService quizService;
    @Autowired
    private AttemptService attemptService;
    @Autowired
    private AuthService authService;
    @Autowired
    private GradingService gradingService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private UserRepository userRepository;

    private static QuestionInput question(String text, int points) {
        return new QuestionInput(text, points, List.of(new OptionInput("right", true), new OptionInput("wrong", false)));
    }

    /** Takes the quiz, answering the given questions correctly and the rest wrongly. */
    private Attempt takeQuiz(Quiz quiz, Long studentId, List<Question> questions, List<Question> correct) {
        Attempt attempt = attemptService.start(quiz.getId(), studentId);
        for (Question q : questions) {
            int optionIndex = correct.contains(q) ? 0 : 1;
            attemptService.submitAnswer(attempt.getId(), studentId, q.getId(), q.getOptions().get(optionIndex).getId());
        }
        return attemptService.submit(attempt.getId(), studentId);
    }

    @Test
    @DisplayName("grade attempts, keep each student's best and average the best scores")
    void gradesAndReports() {
        Long instructorId = userRepository.findByEmail("instructor@uni.edu").orElseThrow().getId();
        Quiz quiz = quizService.createQuiz(instructorId, new QuizInput("Graded quiz", 30, 60, 2));
        Question first10 = quizService.addQuestion(quiz.getId(), instructorId, question("Question 1", 10));
        Question second10 = quizService.addQuestion(quiz.getId(), instructorId, question("Question 2", 10));
        quizService.publish(quiz.getId(), instructorId);
        List<Question> questions = List.of(first10, second10);

        Long alice = authService.register("Alice", "alice.r@uni.edu", "Secure123").getId();
        Long bob = authService.register("Bob", "bob.r@uni.edu", "Secure123").getId();

        Attempt aliceFirst = takeQuiz(quiz, alice, questions, List.of());           // 0 / 20  = 0 %
        takeQuiz(quiz, alice, questions, List.of(first10));                         // 10 / 20 = 50 %
        takeQuiz(quiz, bob, questions, List.of(first10, second10));                 // 20 / 20 = 100 %

        GradeResult first = gradingService.grade(aliceFirst);
        assertEquals(new BigDecimal("0.00"), first.score());
        assertEquals(Outcome.FAIL, first.outcome());
        assertEquals(LetterGrade.F, first.letterGrade());

        QuizReport report = reportService.quizReport(quiz.getId(), instructorId);
        assertEquals(new BigDecimal("50.00"), report.students().get(0).bestScore());
        assertEquals(new BigDecimal("100.00"), report.students().get(1).bestScore());
        assertEquals(new BigDecimal("75.00"), report.classAverage());
        assertEquals(1, report.passedCount());
    }
}
