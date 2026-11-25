package com.quizapp.blackbox;

import com.quizapp.exception.Messages;
import com.quizapp.model.User;
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
 * R1: Register.
 * Given a user provides name, email and password, when the registration request is submitted,
 * then the system creates an account with role "STUDENT" and shows "Registration successful".
 */
@BlackBoxTest
@DisplayName("R1 - Register")
class R01RegisterTest {

    static final String SUCCESS = Messages.REGISTER_OK + " (role STUDENT)";
    private static final String PASSWORD = "Secure123";

    @Autowired
    private AuthService authService;

    /** What the user sees after submitting the registration form. */
    private String register(String name, String email) {
        return ActualOutput.of(() -> {
            User user = authService.register(name, email, PASSWORD);
            return Messages.REGISTER_OK + " (role " + user.getRole() + ")";
        });
    }

    @Nested
    @DisplayName("Table 1-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", emptyValue = "", textBlock = """
                TC-R01-EP-01 | Valid name, valid email                 | Alice Smith | alice@uni.edu         | Registration successful (role STUDENT)
                TC-R01-EP-02 | Valid email in mixed case with spaces  | Alice Smith | '  Alice@Uni.EDU '    | Registration successful (role STUDENT)
                TC-R01-EP-03 | Name empty                             | ''          | alice@uni.edu         | Name is required
                TC-R01-EP-04 | Name only spaces                       | '   '       | alice@uni.edu         | Name is required
                TC-R01-EP-05 | Name missing (null)                    | NULL        | alice@uni.edu         | Name is required
                TC-R01-EP-06 | Email without @                        | Alice Smith | alice.uni.edu         | Invalid email
                TC-R01-EP-07 | Email without domain extension         | Alice Smith | alice@uni             | Invalid email
                TC-R01-EP-08 | Email without local part               | Alice Smith | @uni.edu              | Invalid email
                TC-R01-EP-09 | Email with a space inside              | Alice Smith | 'ali ce@uni.edu'      | Invalid email
                TC-R01-EP-10 | Email empty                            | Alice Smith | ''                    | Invalid email
                """)
        void partitions(String id, String condition, String name, String email, String expected, TestCase testCase) {
            String actual = register(name, email);
            testCase.record(id, condition, "name=" + quoted(name) + ", email=" + quoted(email)
                    + ", password=" + quoted(PASSWORD), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 1-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R01-BVA-01 | Name length = min - 1 (0)   | 0   | Name is required
                TC-R01-BVA-02 | Name length = min (1)       | 1   | Registration successful (role STUDENT)
                TC-R01-BVA-03 | Name length = max (100)     | 100 | Registration successful (role STUDENT)
                TC-R01-BVA-04 | Name length = max + 1 (101) | 101 | Name must be at most 100 characters
                """)
        void nameLength(String id, String condition, int length, String expected, TestCase testCase) {
            String actual = register("N".repeat(length), "alice@uni.edu");
            testCase.record(id, condition, "name = " + length + " characters, email=\"alice@uni.edu\"", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R01-BVA-05 | Email length = max (100)     | 100 | Registration successful (role STUDENT)
                TC-R01-BVA-06 | Email length = max + 1 (101) | 101 | Invalid email
                """)
        void emailLength(String id, String condition, int length, String expected, TestCase testCase) {
            String domain = "@uni.edu";
            String email = "a".repeat(length - domain.length()) + domain;
            String actual = register("Alice Smith", email);
            testCase.record(id, condition, "email = " + length + " characters (\"aaa...a@uni.edu\")", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R01-BVA-07 | Domain extension length = min - 1 (1) | a@uni.e  | Invalid email
                TC-R01-BVA-08 | Domain extension length = min (2)     | a@uni.ed | Registration successful (role STUDENT)
                """)
        void domainExtension(String id, String condition, String email, String expected, TestCase testCase) {
            String actual = register("Alice Smith", email);
            testCase.record(id, condition, "email=\"" + email + "\"", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
