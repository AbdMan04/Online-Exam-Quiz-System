package com.quizapp.whitebox;

import com.quizapp.dto.QuizAvailability;
import com.quizapp.model.Attempt;
import com.quizapp.model.QuizStatus;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * R14 white-box tests. Functions under test: {@code AttemptService.start} (new attempt and resume paths),
 * {@code listAvailableQuizzes}, {@code remainingSeconds}, {@code Attempt.getDeadline} /
 * {@code remainingSecondsAt}, {@code QuizAvailability.canStart}, and the dashboard / start handlers of
 * {@code StudentController}.
 */
@DisplayName("R14 - Start attempt (white-box)")
class R14StartAttemptWhiteBoxTest extends AttemptWhiteBoxSupport {

    /** Alice presses Start; reports which attempt she gets and its timer. */
    private String start() {
        return ActualOutput.of(() -> {
            Attempt attempt = attemptService.start(QUIZ_ID, STUDENT_ID);
            return "attempt " + attempt.getId() + " started " + time(attempt.getStartedAt()) + ", deadline "
                    + time(attempt.getDeadline()) + ", " + attemptService.remainingSeconds(attempt) + " s left";
        });
    }

    @Nested
    @DisplayName("Table 14-3: Statement coverage")
    class StatementCoverage {

        @Test
        void newAttempt(TestCase testCase) {
            String actual = start();
            String expected = "attempt 50 started 10:00, deadline 10:30, 1800 s left";
            testCase.record("TC-R14-SC-01", "start(): find quiz, no open attempt, limit check, find student, create and save",
                    "10:00, quiz 10 (PUBLISHED, 30 min, max 2), 0 attempts used", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void resumeAttempt(TestCase testCase) {
            openAttemptStartedAt(T0);
            clockAt(10, 0);
            String actual = start();
            String expected = "attempt 50 started 10:00, deadline 10:30, 1200 s left";
            testCase.record("TC-R14-SC-02", "start(): statement 'return open.get()' (resume)",
                    "10:10, open attempt 50 started at 10:00", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void unknownStudent(TestCase testCase) {
            String actual;
            try {
                attemptService.start(QUIZ_ID, MISSING_ID);
                actual = "started";
            } catch (IllegalStateException e) {
                actual = "IllegalStateException: " + e.getMessage();
            }
            String expected = "IllegalStateException: Unknown student 999";
            testCase.record("TC-R14-SC-03", "start(): orElseThrow lambda for an unknown student id (programming error)",
                    "student 999 (not in database)", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void dashboard(TestCase testCase) {
            when(quizRepository.findByStatusOrderByIdDesc(QuizStatus.PUBLISHED)).thenReturn(List.of(quiz));
            attemptsUsed(1);
            openAttemptStartedAt(T0);
            ExtendedModelMap model = new ExtendedModelMap();
            String view = studentController().dashboard(studentController().currentUser(STUDENT_SESSION), model);
            QuizAvailability row = ((List<?>) model.get("quizzes")).stream().map(QuizAvailability.class::cast)
                    .findFirst().orElseThrow();
            String actual = "view=" + view + ", " + row.quiz().getTitle() + ": used " + row.attemptsUsed()
                    + ", open attempt " + row.openAttemptId() + ", left " + row.attemptsLeft();
            String expected = "view=student/dashboard, Java Basics: used 1, open attempt 50, left 1";
            testCase.record("TC-R14-SC-04",
                    "StudentController.currentUser() + dashboard(); listAvailableQuizzes(); QuizAvailability.attemptsLeft()",
                    "Alice used 1 of 2 attempts, attempt 50 is open", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerStart(TestCase testCase) {
            String actual = "view=" + studentController().start(STUDENT_SESSION, QUIZ_ID);
            String expected = "view=redirect:/student/attempts/50";
            testCase.record("TC-R14-SC-05", "StudentController.start(), attemptPage()",
                    "POST /student/quizzes/10/start", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 14-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R14-BC-01 | start: open.isPresent() -> FALSE                                       | false | 0  | attempt 50 started 10:00, deadline 10:30, 1800 s left
                TC-R14-BC-02 | open.isPresent() -> TRUE, !checkExpiry -> TRUE (time left, resume)     | true  | 10 | attempt 49 started 10:00, deadline 10:30, 1200 s left
                TC-R14-BC-03 | open.isPresent() -> TRUE, !checkExpiry -> FALSE (expired, new attempt) | true  | 31 | attempt 50 started 10:31, deadline 11:01, 1800 s left
                """)
        void startBranches(String id, String condition, boolean openAttempt, int minutesLater, String expected,
                           TestCase testCase) {
            if (openAttempt) {
                Fixtures.withId(openAttemptStartedAt(T0), 49L); // the open attempt is #49; a new one gets #50
                attemptsUsed(1);
            }
            clockAt(minutesLater, 0);
            String actual = start();
            testCase.record(id, condition, String.format("10:%02d, open attempt 49 started 10:00: %s, 30-minute quiz",
                    minutesLater, openAttempt ? "yes" : "no"), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NONE", textBlock = """
                TC-R14-BC-04 | canStart: openAttemptId != null -> TRUE (Resume button)                | 2 | 50   | true (left 0)
                TC-R14-BC-05 | openAttemptId != null -> FALSE, attemptsLeft() > 0 -> TRUE (Start)    | 1 | NONE | true (left 1)
                TC-R14-BC-06 | openAttemptId != null -> FALSE, attemptsLeft() > 0 -> FALSE (none left) | 2 | NONE | false (left 0)
                """)
        void canStartBranches(String id, String condition, long used, Long openAttemptId, String expected,
                              TestCase testCase) {
            QuizAvailability row = new QuizAvailability(quiz, used, openAttemptId);
            String actual = row.canStart() + " (left " + row.attemptsLeft() + ")";
            testCase.record(id, condition, "max 2 attempts, used " + used + ", open attempt " + openAttemptId,
                    expected, actual);
            assertEquals(expected, actual);
        }
    }
}
