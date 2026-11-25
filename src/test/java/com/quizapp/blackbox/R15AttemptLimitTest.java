package com.quizapp.blackbox;

import com.quizapp.model.Attempt;
import com.quizapp.model.Quiz;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R15: Attempt limit.
 * Given a student has used all allowed attempts, when the student tries to start again,
 * then the system rejects it with "No attempts remaining".
 */
@DisplayName("R15 - Attempt limit")
class R15AttemptLimitTest extends AttemptSupport {

    @Nested
    @DisplayName("Table 15-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R15-EP-01 | Attempts used below the limit (0 of 1)  | 0 | Attempt started, 1800 s left
                TC-R15-EP-02 | All attempts used and submitted (1 of 1) | 1 | No attempts remaining
                """)
        void submittedAttempts(String id, String condition, int used, String expected, TestCase testCase) {
            Quiz quiz = publishedQuiz(30, 60, 1, 5);
            Long student = newStudent();
            useAttempts(quiz.getId(), student, used);

            String actual = start(quiz.getId(), student, null);
            testCase.record(id, condition, "max attempts 1, " + used + " submitted, press Start", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R15-EP-03 | Last allowed attempt still running (time left)    | 10 | Same attempt resumed, 1200 s left
                TC-R15-EP-04 | Last allowed attempt ran out of time (not submitted) | 31 | No attempts remaining
                """)
        void lastAttemptOpen(String id, String condition, int minutesLater, String expected, TestCase testCase) {
            Quiz quiz = publishedQuiz(30, 60, 1, 5);
            Long student = newStudent();
            Attempt open = attemptService.start(quiz.getId(), student);
            clock.advance(Duration.ofMinutes(minutesLater));

            String actual = start(quiz.getId(), student, open.getId());
            testCase.record(id, condition, "max attempts 1, the only attempt started " + minutesLater
                    + " min ago (30-minute quiz), press Start", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 15-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R15-BVA-01 | Attempts used = max - 1 (max 1)  | 1 | 0 | Attempt started, 1800 s left
                TC-R15-BVA-02 | Attempts used = max (max 1)      | 1 | 1 | No attempts remaining
                TC-R15-BVA-03 | Attempts used = max - 1 (max 5)  | 5 | 4 | Attempt started, 1800 s left
                TC-R15-BVA-04 | Attempts used = max (max 5)      | 5 | 5 | No attempts remaining
                """)
        void boundaries(String id, String condition, int maxAttempts, int used, String expected, TestCase testCase) {
            Quiz quiz = publishedQuiz(30, 60, maxAttempts, 5);
            Long student = newStudent();
            useAttempts(quiz.getId(), student, used);

            String actual = start(quiz.getId(), student, null);
            testCase.record(id, condition, "max attempts " + maxAttempts + ", " + used + " used, press Start",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }
}
