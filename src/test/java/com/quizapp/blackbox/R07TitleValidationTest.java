package com.quizapp.blackbox;

import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R7: Title validation.
 * Given an instructor enters a title shorter than 3 or longer than 100 characters, when the quiz is saved,
 * then the system rejects it with "Invalid title length".
 */
@DisplayName("R7 - Title validation")
class R07TitleValidationTest extends QuizFormSupport {

    @Nested
    @DisplayName("Table 7-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", emptyValue = "", textBlock = """
                TC-R07-EP-01 | Title 3-100 characters                      | Java Basics | Quiz created with status DRAFT
                TC-R07-EP-02 | Title shorter than 3                        | Ja          | Invalid title length
                TC-R07-EP-03 | Title longer than 100                       | LEN:150     | Invalid title length
                TC-R07-EP-04 | Title empty                                 | ''          | Invalid title length
                TC-R07-EP-05 | Title only spaces                           | '      '    | Invalid title length
                TC-R07-EP-06 | Title missing (null)                        | NULL        | Invalid title length
                TC-R07-EP-07 | Short title padded with spaces (trimmed)    | '   Ja   '  | Invalid title length
                """)
        void partitions(String id, String condition, String titleSpec, String expected, TestCase testCase) {
            String title = title(titleSpec);
            String actual = saveQuiz(title, 30, 60, 2);
            testCase.record(id, condition, describe(title, 30, 60, 2), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 7-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R07-BVA-01 | Length = min - 1 (2)   | 2   | Invalid title length
                TC-R07-BVA-02 | Length = min (3)       | 3   | Quiz created with status DRAFT
                TC-R07-BVA-03 | Length = max (100)     | 100 | Quiz created with status DRAFT
                TC-R07-BVA-04 | Length = max + 1 (101) | 101 | Invalid title length
                """)
        void length(String id, String condition, int length, String expected, TestCase testCase) {
            String title = "T".repeat(length);
            String actual = saveQuiz(title, 30, 60, 2);
            testCase.record(id, condition, describe(title, 30, 60, 2), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
