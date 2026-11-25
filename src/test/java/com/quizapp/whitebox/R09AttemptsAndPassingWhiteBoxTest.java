package com.quizapp.whitebox;

import com.quizapp.dto.QuizInput;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.validation.QuizValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R9 white-box tests. Functions under test: the passing-score and max-attempts decisions of
 * {@code QuizValidator.validate}, each through {@code ValidationRules.inRange}.
 */
@WhiteBoxTest
@DisplayName("R9 - Attempts and passing score validation (white-box)")
class R09AttemptsAndPassingWhiteBoxTest {

    private static String validate(Integer passing, Integer attempts) {
        return ActualOutput.of(() -> {
            QuizValidator.validate(new QuizInput("Java Basics", 30, passing, attempts));
            return "valid";
        });
    }

    @Nested
    @DisplayName("Table 9-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R09-SC-01 | validate(): statement 'throw INVALID_PASSING_SCORE'   | -1 | 2 | Passing score must be 0-100
                TC-R09-SC-02 | validate(): statement 'throw INVALID_ATTEMPTS'        | 60 | 0 | Max attempts must be 1-5
                TC-R09-SC-03 | validate(): both checks pass, method returns normally | 60 | 2 | valid
                """)
        void statements(String id, String condition, Integer passing, Integer attempts, String expected,
                        TestCase testCase) {
            String actual = validate(passing, attempts);
            testCase.record(id, condition, "passing=" + passing + ", attempts=" + attempts, expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 9-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R09-BC-01 | passing: value != null -> FALSE                     | NULL | 2    | Passing score must be 0-100
                TC-R09-BC-02 | passing: != null -> TRUE, >= 0 -> FALSE             | -1   | 2    | Passing score must be 0-100
                TC-R09-BC-03 | passing: >= 0 -> TRUE, <= 100 -> FALSE              | 101  | 2    | Passing score must be 0-100
                TC-R09-BC-04 | passing: in range -> TRUE; attempts != null -> FALSE | 0   | NULL | Max attempts must be 1-5
                TC-R09-BC-05 | attempts: != null -> TRUE, >= 1 -> FALSE            | 100  | 0    | Max attempts must be 1-5
                TC-R09-BC-06 | attempts: >= 1 -> TRUE, <= 5 -> FALSE              | 60   | 6    | Max attempts must be 1-5
                TC-R09-BC-07 | attempts: >= 1 -> TRUE, <= 5 -> TRUE (min)         | 60   | 1    | valid
                TC-R09-BC-08 | attempts: >= 1 -> TRUE, <= 5 -> TRUE (max)         | 60   | 5    | valid
                """)
        void branches(String id, String condition, Integer passing, Integer attempts, String expected,
                      TestCase testCase) {
            String actual = validate(passing, attempts);
            testCase.record(id, condition, "passing=" + passing + ", attempts=" + attempts, expected, actual);
            assertEquals(expected, actual);
        }
    }
}
