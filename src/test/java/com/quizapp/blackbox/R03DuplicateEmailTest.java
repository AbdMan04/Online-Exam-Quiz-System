package com.quizapp.blackbox;

import com.quizapp.service.AuthService;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.BlackBoxTest;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static com.quizapp.support.ActualOutput.quoted;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R3: Duplicate email.
 * Given an account with email "alice@uni.edu" already exists, when a new user registers with the same email,
 * then the system rejects it with "Email already registered".
 */
@BlackBoxTest
@DisplayName("R3 - Duplicate email")
class R03DuplicateEmailTest {

    private static final String EXISTING = "alice@uni.edu";
    private static final String SUCCESS = R01RegisterTest.SUCCESS;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void givenAnExistingAccount() {
        authService.register("Alice Smith", EXISTING, "Secure123");
    }

    private String registerWith(String email) {
        return ActualOutput.of(() -> {
            authService.register("New User", email, "Other1234");
            return SUCCESS;
        });
    }

    @Nested
    @DisplayName("Table 3-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R03-EP-01 | Email not registered yet                  | bob@uni.edu           | Registration successful (role STUDENT)
                TC-R03-EP-02 | Same email as an existing account         | alice@uni.edu         | Email already registered
                TC-R03-EP-03 | Same email in different letter case       | ALICE@Uni.Edu         | Email already registered
                TC-R03-EP-04 | Same email with surrounding spaces        | '  alice@uni.edu  '   | Email already registered
                TC-R03-EP-05 | Same address but invalid format (checked first) | alice@uni      | Invalid email
                """)
        void partitions(String id, String condition, String email, String expected, TestCase testCase) {
            String actual = registerWith(email);
            testCase.record(id, condition, "existing=" + quoted(EXISTING) + ", new email=" + quoted(email), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 3-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R03-BVA-01 | Email differs from existing by 1 character (just outside duplicate) | alicf@uni.edu | Registration successful (role STUDENT)
                TC-R03-BVA-02 | Email differs from existing by 0 characters (duplicate)             | alice@uni.edu | Email already registered
                TC-R03-BVA-03 | Existing email plus 1 character                                     | alice1@uni.edu | Registration successful (role STUDENT)
                TC-R03-BVA-04 | Existing email minus 1 character                                    | alic@uni.edu  | Registration successful (role STUDENT)
                """)
        void nearDuplicates(String id, String condition, String email, String expected, TestCase testCase) {
            String actual = registerWith(email);
            testCase.record(id, condition, "existing=" + quoted(EXISTING) + ", new email=" + quoted(email), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
