package com.quizapp.whitebox;

import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * R5 white-box tests. Functions under test: {@code User.recordFailedLogin} (lock threshold) and the
 * lock branches of {@code AuthService.login} ({@code if (user.isLocked())} and
 * {@code user.isLocked() ? ACCOUNT_LOCKED : BAD_CREDENTIALS}).
 */
@WhiteBoxTest
@DisplayName("R5 - Account lock (white-box)")
class R05AccountLockWhiteBoxTest {

    private static final String PASSWORD = "Secure123";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthService authService;
    private User alice;

    @BeforeEach
    void givenAliceIsRegistered() {
        authService = new AuthService(userRepository, encoder);
        alice = new User("Alice Smith", "alice@uni.edu", encoder.encode(PASSWORD), Role.STUDENT);
        lenient().when(userRepository.findByEmail("alice@uni.edu")).thenReturn(Optional.of(alice));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    /** Alice already has {@code earlierFailures} failed logins recorded. */
    private void earlierFailures(int earlierFailures) {
        for (int i = 0; i < earlierFailures; i++) {
            alice.recordFailedLogin();
        }
    }

    private String state() {
        return "failed count " + alice.getFailedLoginAttempts() + ", " + (alice.isLocked() ? "locked" : "not locked");
    }

    /** Calls recordFailedLogin() once more and reports the account state. */
    private String recordOneMoreFailure(int earlier) {
        earlierFailures(earlier);
        alice.recordFailedLogin();
        return state();
    }

    /** Logs in once and reports the message plus the account state. */
    private String loginOnce(int earlier, String password) {
        earlierFailures(earlier);
        String message = ActualOutput.of(() -> "logged in as " + authService.login("alice@uni.edu", password).getRole());
        return message + " (" + state() + ")";
    }

    @Nested
    @DisplayName("Table 5-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R05-SC-01 | recordFailedLogin(): increment statement only (limit not reached) | 0 | failed count 1, not locked
                TC-R05-SC-02 | recordFailedLogin(): increment and 'locked = true' statements     | 2 | failed count 3, locked
                """)
        void recordFailedLoginStatements(String id, String condition, int earlier, String expected,
                                         TestCase testCase) {
            String actual = recordOneMoreFailure(earlier);
            testCase.record(id, condition, earlier + " earlier failures, then recordFailedLogin()", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R05-SC-03 | login(): statement 'throw ACCOUNT_LOCKED' for an already locked account | 3 | Secure123 | Account locked (failed count 3, locked)
                TC-R05-SC-04 | login(): failure path ending in 'throw ACCOUNT_LOCKED' (3rd failure)     | 2 | Wrong1234 | Account locked (failed count 3, locked)
                """)
        void loginLockStatements(String id, String condition, int earlier, String password, String expected,
                                 TestCase testCase) {
            String actual = loginOnce(earlier, password);
            testCase.record(id, condition, earlier + " earlier failures, then login with password=\"" + password + "\"",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 5-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R05-BC-01 | recordFailedLogin: failedLoginAttempts >= 3 -> FALSE (2nd failure) | 1 | failed count 2, not locked
                TC-R05-BC-02 | recordFailedLogin: failedLoginAttempts >= 3 -> TRUE (3rd failure)  | 2 | failed count 3, locked
                """)
        void thresholdBranches(String id, String condition, int earlier, String expected, TestCase testCase) {
            String actual = recordOneMoreFailure(earlier);
            testCase.record(id, condition, earlier + " earlier failures, then recordFailedLogin()", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R05-BC-03 | login: user.isLocked() -> TRUE (password never checked)              | 3 | Secure123 | Account locked (failed count 3, locked)
                TC-R05-BC-04 | login: after failure, user.isLocked() ? ... -> TRUE (ACCOUNT_LOCKED) | 2 | Wrong1234 | Account locked (failed count 3, locked)
                TC-R05-BC-05 | login: after failure, user.isLocked() ? ... -> FALSE (BAD_CREDENTIALS) | 1 | Wrong1234 | Invalid email or password (failed count 2, not locked)
                """)
        void loginLockBranches(String id, String condition, int earlier, String password, String expected,
                               TestCase testCase) {
            String actual = loginOnce(earlier, password);
            testCase.record(id, condition, earlier + " earlier failures, then login with password=\"" + password + "\"",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }
}
