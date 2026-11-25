package com.quizapp.whitebox;

import com.quizapp.dto.QuizAvailability;
import com.quizapp.model.Attempt;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R15 white-box tests. Functions under test: the limit check of {@code AttemptService.start}
 * ({@code countByQuizIdAndStudentId(...) >= quiz.getMaxAttempts()}) and {@code QuizAvailability.attemptsLeft}.
 * Quiz 10 allows 2 attempts.
 */
@DisplayName("R15 - Attempt limit (white-box)")
class R15AttemptLimitWhiteBoxTest extends AttemptWhiteBoxSupport {

    private String start() {
        return ActualOutput.of(() -> "attempt " + attemptService.start(QUIZ_ID, STUDENT_ID).getId() + " started");
    }

    @Nested
    @DisplayName("Table 15-3: Statement coverage")
    class StatementCoverage {

        @Test
        void throwNoAttemptsLeft(TestCase testCase) {
            attemptsUsed(2);
            String actual = start();
            String expected = "No attempts remaining";
            testCase.record("TC-R15-SC-01", "start(): statement 'throw NO_ATTEMPTS_LEFT'",
                    "max 2 attempts, 2 used, no open attempt", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void attemptsLeftStatement(TestCase testCase) {
            String actual = "left " + new QuizAvailability(quiz, 1, null).attemptsLeft();
            testCase.record("TC-R15-SC-02", "QuizAvailability.attemptsLeft(): return statement",
                    "max 2 attempts, 1 used", "left 1", actual);
            assertEquals("left 1", actual);
        }
    }

    @Nested
    @DisplayName("Table 15-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R15-BC-01 | start: attempts used >= max -> TRUE (2 of 2)  | 2 | No attempts remaining
                TC-R15-BC-02 | start: attempts used >= max -> FALSE (1 of 2) | 1 | attempt 50 started
                """)
        void limitBranches(String id, String condition, long used, String expected, TestCase testCase) {
            attemptsUsed(used);
            String actual = start();
            testCase.record(id, condition, "max 2 attempts, " + used + " used, no open attempt", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void expiredOpenAttemptCounts(TestCase testCase) {
            Attempt open = openAttemptStartedAt(T0);
            attemptsUsed(2);
            clockAt(31, 0);
            String actual = start() + "; open attempt is now " + open.getStatus();
            String expected = "No attempts remaining; open attempt is now SUBMITTED";
            testCase.record("TC-R15-BC-03",
                    "open attempt expired (!checkExpiry -> FALSE), then attempts used >= max -> TRUE",
                    "10:31, max 2, 2 used, the 2nd attempt was started at 10:00 and never submitted", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
