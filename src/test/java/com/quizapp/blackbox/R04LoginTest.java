package com.quizapp.blackbox;

import com.quizapp.service.AuthService;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.BlackBoxTest;
import com.quizapp.support.TestCase;
import com.quizapp.web.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static com.quizapp.support.ActualOutput.quoted;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R4: Login.
 * Given a registered user with correct credentials, when the user logs in,
 * then the system opens the dashboard for the user's role.
 */
@BlackBoxTest
@DisplayName("R4 - Login")
class R04LoginTest {

    static final String STUDENT_EMAIL = "alice@uni.edu";
    static final String STUDENT_PASSWORD = "Secure123";

    @Autowired
    private AuthService authService;

    @BeforeEach
    void givenARegisteredStudent() {
        authService.register("Alice Smith", STUDENT_EMAIL, STUDENT_PASSWORD);
    }

    /** What the user sees after logging in: the dashboard that opens, or the error message. */
    static String login(AuthService authService, String email, String password) {
        return ActualOutput.of(() -> "Opens " + SessionUser.from(authService.login(email, password)).dashboardPath());
    }

    @Nested
    @DisplayName("Table 4-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", emptyValue = "", textBlock = """
                TC-R04-EP-01 | Student, correct credentials            | alice@uni.edu      | Secure123 | Opens /student/dashboard
                TC-R04-EP-02 | Instructor, correct credentials         | instructor@uni.edu | Teach1234 | Opens /instructor/dashboard
                TC-R04-EP-03 | Correct credentials, email in caps      | ALICE@UNI.EDU      | Secure123 | Opens /student/dashboard
                TC-R04-EP-04 | Registered email, wrong password        | alice@uni.edu      | Wrong1234 | Invalid email or password
                TC-R04-EP-05 | Password differs only in letter case    | alice@uni.edu      | secure123 | Invalid email or password
                TC-R04-EP-06 | Email not registered                    | nobody@uni.edu     | Secure123 | Invalid email or password
                TC-R04-EP-07 | Password empty                          | alice@uni.edu      | ''        | Invalid email or password
                TC-R04-EP-08 | Email missing (null)                    | NULL               | Secure123 | Invalid email or password
                """)
        void partitions(String id, String condition, String email, String password, String expected, TestCase testCase) {
            String actual = login(authService, email, password);
            testCase.record(id, condition, "email=" + quoted(email) + ", password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 4-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R04-BVA-01 | Earlier wrong passwords = min (0)                  | 0 | Opens /student/dashboard
                TC-R04-BVA-02 | Earlier wrong passwords = max still allowed (2)    | 2 | Opens /student/dashboard
                TC-R04-BVA-03 | Earlier wrong passwords = max + 1 (3, locked)      | 3 | Account locked
                """)
        void earlierFailures(String id, String condition, int failures, String expected, TestCase testCase) {
            for (int i = 0; i < failures; i++) {
                login(authService, STUDENT_EMAIL, "Wrong1234");
            }
            String actual = login(authService, STUDENT_EMAIL, STUDENT_PASSWORD);
            testCase.record(id, condition, failures + " wrong passwords, then email=" + quoted(STUDENT_EMAIL)
                    + ", password=" + quoted(STUDENT_PASSWORD), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
