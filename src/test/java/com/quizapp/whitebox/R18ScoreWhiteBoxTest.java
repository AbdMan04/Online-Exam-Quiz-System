package com.quizapp.whitebox;

import com.quizapp.dto.GradeResult;
import com.quizapp.model.Attempt;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R18 white-box tests. Functions under test: {@code GradingService.calculateScore} and {@code grade},
 * {@code Attempt.getEarnedPoints}, {@code Quiz.getTotalPoints}, and the result branch of
 * {@code StudentController.attempt}. Quiz 10 has two 5-point questions (10 points in total).
 */
@DisplayName("R18 - Score calculation (white-box)")
class R18ScoreWhiteBoxTest extends AttemptWhiteBoxSupport {

    private String calculate(int earned, int total) {
        try {
            return gradingService.calculateScore(earned, total) + " %";
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException: " + e.getMessage();
        }
    }

    /** Alice's submitted attempt 50 with question 1 answered right and question 2 answered wrong (5 of 10). */
    private Attempt submittedHalfRight() {
        Attempt attempt = attemptStartedAt(T0);
        attempt.recordAnswer(question1, question1.getOptions().get(0));
        attempt.recordAnswer(question2, question2.getOptions().get(1));
        attempt.submit(T0.plusMinutes(10));
        return attempt;
    }

    @Nested
    @DisplayName("Table 18-3: Statement coverage")
    class StatementCoverage {

        @Test
        void gradeAttempt(TestCase testCase) {
            Attempt attempt = submittedHalfRight();
            GradeResult result = gradingService.grade(attempt);
            String actual = "earned " + attempt.getEarnedPoints() + " of " + quiz.getTotalPoints() + " -> "
                    + result.score() + " %, " + result.outcome() + ", " + result.letterGrade();
            String expected = "earned 5 of 10 -> 50.00 %, FAIL, F";
            testCase.record("TC-R18-SC-01",
                    "grade(): getEarnedPoints(), getTotalPoints(), calculateScore() return, passFail(), letterGrade()",
                    "question 1 right (5 pt), question 2 wrong (5 pt), passing 60", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R18-SC-02 | calculateScore(): statement 'return earned x 100 / total' (Gherkin example) | 15 | 20 | 75.00 %
                TC-R18-SC-03 | calculateScore(): first 'throw' (total points not positive)                | 5  | 0  | IllegalArgumentException: Total points must be positive
                TC-R18-SC-04 | calculateScore(): second 'throw' (earned points out of range)              | 25 | 20 | IllegalArgumentException: Earned points must be between 0 and the total points
                """)
        void calculateStatements(String id, String condition, int earned, int total, String expected,
                                 TestCase testCase) {
            String actual = calculate(earned, total);
            testCase.record(id, condition, "earned=" + earned + ", total=" + total, expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void resultPage(TestCase testCase) {
            submittedHalfRight();
            ExtendedModelMap model = new ExtendedModelMap();
            String view = studentController().attempt(STUDENT_SESSION, ATTEMPT_ID, model);
            GradeResult result = (GradeResult) model.get("result");
            String actual = "view=" + view + ", result=" + result.score() + " % " + result.outcome() + " "
                    + result.letterGrade();
            String expected = "view=student/attempt, result=50.00 % FAIL F";
            testCase.record("TC-R18-SC-05", "StudentController.attempt(): statement that adds the graded result",
                    "GET /student/attempts/50 (submitted, 5 of 10 points)", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 18-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R18-BC-01 | calculateScore: totalPoints <= 0 -> TRUE                              | 5  | 0  | IllegalArgumentException: Total points must be positive
                TC-R18-BC-02 | totalPoints <= 0 -> FALSE, earned < 0 -> TRUE                         | -1 | 20 | IllegalArgumentException: Earned points must be between 0 and the total points
                TC-R18-BC-03 | earned < 0 -> FALSE, earned > total -> TRUE                           | 21 | 20 | IllegalArgumentException: Earned points must be between 0 and the total points
                TC-R18-BC-04 | earned < 0 -> FALSE, earned > total -> FALSE (score calculated)        | 20 | 20 | 100.00 %
                """)
        void calculateBranches(String id, String condition, int earned, int total, String expected,
                               TestCase testCase) {
            String actual = calculate(earned, total);
            testCase.record(id, condition, "earned=" + earned + ", total=" + total, expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void resultBranch(TestCase testCase) {
            submittedHalfRight();
            ExtendedModelMap model = new ExtendedModelMap();
            studentController().attempt(STUDENT_SESSION, ATTEMPT_ID, model);
            String actual = "result shown=" + model.containsAttribute("result");
            testCase.record("TC-R18-BC-05", "StudentController.attempt: !isInProgress() -> TRUE (submitted)",
                    "GET /student/attempts/50 (submitted)", "result shown=true", actual);
            assertEquals("result shown=true", actual);
        }
    }
}
