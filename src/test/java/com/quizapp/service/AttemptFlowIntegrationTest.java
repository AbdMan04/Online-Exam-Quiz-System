package com.quizapp.service;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Attempt;
import com.quizapp.model.AttemptStatus;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Tag("integration")
@DisplayName("R14-R17 end to end on the H2 database")
class AttemptFlowIntegrationTest {

    @Autowired
    private QuizService quizService;
    @Autowired
    private AttemptService attemptService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("start, answer, submit, reject late answer, then hit the attempt limit")
    void attemptLifecycle() {
        Long instructorId = userRepository.findByEmail("instructor@uni.edu").orElseThrow().getId();
        Quiz quiz = quizService.createQuiz(instructorId, new QuizInput("One-shot quiz", 10, 60, 1));
        Question question = quizService.addQuestion(quiz.getId(), instructorId, new QuestionInput("2 + 2 = ?", 5,
                List.of(new OptionInput("4", true), new OptionInput("5", false))));
        Long studentId = authService.register("Stu Dent", "stu2@uni.edu", "Secure123").getId();

        BusinessException draft = assertThrows(BusinessException.class, () -> attemptService.start(quiz.getId(), studentId));
        assertEquals(Messages.QUIZ_NOT_AVAILABLE, draft.getMessage());

        quizService.publish(quiz.getId(), instructorId);
        Attempt attempt = attemptService.start(quiz.getId(), studentId);
        Long optionId = question.getOptions().get(0).getId();
        attemptService.submitAnswer(attempt.getId(), studentId, question.getId(), optionId);
        attemptService.submit(attempt.getId(), studentId);

        assertEquals(AttemptStatus.SUBMITTED, attemptService.getAttempt(attempt.getId(), studentId).getStatus());
        BusinessException late = assertThrows(BusinessException.class,
                () -> attemptService.submitAnswer(attempt.getId(), studentId, question.getId(), optionId));
        assertEquals(Messages.TIME_EXPIRED, late.getMessage());
        BusinessException limit = assertThrows(BusinessException.class, () -> attemptService.start(quiz.getId(), studentId));
        assertEquals(Messages.NO_ATTEMPTS_LEFT, limit.getMessage());
    }
}
