package com.quizapp.service;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.AnswerOption;
import com.quizapp.model.Attempt;
import com.quizapp.model.AttemptStatus;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttemptService - R14 to R17")
class AttemptServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 25, 10, 0);
    private static final int DURATION = 30;
    private static final Long QUIZ_ID = 10L;
    private static final Long STUDENT_ID = 5L;
    private static final Long ATTEMPT_ID = 50L;
    private static final Long QUESTION_ID = 100L;
    private static final Long RIGHT_OPTION = 1000L;
    private static final Long WRONG_OPTION = 1001L;

    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private UserRepository userRepository;

    private User student;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        User instructor = new User("Dr. Smith", "smith@uni.edu", "hash", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(instructor, "id", 1L);
        student = new User("Alice", "alice@uni.edu", "hash", Role.STUDENT);
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);

        quiz = new Quiz("Java Basics", DURATION, 60, 2, instructor);
        ReflectionTestUtils.setField(quiz, "id", QUIZ_ID);
        AnswerOption right = new AnswerOption("A virtual machine", true);
        AnswerOption wrong = new AnswerOption("A compiler", false);
        ReflectionTestUtils.setField(right, "id", RIGHT_OPTION);
        ReflectionTestUtils.setField(wrong, "id", WRONG_OPTION);
        Question question = new Question("What is the JVM?", 5, List.of(right, wrong));
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        quiz.addQuestion(question);
        quiz.publish();
    }

    /** The service as it behaves at the given moment. */
    private AttemptService serviceAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new AttemptService(attemptRepository, quizRepository, userRepository, clock);
    }

    private Attempt attemptStartedAt(LocalDateTime startedAt) {
        Attempt attempt = new Attempt(quiz, student, startedAt);
        ReflectionTestUtils.setField(attempt, "id", ATTEMPT_ID);
        return attempt;
    }

    private static String messageOf(Executable action) {
        return assertThrows(BusinessException.class, action).getMessage();
    }

    @Nested
    @DisplayName("Start (R14-R16)")
    class Start {

        @Test
        @DisplayName("R14 a PUBLISHED quiz with attempts left starts a new attempt and the timer")
        void startsAttempt() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());
            when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(1L);
            when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(attemptRepository.save(any(Attempt.class))).thenAnswer(call -> call.getArgument(0));

            Attempt attempt = serviceAt(T0).start(QUIZ_ID, STUDENT_ID);

            assertEquals(T0, attempt.getStartedAt());
            assertEquals(T0.plusMinutes(DURATION), attempt.getDeadline());
            assertTrue(attempt.isInProgress());
        }

        @Test
        @DisplayName("R14 an open, unexpired attempt is resumed instead of creating a new one")
        void resumesOpenAttempt() {
            Attempt open = attemptStartedAt(T0);
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(open));

            assertSame(open, serviceAt(T0.plusMinutes(5)).start(QUIZ_ID, STUDENT_ID));
            verify(attemptRepository, never()).save(any());
        }

        @Test
        @DisplayName("R15 all attempts used -> No attempts remaining")
        void noAttemptsLeft() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());
            when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(2L);

            assertEquals(Messages.NO_ATTEMPTS_LEFT, messageOf(() -> serviceAt(T0).start(QUIZ_ID, STUDENT_ID)));
        }

        @Test
        @DisplayName("R15 an expired open attempt is auto-submitted and still counts toward the limit")
        void expiredOpenAttemptCounts() {
            Attempt open = attemptStartedAt(T0);
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(open));
            when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(2L);

            assertEquals(Messages.NO_ATTEMPTS_LEFT,
                    messageOf(() -> serviceAt(T0.plusMinutes(DURATION)).start(QUIZ_ID, STUDENT_ID)));
            assertEquals(AttemptStatus.SUBMITTED, open.getStatus());
        }

        @Test
        @DisplayName("R16 a DRAFT quiz cannot be started")
        void draftQuiz() {
            Quiz draft = new Quiz("Draft", DURATION, 60, 2, quiz.getInstructor());
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(draft));

            assertEquals(Messages.QUIZ_NOT_AVAILABLE, messageOf(() -> serviceAt(T0).start(QUIZ_ID, STUDENT_ID)));
        }

        @Test
        @DisplayName("R16 a quiz that does not exist is not available")
        void missingQuiz() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

            assertEquals(Messages.QUIZ_NOT_AVAILABLE, messageOf(() -> serviceAt(T0).start(QUIZ_ID, STUDENT_ID)));
        }
    }

    @Nested
    @DisplayName("Answers and timer (R17)")
    class Timer {

        @Test
        @DisplayName("one second before the deadline the attempt is still running")
        void beforeDeadline() {
            Attempt attempt = attemptStartedAt(T0);

            assertFalse(serviceAt(T0.plusMinutes(DURATION).minusSeconds(1)).checkExpiry(attempt));
            assertTrue(attempt.isInProgress());
        }

        @Test
        @DisplayName("exactly at the deadline the attempt is auto-submitted at the deadline")
        void atDeadline() {
            Attempt attempt = attemptStartedAt(T0);

            assertTrue(serviceAt(T0.plusMinutes(DURATION)).checkExpiry(attempt));
            assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
            assertEquals(T0.plusMinutes(DURATION), attempt.getSubmittedAt());
        }

        @Test
        @DisplayName("an answer is saved, and choosing again replaces it")
        void savesAndReplacesAnswer() {
            Attempt attempt = attemptStartedAt(T0);
            when(attemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(attempt));
            AttemptService service = serviceAt(T0.plusMinutes(1));

            service.submitAnswer(ATTEMPT_ID, STUDENT_ID, QUESTION_ID, WRONG_OPTION);
            service.submitAnswer(ATTEMPT_ID, STUDENT_ID, QUESTION_ID, RIGHT_OPTION);

            assertEquals(Map.of(QUESTION_ID, RIGHT_OPTION), attempt.getSelectedOptionIds());
        }

        @Test
        @DisplayName("R17 after expiry the attempt keeps saved answers and later answers are rejected")
        void answerAfterExpiry() {
            Attempt attempt = attemptStartedAt(T0);
            attempt.recordAnswer(quiz.getQuestions().get(0), quiz.getQuestions().get(0).getOptions().get(0));
            when(attemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(attempt));

            assertEquals(Messages.TIME_EXPIRED, messageOf(() -> serviceAt(T0.plusMinutes(DURATION + 1))
                    .submitAnswer(ATTEMPT_ID, STUDENT_ID, QUESTION_ID, WRONG_OPTION)));
            assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
            assertEquals(Map.of(QUESTION_ID, RIGHT_OPTION), attempt.getSelectedOptionIds());
        }

        @Test
        @DisplayName("an option from another question is rejected")
        void invalidOption() {
            Attempt attempt = attemptStartedAt(T0);
            when(attemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(attempt));

            assertEquals(Messages.INVALID_ANSWER, messageOf(() -> serviceAt(T0)
                    .submitAnswer(ATTEMPT_ID, STUDENT_ID, QUESTION_ID, 9999L)));
        }

        @Test
        @DisplayName("submitting finishes the attempt at the current time")
        void submit() {
            Attempt attempt = attemptStartedAt(T0);
            when(attemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(attempt));

            serviceAt(T0.plusMinutes(10)).submit(ATTEMPT_ID, STUDENT_ID);

            assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
            assertEquals(T0.plusMinutes(10), attempt.getSubmittedAt());
        }

        @Test
        @DisplayName("another student's attempt is reported as not found")
        void otherStudent() {
            when(attemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(attemptStartedAt(T0)));

            assertEquals(Messages.ATTEMPT_NOT_FOUND, messageOf(() -> serviceAt(T0).getAttempt(ATTEMPT_ID, 999L)));
        }
    }
}
