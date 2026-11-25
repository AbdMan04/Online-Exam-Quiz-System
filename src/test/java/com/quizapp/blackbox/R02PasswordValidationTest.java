package com.quizapp.blackbox;

import com.quizapp.service.AuthService;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.BlackBoxTest;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static com.quizapp.support.ActualOutput.quoted;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R2: Password validation.
 * Given a user provides a password of length 7, when the registration request is submitted,
 * then the system rejects it with "Password must be 8-20 characters with a letter and a digit".
 */
@BlackBoxTest
@DisplayName("R2 - Password validation")
class R02PasswordValidationTest {

    private static final String SUCCESS = R01RegisterTest.SUCCESS;

    @Autowired
    private AuthService authService;

    private String registerWith(String password) {
        return ActualOutput.of(() -> {
            authService.register("Alice Smith", "alice@uni.edu", password);
            return SUCCESS;
        });
    }

    /** A password of the given length containing letters and one digit. */
    private static String passwordOfLength(int length) {
        return "a1" + "b".repeat(length - 2);
    }

    @Nested
    @DisplayName("Table 2-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", emptyValue = "", textBlock = """
                TC-R02-EP-01 | Valid: 8-20 chars with letters and digits | Secure123             | Registration successful (role STUDENT)
                TC-R02-EP-02 | Valid: with special characters            | Secure@123            | Registration successful (role STUDENT)
                TC-R02-EP-03 | Invalid: shorter than 8                   | abc12                 | Password must be 8-20 characters with a letter and a digit
                TC-R02-EP-04 | Invalid: longer than 20                   | abcdefghij1234567890X | Password must be 8-20 characters with a letter and a digit
                TC-R02-EP-05 | Invalid: letters only (no digit)          | abcdefgh              | Password must be 8-20 characters with a letter and a digit
                TC-R02-EP-06 | Invalid: digits only (no letter)          | 12345678              | Password must be 8-20 characters with a letter and a digit
                TC-R02-EP-07 | Invalid: special characters only          | @@@@@@@@              | Password must be 8-20 characters with a letter and a digit
                TC-R02-EP-08 | Invalid: empty                            | ''                    | Password must be 8-20 characters with a letter and a digit
                TC-R02-EP-09 | Invalid: missing (null)                   | NULL                  | Password must be 8-20 characters with a letter and a digit
                """)
        void partitions(String id, String condition, String password, String expected, TestCase testCase) {
            String actual = registerWith(password);
            testCase.record(id, condition, "password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 2-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R02-BVA-01 | Length = min - 1 (7)  | 7  | Password must be 8-20 characters with a letter and a digit
                TC-R02-BVA-02 | Length = min (8)      | 8  | Registration successful (role STUDENT)
                TC-R02-BVA-03 | Length = max (20)     | 20 | Registration successful (role STUDENT)
                TC-R02-BVA-04 | Length = max + 1 (21) | 21 | Password must be 8-20 characters with a letter and a digit
                """)
        void length(String id, String condition, int length, String expected, TestCase testCase) {
            String password = passwordOfLength(length);
            String actual = registerWith(password);
            testCase.record(id, condition, "password=" + quoted(password) + " (" + length + " chars)", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R02-BVA-05 | Letters = min - 1 (0) | 12345678 | Password must be 8-20 characters with a letter and a digit
                TC-R02-BVA-06 | Letters = min (1)     | 1234567a | Registration successful (role STUDENT)
                TC-R02-BVA-07 | Digits = min - 1 (0)  | abcdefgh | Password must be 8-20 characters with a letter and a digit
                TC-R02-BVA-08 | Digits = min (1)      | abcdefg1 | Registration successful (role STUDENT)
                """)
        void characterMix(String id, String condition, String password, String expected, TestCase testCase) {
            String actual = registerWith(password);
            testCase.record(id, condition, "password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
