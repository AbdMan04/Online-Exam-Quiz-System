package com.quizapp.blackbox;

import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R8: Duration validation.
 * Given an instructor enters a duration outside 5-180 minutes, when the quiz is saved,
 * then the system rejects it with "Duration must be 5-180 minutes".
 */
@DisplayName("R8 - Duration validation")
class R08DurationValidationTest extends QuizFormSupport {

    @Nested
    @DisplayName("Table 8-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R08-EP-01 | Duration inside 5-180       | 30   | Quiz created with status DRAFT
                TC-R08-EP-02 | Duration below 5            | 2    | Duration must be 5-180 minutes
                TC-R08-EP-03 | Duration above 180          | 300  | Duration must be 5-180 minutes
                TC-R08-EP-04 | Duration negative           | -10  | Duration must be 5-180 minutes
                TC-R08-EP-05 | Duration missing (null)     | NULL | Duration must be 5-180 minutes
                """)
        void partitions(String id, String condition, Integer duration, String expected, TestCase testCase) {
            String actual = saveQuiz("Java Basics", duration, 60, 2);
            testCase.record(id, condition, describe("Java Basics", duration, 60, 2), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 8-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R08-BVA-01 | Duration = min - 1 (4)   | 4   | Duration must be 5-180 minutes
                TC-R08-BVA-02 | Duration = min (5)       | 5   | Quiz created with status DRAFT
                TC-R08-BVA-03 | Duration = max (180)     | 180 | Quiz created with status DRAFT
                TC-R08-BVA-04 | Duration = max + 1 (181) | 181 | Duration must be 5-180 minutes
                """)
        void boundaries(String id, String condition, int duration, String expected, TestCase testCase) {
            String actual = saveQuiz("Java Basics", duration, 60, 2);
            testCase.record(id, condition, describe("Java Basics", duration, 60, 2), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
