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
 * R7 white-box tests. Functions under test: {@code QuizValidator.isValidTitle}
 * ({@code title == null}, then {@code inRange(title.trim().length(), 3, 100)}) and the title decision
 * of {@code QuizValidator.validate}.
 */
@WhiteBoxTest
@DisplayName("R7 - Title validation (white-box)")
class R07TitleWhiteBoxTest {

    @Nested
    @DisplayName("Table 7-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R07-SC-01 | isValidTitle(): statement 'return false' (null)    | NULL        | false
                TC-R07-SC-02 | isValidTitle(): statement 'return inRange(...)'    | Java Basics | true
                """)
        void isValidTitleStatements(String id, String condition, String title, String expected, TestCase testCase) {
            String actual = String.valueOf(QuizValidator.isValidTitle(title));
            testCase.record(id, condition, "title=" + Specs.describe(title), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R07-SC-03 | validate(): statement 'throw INVALID_TITLE' | Ja | Invalid title length
                """)
        void validateThrow(String id, String condition, String title, String expected, TestCase testCase) {
            String actual = ActualOutput.of(() -> {
                QuizValidator.validate(new QuizInput(title, 30, 60, 2));
                return "valid";
            });
            testCase.record(id, condition, "title=" + Specs.describe(title) + ", 30, 60, 2", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 7-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R07-BC-01 | isValidTitle: title == null -> TRUE                                   | NULL       | false
                TC-R07-BC-02 | title == null -> FALSE; inRange: length >= 3 -> FALSE                  | Ja         | false
                TC-R07-BC-03 | inRange: length >= 3 -> TRUE, length <= 100 -> FALSE                   | LEN:101    | false
                TC-R07-BC-04 | inRange: length >= 3 -> TRUE, length <= 100 -> TRUE                    | LEN:3      | true
                TC-R07-BC-05 | trim() before inRange: 8 characters but only 2 after trimming -> FALSE | '   Ja   ' | false
                """)
        void isValidTitleBranches(String id, String condition, String titleSpec, String expected, TestCase testCase) {
            String title = Specs.text(titleSpec);
            String actual = String.valueOf(QuizValidator.isValidTitle(title));
            testCase.record(id, condition, "title=" + Specs.describe(title), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
