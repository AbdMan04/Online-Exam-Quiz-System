package com.quizapp.whitebox;

import com.quizapp.model.Attempt;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R17 white-box tests. Functions under test: {@code AttemptService.checkExpiry}, {@code submitAnswer},
 * {@code submit}, {@code getAttempt} / {@code findOwnedAttempt}; {@code Attempt.isExpiredAt},
 * {@code recordAnswer}, {@code getSelectedOptionIds}; {@code Quiz.findQuestion}, {@code Question.findOption};
 * and the attempt page, answer and submit handlers of {@code StudentController}.
 *
 * <p>Attempt 50 starts at 10:00 on the 30-minute quiz, so the deadline is 10:30:00.
 * Question 101 has options 1010 (correct) and 1011; question 102 has 1020 (correct) and 1021.
 */
@DisplayName("R17 - Timer expiry (white-box)")
class R17TimerExpiryWhiteBoxTest extends AttemptWhiteBoxSupport {

    private static String state(Attempt attempt) {
        return attempt.getStatus() + (attempt.isInProgress() ? "" : " at " + time(attempt.getSubmittedAt()))
                + ", answers " + attempt.getSelectedOptionIds();
    }

    /** Alice (or another student) answers; reports the message and the attempt's state. */
    private String answer(Attempt attempt, Long studentId, Long questionId, Long optionId) {
        String message = ActualOutput.of(() -> {
            attemptService.submitAnswer(ATTEMPT_ID, studentId, questionId, optionId);
            return "saved";
        });
        return message + " [" + state(attempt) + "]";
    }

    @Nested
    @DisplayName("Table 17-3: Statement coverage")
    class StatementCoverage {

        @Test
        void saveAnswer(TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            clockAt(5, 0);
            String actual = answer(attempt, STUDENT_ID, 101L, 1010L);
            String expected = "saved [IN_PROGRESS, answers {101=1010}]";
            testCase.record("TC-R17-SC-01",
                    "submitAnswer(): find attempt, checkExpiry, findQuestion, findOption, recordAnswer (new answer), save",
                    "10:05, question 101, option 1010", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void answerAfterSubmit(TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            attempt.submit(T0.plusMinutes(5));
            clockAt(6, 0);
            String actual = answer(attempt, STUDENT_ID, 101L, 1010L);
            String expected = "Time is up - attempt already submitted [SUBMITTED at 10:05, answers {}]";
            testCase.record("TC-R17-SC-02", "submitAnswer(): statement 'throw TIME_EXPIRED'",
                    "10:06, attempt submitted at 10:05", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-SC-03 | submitAnswer(): findQuestion orElseThrow lambda 'throw INVALID_ANSWER' | 999 | 1010 | Invalid answer [IN_PROGRESS, answers {}]
                TC-R17-SC-04 | submitAnswer(): findOption orElseThrow lambda 'throw INVALID_ANSWER'   | 101 | 1020 | Invalid answer [IN_PROGRESS, answers {}]
                """)
        void invalidAnswer(String id, String condition, Long questionId, Long optionId, String expected,
                           TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            String actual = answer(attempt, STUDENT_ID, questionId, optionId);
            testCase.record(id, condition, "question " + questionId + ", option " + optionId, expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void autoSubmit(TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            clockAt(45, 0);
            String actual = attemptService.checkExpiry(attempt) + " [" + state(attempt) + "]";
            String expected = "true [SUBMITTED at 10:30, answers {}]";
            testCase.record("TC-R17-SC-05", "checkExpiry(): auto-submit at the deadline, save, return true",
                    "10:45, attempt started 10:00 (deadline 10:30)", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void manualSubmit(TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            clockAt(5, 0);
            attemptService.submit(ATTEMPT_ID, STUDENT_ID);
            String actual = state(attempt);
            String expected = "SUBMITTED at 10:05, answers {}";
            testCase.record("TC-R17-SC-06", "submit(): 'attempt.submit(now)' and save statements",
                    "10:05, Alice presses Submit", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void otherStudentsAttempt(TestCase testCase) {
            attemptStartedAt(T0);
            String actual = ActualOutput.of(() -> state(attemptService.getAttempt(ATTEMPT_ID, OTHER_STUDENT_ID)));
            String expected = "Attempt not found";
            testCase.record("TC-R17-SC-07", "getAttempt() / findOwnedAttempt(): orElseThrow lambda",
                    "student 6 opens Alice's attempt 50", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-SC-08 | StudentController.saveAnswer(): try-block   | 5  | HTTP 200 {status=saved}
                TC-R17-SC-09 | StudentController.saveAnswer(): catch-block | 40 | HTTP 409 {error=Time is up - attempt already submitted}
                """)
        void controllerSaveAnswer(String id, String condition, int minute, String expected, TestCase testCase) {
            attemptStartedAt(T0);
            clockAt(minute, 0);
            ResponseEntity<Map<String, String>> response =
                    studentController().saveAnswer(STUDENT_SESSION, ATTEMPT_ID, 101L, 1010L);
            String actual = "HTTP " + response.getStatusCode().value() + " " + response.getBody();
            testCase.record(id, condition, String.format("10:%02d, POST answer question 101 = option 1010", minute),
                    expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerSubmit(TestCase testCase) {
            attemptStartedAt(T0);
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
            String actual = "view=" + studentController().submit(STUDENT_SESSION, ATTEMPT_ID, redirect)
                    + ", flash message=" + redirect.getFlashAttributes().get("message");
            String expected = "view=redirect:/student/attempts/50, flash message=Attempt submitted";
            testCase.record("TC-R17-SC-10", "StudentController.submit()", "POST /student/attempts/50/submit",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 17-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-BC-01 | checkExpiry: isInProgress() -> FALSE (already submitted at 10:05) | true  | 40 | 0  | false [SUBMITTED at 10:05, answers {}]
                TC-R17-BC-02 | isInProgress() -> TRUE, isExpiredAt(10:29:59) -> FALSE            | false | 29 | 59 | false [IN_PROGRESS, answers {}]
                TC-R17-BC-03 | isInProgress() -> TRUE, isExpiredAt(10:30:00) -> TRUE             | false | 30 | 0  | true [SUBMITTED at 10:30, answers {}]
                """)
        void checkExpiryBranches(String id, String condition, boolean submittedEarlier, int minute, int second,
                                 String expected, TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            if (submittedEarlier) {
                attempt.submit(T0.plusMinutes(5));
            }
            clockAt(minute, second);
            String actual = attemptService.checkExpiry(attempt) + " [" + state(attempt) + "]";
            testCase.record(id, condition, String.format("10:%02d:%02d, started 10:00, deadline 10:30:00", minute, second),
                    expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-BC-04 | submitAnswer: !isInProgress() -> TRUE (time ran out at 10:30)      | 31 | 1011 | Time is up - attempt already submitted [SUBMITTED at 10:30, answers {101=1010}]
                TC-R17-BC-05 | submitAnswer: !isInProgress() -> FALSE; recordAnswer replaces the earlier choice | 20 | 1011 | saved [IN_PROGRESS, answers {101=1011}]
                """)
        void submitAnswerBranches(String id, String condition, int minute, Long newOption, String expected,
                                  TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            attempt.recordAnswer(question1, question1.getOptions().get(0)); // 10:01 answer: option 1010
            clockAt(minute, 0);
            String actual = answer(attempt, STUDENT_ID, 101L, newOption);
            testCase.record(id, condition, String.format("question 101 answered 1010 earlier; at 10:%02d answer %d",
                    minute, newOption), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-BC-06 | submit: !checkExpiry -> TRUE, isInProgress() -> TRUE (submitted now)          | false | 10 | SUBMITTED at 10:10, answers {}
                TC-R17-BC-07 | submit: !checkExpiry -> TRUE, isInProgress() -> FALSE (already submitted)    | true  | 10 | SUBMITTED at 10:05, answers {}
                TC-R17-BC-08 | submit: !checkExpiry -> FALSE (expired; submitted at the deadline, not now) | false | 50 | SUBMITTED at 10:30, answers {}
                """)
        void submitBranches(String id, String condition, boolean submittedEarlier, int minute, String expected,
                            TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            if (submittedEarlier) {
                attempt.submit(T0.plusMinutes(5));
            }
            clockAt(minute, 0);
            attemptService.submit(ATTEMPT_ID, STUDENT_ID);
            String actual = state(attempt);
            testCase.record(id, condition, String.format("10:%02d, press Submit; submitted earlier at 10:05: %s",
                    minute, submittedEarlier ? "yes" : "no"), expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void ownerCheck(TestCase testCase) {
            Attempt attempt = attemptStartedAt(T0);
            String actual = answer(attempt, OTHER_STUDENT_ID, 101L, 1010L);
            String expected = "Attempt not found [IN_PROGRESS, answers {}]";
            testCase.record("TC-R17-BC-09", "findOwnedAttempt: student id equals 6 -> FALSE",
                    "student 6 answers in Alice's attempt 50", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void attemptPageWhileRunning(TestCase testCase) {
            attemptStartedAt(T0);
            clockAt(5, 0);
            ExtendedModelMap model = new ExtendedModelMap();
            String view = studentController().attempt(STUDENT_SESSION, ATTEMPT_ID, model);
            String actual = "view=" + view + ", remaining " + model.get("remainingSeconds") + " s, result shown="
                    + model.containsAttribute("result");
            String expected = "view=student/attempt, remaining 1500 s, result shown=false";
            testCase.record("TC-R17-BC-10", "StudentController.attempt: !isInProgress() -> FALSE (no result yet)",
                    "10:05, GET /student/attempts/50 (running)", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
