package com.quizapp.whitebox;

import com.quizapp.service.GradingService;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R19 white-box tests. Functions under test: {@code GradingService.passFail}, {@code letterGrade} and
 * {@code requireValidScore} ({@code score == null || score < 0 || score > 100}).
 * Scores are exact decimals, so the sub-point boundaries 59.99 / 60.00 etc. are tested here.
 */
@WhiteBoxTest
@DisplayName("R19 - Pass/fail and letter grade (white-box)")
class R19PassFailGradeWhiteBoxTest {

    private final GradingService grading = new GradingService();

    private static BigDecimal score(String text) {
        return text.equals("NULL") ? null : new BigDecimal(text);
    }

    private static String run(Supplier<Object> action) {
        try {
            return String.valueOf(action.get());
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException: " + e.getMessage();
        }
    }

    @Nested
    @DisplayName("Table 19-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R19-SC-01 | letterGrade(): statement 'return A' | 95   | A
                TC-R19-SC-02 | letterGrade(): statement 'return B' | 85   | B
                TC-R19-SC-03 | letterGrade(): statement 'return C' | 75   | C
                TC-R19-SC-04 | letterGrade(): statement 'return D' | 65   | D
                TC-R19-SC-05 | letterGrade(): statement 'return F' | 40   | F
                TC-R19-SC-06 | requireValidScore(): statement 'throw' | NULL | IllegalArgumentException: Score must be between 0 and 100
                """)
        void letterGradeStatements(String id, String condition, String scoreText, String expected, TestCase testCase) {
            String actual = run(() -> grading.letterGrade(score(scoreText)));
            testCase.record(id, condition, "letterGrade(" + scoreText + ")", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R19-SC-07 | passFail(): statement 'return score >= passing ? PASS : FAIL' | 75 | 60 | PASS
                """)
        void passFailStatement(String id, String condition, String scoreText, int passing, String expected,
                               TestCase testCase) {
            String actual = run(() -> grading.passFail(score(scoreText), passing));
            testCase.record(id, condition, "passFail(" + scoreText + ", " + passing + ")", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 19-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R19-BC-01 | requireValidScore: score == null -> TRUE                      | NULL   | IllegalArgumentException: Score must be between 0 and 100
                TC-R19-BC-02 | score == null -> FALSE, score < 0 -> TRUE                     | -0.01  | IllegalArgumentException: Score must be between 0 and 100
                TC-R19-BC-03 | score < 0 -> FALSE, score > 100 -> TRUE                       | 100.01 | IllegalArgumentException: Score must be between 0 and 100
                TC-R19-BC-04 | valid score; letterGrade: score >= 90 -> TRUE                 | 90.00  | A
                TC-R19-BC-05 | score >= 90 -> FALSE, score >= 80 -> TRUE                     | 89.99  | B
                TC-R19-BC-06 | score >= 80 -> FALSE, score >= 70 -> TRUE                     | 79.99  | C
                TC-R19-BC-07 | score >= 70 -> FALSE, score >= 60 -> TRUE                     | 69.99  | D
                TC-R19-BC-08 | score >= 60 -> FALSE                                          | 59.99  | F
                """)
        void letterGradeBranches(String id, String condition, String scoreText, String expected, TestCase testCase) {
            String actual = run(() -> grading.letterGrade(score(scoreText)));
            testCase.record(id, condition, "letterGrade(" + scoreText + ")", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R19-BC-09 | passFail: score >= passing -> TRUE (exactly at passing)  | 60.00 | 60 | PASS
                TC-R19-BC-10 | passFail: score >= passing -> FALSE (0.01 below)         | 59.99 | 60 | FAIL
                """)
        void passFailBranches(String id, String condition, String scoreText, int passing, String expected,
                              TestCase testCase) {
            String actual = run(() -> grading.passFail(score(scoreText), passing));
            testCase.record(id, condition, "passFail(" + scoreText + ", " + passing + ")", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
