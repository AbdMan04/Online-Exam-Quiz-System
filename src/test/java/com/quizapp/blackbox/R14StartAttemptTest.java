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
 * R14: Start attempt.
 * Given a PUBLISHED quiz and a student with attempts used less than max attempts, when the student starts
 * the quiz, then a new attempt is created and the timer starts.
 */
@DisplayName("R14 - Start attempt")
class R14StartAttemptTest extends AttemptSupport {

    @Nested
    @DisplayName("Table 14-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R14-EP-01 | PUBLISHED quiz, no attempts used yet        | 0 | Attempt started, 1800 s left
                TC-R14-EP-02 | PUBLISHED quiz, some attempts used (1 of 2) | 1 | Attempt started, 1800 s left
                """)
        void newAttempt(String id, String condition, int used, String expected, TestCase testCase) {
            Quiz quiz = publishedQuiz(30, 60, 2, 5);
            Long student = newStudent();
            useAttempts(quiz.getId(), student, used);

            String actual = start(quiz.getId(), student, null);
            testCase.record(id, condition, "30-minute quiz, max attempts 2, " + used + " used, press Start",
                    expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R14-EP-03 | An attempt is already running (started 10 min ago) | Same attempt resumed, 1200 s left
                """)
        void resumeOpenAttempt(String id, String condition, String expected, TestCase testCase) {
            Quiz quiz = publishedQuiz(30, 60, 2, 5);
            Long student = newStudent();
            Attempt open = attemptService.start(quiz.getId(), student);
            clock.advance(Duration.ofMinutes(10));

            String actual = start(quiz.getId(), student, open.getId());
            testCase.record(id, condition, "30-minute quiz, attempt started at 10:00, press Start again at 10:10",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 14-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R14-BVA-01 | Duration = min (5): timer starts at 5 minutes     | 5   | 2 | 0 | Attempt started, 300 s left
                TC-R14-BVA-02 | Duration = max (180): timer starts at 180 minutes | 180 | 2 | 0 | Attempt started, 10800 s left
                TC-R14-BVA-03 | Attempts used = max - 1 (1 of 2)                  | 30  | 2 | 1 | Attempt started, 1800 s left
                TC-R14-BVA-04 | Attempts used = max - 1 (4 of 5)                  | 30  | 5 | 4 | Attempt started, 1800 s left
                """)
        void boundaries(String id, String condition, int duration, int maxAttempts, int used, String expected,
                        TestCase testCase) {
            Quiz quiz = publishedQuiz(duration, 60, maxAttempts, 5);
            Long student = newStudent();
            useAttempts(quiz.getId(), student, used);

            String actual = start(quiz.getId(), student, null);
            testCase.record(id, condition, duration + "-minute quiz, max attempts " + maxAttempts + ", " + used
                    + " used, press Start", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
