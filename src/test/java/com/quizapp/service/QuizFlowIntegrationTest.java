package com.quizapp.service;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Attempt;
import com.quizapp.model.Quiz;
import com.quizapp.model.QuizStatus;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional // each test is rolled back, so tests do not affect each other
@Tag("integration")
@DisplayName("R6-R13 end to end on the H2 database")
class QuizFlowIntegrationTest {

    @Autowired
    private QuizService quizService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AttemptRepository attemptRepository;

    private Long instructorId() {
        return userRepository.findByEmail("instructor@uni.edu").orElseThrow().getId();
    }

    private static QuestionInput question() {
        return new QuestionInput("2 + 2 = ?", 5, List.of(
                new OptionInput("4", true), new OptionInput("5", false)));
    }

    @Test
    @DisplayName("create, add question, publish; then an attempt locks the questions")
    void fullFlow() {
        Long instructorId = instructorId();
        Quiz quiz = quizService.createQuiz(instructorId, new QuizInput("Java Basics", 30, 60, 2));
        quizService.addQuestion(quiz.getId(), instructorId, question());
        quizService.publish(quiz.getId(), instructorId);

        Quiz stored = quizService.getQuiz(quiz.getId(), instructorId);
        assertEquals(QuizStatus.PUBLISHED, stored.getStatus());
        assertEquals(1, stored.getQuestionCount());
        assertEquals(5, stored.getTotalPoints());

        User student = authService.register("Stu Dent", "stu@uni.edu", "Secure123");
        attemptRepository.save(new Attempt(stored, student, LocalDateTime.now()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> quizService.addQuestion(quiz.getId(), instructorId, question()));
        assertEquals(Messages.QUIZ_LOCKED, ex.getMessage());
    }
}
