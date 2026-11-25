package com.quizapp.blackbox;

import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R6: Create quiz.
 * Given an instructor enters title "Java Basics", duration 30, passing score 60, and max attempts 2,
 * when the quiz is saved, then the quiz is created with status "DRAFT".
 */
@DisplayName("R6 - Create quiz")
class R06CreateQuizTest extends QuizFormSupport {

    @Nested
    @DisplayName("Table 6-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R06-EP-01 | All fields valid                                       | Java Basics       | 30  | 60  | 2 | Quiz created with status DRAFT
                TC-R06-EP-02 | All valid, title with surrounding spaces               | '  Java Basics  ' | 30  | 60  | 2 | Quiz created with status DRAFT
                TC-R06-EP-03 | Title and duration invalid (title reported first)      | Ja                | 300 | 60  | 2 | Invalid title length
                TC-R06-EP-04 | Duration and passing score invalid (duration first)    | Java Basics       | 300 | 150 | 2 | Duration must be 5-180 minutes
                TC-R06-EP-05 | Passing score and attempts invalid (passing first)     | Java Basics       | 30  | 150 | 9 | Passing score must be 0-100
                """)
        void partitions(String id, String condition, String title, Integer duration, Integer passing, Integer attempts,
                        String expected, TestCase testCase) {
            String actual = saveQuiz(title, duration, passing, attempts);
            testCase.record(id, condition, describe(title, duration, passing, attempts), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 6-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R06-BVA-01 | Every field at its minimum            | LEN:3   | 5   | 0   | 1 | Quiz created with status DRAFT
                TC-R06-BVA-02 | Every field at its maximum            | LEN:100 | 180 | 100 | 5 | Quiz created with status DRAFT
                TC-R06-BVA-03 | Every field at minimum - 1            | LEN:2   | 4   | -1  | 0 | Invalid title length
                TC-R06-BVA-04 | Every field at maximum + 1            | LEN:101 | 181 | 101 | 6 | Invalid title length
                """)
        void allFieldsAtBoundaries(String id, String condition, String titleSpec, Integer duration, Integer passing,
                                   Integer attempts, String expected, TestCase testCase) {
            String title = title(titleSpec);
            String actual = saveQuiz(title, duration, passing, attempts);
            testCase.record(id, condition, describe(title, duration, passing, attempts), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
