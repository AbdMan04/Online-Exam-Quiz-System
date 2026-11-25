package com.quizapp.whitebox;

import com.quizapp.dto.QuizInput;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.validation.QuizValidator;
import com.quizapp.validation.ValidationRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R8 white-box tests. Functions under test: the duration decision of {@code QuizValidator.validate} and
 * {@code ValidationRules.inRange(value, 5, 180)} ({@code value != null && value >= min && value <= max}).
 */
@WhiteBoxTest
@DisplayName("R8 - Duration validation (white-box)")
class R08DurationWhiteBoxTest {

    private static String validateDuration(Integer duration) {
        return ActualOutput.of(() -> {
            QuizValidator.validate(new QuizInput("Java Basics", duration, 60, 2));
            return "valid";
        });
    }

    @Nested
    @DisplayName("Table 8-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R08-SC-01 | validate(): statement 'throw INVALID_DURATION'               | 4  | Duration must be 5-180 minutes
                TC-R08-SC-02 | validate(): duration check passes, later statements run     | 30 | valid
                """)
        void statements(String id, String condition, Integer duration, String expected, TestCase testCase) {
            String actual = validateDuration(duration);
            testCase.record(id, condition, "title=\"Java Basics\", duration=" + duration + ", 60, 2", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R08-SC-03 | inRange(): its single return statement | 180 | true
                """)
        void inRangeStatement(String id, String condition, Integer value, String expected, TestCase testCase) {
            String actual = String.valueOf(ValidationRules.inRange(value, 5, 180));
            testCase.record(id, condition, "inRange(" + value + ", 5, 180)", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 8-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R08-BC-01 | inRange: value != null -> FALSE                           | NULL | Duration must be 5-180 minutes
                TC-R08-BC-02 | value != null -> TRUE, value >= 5 -> FALSE                | 4    | Duration must be 5-180 minutes
                TC-R08-BC-03 | value >= 5 -> TRUE, value <= 180 -> FALSE                 | 181  | Duration must be 5-180 minutes
                TC-R08-BC-04 | value >= 5 -> TRUE, value <= 180 -> TRUE (lower boundary) | 5    | valid
                TC-R08-BC-05 | value >= 5 -> TRUE, value <= 180 -> TRUE (upper boundary) | 180  | valid
                """)
        void inRangeBranches(String id, String condition, Integer duration, String expected, TestCase testCase) {
            String actual = validateDuration(duration);
            testCase.record(id, condition, "title=\"Java Basics\", duration=" + duration + ", 60, 2", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
