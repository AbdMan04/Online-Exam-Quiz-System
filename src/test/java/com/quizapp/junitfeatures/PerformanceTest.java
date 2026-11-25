package com.quizapp.junitfeatures;

import com.quizapp.dto.QuizReport;
import com.quizapp.model.AnswerOption;
import com.quizapp.model.Attempt;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.service.AttemptService;
import com.quizapp.service.GradingService;
import com.quizapp.service.QuizService;
import com.quizapp.service.ReportService;
import com.quizapp.validation.PasswordValidator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Response-time checks for the slowest operations: password hashing (R1/R4), validating many passwords (R2)
 * and building a large class report (R20).
 *
 * <p>JUnit features: {@code @Timeout}, {@code assertTimeout}, {@code assertTimeoutPreemptively},
 * {@code @RegisterExtension} (a configured {@link TimingExtension}) and {@code @Disabled}.
 */
@Tag("performance")
@DisplayName("Performance")
class PerformanceTest {

    @RegisterExtension
    static final TimingExtension TIMING = new TimingExtension(Duration.ofMillis(500));

    @Test
    @DisplayName("hashing a password at production strength takes less than 2 seconds")
    void passwordHashingIsFastEnough() {
        String hash = assertTimeout(Duration.ofSeconds(2), () -> new BCryptPasswordEncoder().encode("Secure123"));
        assertTrue(hash.startsWith("$2"), "BCrypt hash");
    }

    @Test
    @DisplayName("validating 100 000 passwords finishes within 2 seconds (stopped early if it hangs)")
    void passwordValidationScales() {
        int valid = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            int count = 0;
            for (int i = 0; i < 100_000; i++) {
                count += PasswordValidator.isValid("Secure1" + i) ? 1 : 0; // 8-12 characters, letters + digits
            }
            return count;
        });
        assertEquals(100_000, valid);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @DisplayName("a report over 100 students x 3 attempts is built within 3 seconds")
    void largeReportIsFastEnough() {
        User instructor = new User("Dr. Smith", "smith@uni.edu", "hash", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(instructor, "id", 1L);
        Question question = new Question("Q", 10, List.of(new AnswerOption("right", true), new AnswerOption("wrong", false)));
        Quiz quiz = new Quiz("Big class", 30, 60, 3, instructor);
        quiz.addQuestion(question);
        quiz.publish();

        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 10, 0);
        List<Attempt> attempts = new ArrayList<>();
        for (long s = 1; s <= 100; s++) {
            User student = new User("Student " + s, "s" + s + "@uni.edu", "hash", Role.STUDENT);
            ReflectionTestUtils.setField(student, "id", 100 + s);
            for (int a = 0; a < 3; a++) {
                Attempt attempt = new Attempt(quiz, student, start);
                attempt.recordAnswer(question, question.getOptions().get((int) ((s + a) % 2)));
                attempt.submit(start.plusMinutes(10));
                attempts.add(attempt);
            }
        }

        QuizService quizService = mock(QuizService.class);
        AttemptRepository attemptRepository = mock(AttemptRepository.class);
        when(quizService.getQuiz(anyLong(), any())).thenReturn(quiz);
        when(attemptRepository.findByQuizIdOrderByStartedAtAsc(any())).thenReturn(attempts);
        AttemptService attemptService = new AttemptService(attemptRepository, null, null, Clock.systemUTC());
        ReportService reportService = new ReportService(quizService, attemptService, new GradingService(), attemptRepository);

        QuizReport report = reportService.quizReport(10L, 1L);

        assertEquals(100, report.students().size());
        assertEquals(100, report.passedCount(), "every student answered right in at least one of 3 attempts");
    }

    @Test
    @Disabled("Waits 30 real minutes for a quiz timer to run out. R17 is tested at exact seconds with "
            + "MutableClock instead (R17TimerExpiryTest, R17TimerExpiryWhiteBoxTest).")
    @DisplayName("real-time timer expiry (replaced by clock-controlled tests)")
    void realTimeTimerExpiry() throws InterruptedException {
        TimeUnit.MINUTES.sleep(30);
    }
}
