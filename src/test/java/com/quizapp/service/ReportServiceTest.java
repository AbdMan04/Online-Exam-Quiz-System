package com.quizapp.service;

import com.quizapp.dto.QuizReport;
import com.quizapp.dto.StudentResult;
import com.quizapp.model.AnswerOption;
import com.quizapp.model.Attempt;
import com.quizapp.model.LetterGrade;
import com.quizapp.model.Outcome;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService - R20")
class ReportServiceTest {

    private static final Long QUIZ_ID = 10L;
    private static final Long INSTRUCTOR_ID = 1L;
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 25, 10, 0);

    @Mock
    private QuizService quizService;
    @Mock
    private AttemptService attemptService;
    @Mock
    private AttemptRepository attemptRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(quizService, attemptService, new GradingService(), attemptRepository);
    }

    @Nested
    @DisplayName("Pure calculations")
    class Calculations {

        @Test
        @DisplayName("best score is the highest attempt")
        void bestScore() {
            assertEquals(new BigDecimal("80.00"), reportService.bestScore(
                    List.of(new BigDecimal("40.00"), new BigDecimal("80.00"), new BigDecimal("60.00"))));
        }

        @Test
        @DisplayName("best score needs at least one score")
        void bestScoreEmpty() {
            assertThrows(IllegalArgumentException.class, () -> reportService.bestScore(List.of()));
        }

        @Test
        @DisplayName("class average is rounded half-up to 2 decimals")
        void classAverage() {
            assertEquals(new BigDecimal("66.67"), reportService.classAverage(
                    List.of(new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))));
        }

        @Test
        @DisplayName("class average of nobody is 0.00")
        void classAverageEmpty() {
            assertEquals(new BigDecimal("0.00"), reportService.classAverage(List.of()));
        }
    }

    @Nested
    @DisplayName("Quiz report")
    class Report {

        private Quiz quiz;
        private Question q1;
        private Question q2;

        @BeforeEach
        void buildQuiz() {
            User instructor = new User("Dr. Smith", "smith@uni.edu", "hash", Role.INSTRUCTOR);
            quiz = new Quiz("Java Basics", 30, 60, 3, instructor);
            ReflectionTestUtils.setField(quiz, "id", QUIZ_ID);
            q1 = new Question("Q1", 15, List.of(new AnswerOption("right", true), new AnswerOption("wrong", false)));
            q2 = new Question("Q2", 5, List.of(new AnswerOption("right", true), new AnswerOption("wrong", false)));
            quiz.addQuestion(q1);
            quiz.addQuestion(q2);
            quiz.publish();
        }

        private User student(long id, String name) {
            User student = new User(name, name.toLowerCase() + "@uni.edu", "hash", Role.STUDENT);
            ReflectionTestUtils.setField(student, "id", id);
            return student;
        }

        /** A submitted attempt where the given questions were answered correctly. */
        private Attempt submitted(User student, Question... correct) {
            Attempt attempt = new Attempt(quiz, student, T0);
            for (Question question : correct) {
                attempt.recordAnswer(question, question.getOptions().get(0));
            }
            attempt.submit(T0.plusMinutes(10));
            return attempt;
        }

        @Test
        @DisplayName("R20 each student's best attempt is reported and the average uses best scores only")
        void report() {
            User alice = student(5, "Alice");
            User bob = student(6, "Bob");
            Attempt aliceFirst = submitted(alice, q2);          // 5/20  = 25.00
            Attempt aliceSecond = submitted(alice, q1);         // 15/20 = 75.00
            Attempt bobOnly = submitted(bob, q1, q2);           // 20/20 = 100.00
            Attempt carolOpen = new Attempt(quiz, student(7, "Carol"), T0); // still running - not reported
            when(quizService.getQuiz(QUIZ_ID, INSTRUCTOR_ID)).thenReturn(quiz);
            when(attemptRepository.findByQuizIdOrderByStartedAtAsc(QUIZ_ID))
                    .thenReturn(List.of(aliceFirst, bobOnly, aliceSecond, carolOpen));

            QuizReport report = reportService.quizReport(QUIZ_ID, INSTRUCTOR_ID);

            StudentResult aliceRow = report.students().get(0);
            assertAll(
                    () -> assertEquals(2, report.students().size()),
                    () -> assertEquals("Alice", aliceRow.studentName()),
                    () -> assertEquals(2, aliceRow.submittedAttempts()),
                    () -> assertEquals(new BigDecimal("75.00"), aliceRow.bestScore()),
                    () -> assertEquals(Outcome.PASS, aliceRow.outcome()),
                    () -> assertEquals(LetterGrade.C, aliceRow.letterGrade()),
                    () -> assertEquals(new BigDecimal("100.00"), report.students().get(1).bestScore()),
                    () -> assertEquals(new BigDecimal("87.50"), report.classAverage()),
                    () -> assertEquals(2, report.passedCount())
            );
        }
    }
}
