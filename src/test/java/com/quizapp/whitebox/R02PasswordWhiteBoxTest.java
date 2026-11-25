package com.quizapp.whitebox;

import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.validation.PasswordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.quizapp.support.ActualOutput.quoted;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R2 white-box tests. Functions under test: {@code PasswordValidator.validate} and {@code isValid}
 * (with its helpers containsLetter / containsDigit).
 */
@WhiteBoxTest
@DisplayName("R2 - Password validation (white-box)")
class R02PasswordWhiteBoxTest {

    private static String validate(String password) {
        return ActualOutput.of(() -> {
            PasswordValidator.validate(password);
            return "valid";
        });
    }

    @Nested
    @DisplayName("Table 2-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R02-SC-01 | validate() passes; isValid() runs to 'return letter && digit' (both helpers run) | Secure123 | valid
                TC-R02-SC-02 | validate(): statement 'throw INVALID_PASSWORD'; isValid(): 'return false' for null | NULL      | Password must be 8-20 characters with a letter and a digit
                TC-R02-SC-03 | isValid(): 'return false' for a length outside 8-20                                | abc12     | Password must be 8-20 characters with a letter and a digit
                """)
        void statements(String id, String condition, String password, String expected, TestCase testCase) {
            String actual = validate(password);
            testCase.record(id, condition, "password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 2-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R02-BC-01 | isValid: password == null -> TRUE                                   | NULL                  | false
                TC-R02-BC-02 | isValid: password == null -> FALSE, length < 8 -> TRUE              | abc1234               | false
                TC-R02-BC-03 | isValid: length < 8 -> FALSE, length > 20 -> TRUE                   | abcdefghij1234567890X | false
                TC-R02-BC-04 | isValid: length > 20 -> FALSE, containsLetter -> FALSE (digit skipped) | 12345678          | false
                TC-R02-BC-05 | isValid: containsLetter -> TRUE, containsDigit -> FALSE              | abcdefgh              | false
                TC-R02-BC-06 | isValid: containsLetter -> TRUE, containsDigit -> TRUE               | abcdefg1              | true
                """)
        void isValidBranches(String id, String condition, String password, String expected, TestCase testCase) {
            String actual = String.valueOf(PasswordValidator.isValid(password));
            testCase.record(id, condition, "password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R02-BC-07 | validate: !isValid(password) -> TRUE  | abc     | Password must be 8-20 characters with a letter and a digit
                TC-R02-BC-08 | validate: !isValid(password) -> FALSE | Secure123 | valid
                """)
        void validateBranches(String id, String condition, String password, String expected, TestCase testCase) {
            String actual = validate(password);
            testCase.record(id, condition, "password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
