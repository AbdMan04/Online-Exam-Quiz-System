package com.quizapp.blackbox;

import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R9: Attempts and passing score validation.
 * Given an instructor enters max attempts outside 1-5 or passing score outside 0-100, when the quiz is saved,
 * then the system rejects it with "Max attempts must be 1-5" or "Passing score must be 0-100" respectively.
 */
@DisplayName("R9 - Attempts and passing score validation")
class R09AttemptsAndPassingScoreTest extends QuizFormSupport {

    @Nested
    @DisplayName("Table 9-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R09-EP-01 | Passing score and attempts both valid | 60   | 2    | Quiz created with status DRAFT
                TC-R09-EP-02 | Passing score below 0                 | -5   | 2    | Passing score must be 0-100
                TC-R09-EP-03 | Passing score above 100               | 150  | 2    | Passing score must be 0-100
                TC-R09-EP-04 | Passing score missing (null)          | NULL | 2    | Passing score must be 0-100
                TC-R09-EP-05 | Max attempts below 1                  | 60   | -3   | Max attempts must be 1-5
                TC-R09-EP-06 | Max attempts above 5                  | 60   | 10   | Max attempts must be 1-5
                TC-R09-EP-07 | Max attempts missing (null)           | 60   | NULL | Max attempts must be 1-5
                """)
        void partitions(String id, String condition, Integer passing, Integer attempts, String expected,
                        TestCase testCase) {
            String actual = saveQuiz("Java Basics", 30, passing, attempts);
            testCase.record(id, condition, describe("Java Basics", 30, passing, attempts), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 9-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R09-BVA-01 | Passing score = min - 1 (-1)  | -1  | 2 | Passing score must be 0-100
                TC-R09-BVA-02 | Passing score = min (0)       | 0   | 2 | Quiz created with status DRAFT
                TC-R09-BVA-03 | Passing score = max (100)     | 100 | 2 | Quiz created with status DRAFT
                TC-R09-BVA-04 | Passing score = max + 1 (101) | 101 | 2 | Passing score must be 0-100
                TC-R09-BVA-05 | Max attempts = min - 1 (0)    | 60  | 0 | Max attempts must be 1-5
                TC-R09-BVA-06 | Max attempts = min (1)        | 60  | 1 | Quiz created with status DRAFT
                TC-R09-BVA-07 | Max attempts = max (5)        | 60  | 5 | Quiz created with status DRAFT
                TC-R09-BVA-08 | Max attempts = max + 1 (6)    | 60  | 6 | Max attempts must be 1-5
                """)
        void boundaries(String id, String condition, int passing, int attempts, String expected, TestCase testCase) {
            String actual = saveQuiz("Java Basics", 30, passing, attempts);
            testCase.record(id, condition, describe("Java Basics", 30, passing, attempts), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
