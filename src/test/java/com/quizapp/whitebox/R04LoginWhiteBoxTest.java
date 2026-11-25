package com.quizapp.whitebox;

import com.quizapp.config.DataSeeder;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.web.AuthController;
import com.quizapp.web.RoleInterceptor;
import com.quizapp.web.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;

import java.util.Optional;

import static com.quizapp.support.ActualOutput.quoted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * R4 white-box tests. Functions under test: {@code AuthService.login} (success and wrong-password paths),
 * {@code SessionUser.dashboardPath}, {@code AuthController} (home, login form, login, logout),
 * {@code RoleInterceptor.preHandle} (role guard) and {@code DataSeeder.run} (the instructor account).
 * The lock branches of login() are covered in R5.
 */
@WhiteBoxTest
@DisplayName("R4 - Login (white-box)")
class R04LoginWhiteBoxTest {

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
        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.findByEmail("alice@uni.edu")).thenReturn(Optional.of(alice));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    /** Logs in and reports the outcome plus Alice's failed-login counter. */
    private String login(String email, String password) {
        String outcome = ActualOutput.of(() -> "logged in as " + authService.login(email, password).getRole());
        return outcome + " (failed count " + alice.getFailedLoginAttempts() + ")";
    }

    private static String guard(Role required, MockHttpSession session) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (session != null) {
            request.setSession(session);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = new RoleInterceptor(required).preHandle(request, response, new Object());
        return allowed ? "allowed" : "blocked, redirect to " + response.getRedirectedUrl();
    }

    /** Runs the startup seeder and reports which account it saved, if any. */
    private String runSeeder() {
        new DataSeeder(userRepository, encoder).run();
        return mockingDetails(userRepository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save"))
                .map(invocation -> (User) invocation.getArgument(0))
                .map(user -> "saved " + user.getEmail() + " as " + user.getRole())
                .findFirst()
                .orElse("nothing saved");
    }

    private static MockHttpSession sessionWith(Object user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.SESSION_KEY, user);
        return session;
    }

    @Nested
    @DisplayName("Table 4-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R04-SC-01 | login(): success statements (reset counter, save, return user)                      | alice@uni.edu  | Secure123 | logged in as STUDENT (failed count 0)
                TC-R04-SC-02 | login(): orElseThrow lambda 'throw BAD_CREDENTIALS' (unknown email)                 | nobody@uni.edu | Secure123 | Invalid email or password (failed count 0)
                TC-R04-SC-03 | login(): failure statements (recordFailedLogin, save, throw)                        | alice@uni.edu  | Wrong1234 | Invalid email or password (failed count 1)
                """)
        void loginStatements(String id, String condition, String email, String password, String expected,
                             TestCase testCase) {
            String actual = login(email, password);
            testCase.record(id, condition, "email=" + quoted(email) + ", password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerLoginSuccess(TestCase testCase) {
            MockHttpSession session = new MockHttpSession();
            String view = new AuthController(authService).login("alice@uni.edu", PASSWORD, session, new ExtendedModelMap());
            SessionUser stored = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
            String actual = "view=" + view + ", session user=" + stored.name() + " (" + stored.role() + ")";
            String expected = "view=redirect:/student/dashboard, session user=Alice Smith (STUDENT)";
            testCase.record("TC-R04-SC-04", "AuthController.login(): try-block statements (session user stored, redirect)",
                    "email=\"alice@uni.edu\", password=\"Secure123\"", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerLoginFailure(TestCase testCase) {
            ExtendedModelMap model = new ExtendedModelMap();
            String view = new AuthController(authService).login("alice@uni.edu", "Wrong1234", new MockHttpSession(), model);
            String actual = "view=" + view + ", error=" + model.get("error") + ", email kept=" + model.get("email");
            String expected = "view=login, error=Invalid email or password, email kept=alice@uni.edu";
            testCase.record("TC-R04-SC-05", "AuthController.login(): catch-block statements",
                    "email=\"alice@uni.edu\", password=\"Wrong1234\"", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerSimplePages(TestCase testCase) {
            AuthController controller = new AuthController(authService);
            MockHttpSession session = sessionWith(SessionUser.from(alice));
            String actual = "home=" + controller.home() + ", loginForm=" + controller.loginForm()
                    + ", registerForm=" + controller.registerForm() + ", logout=" + controller.logout(session)
                    + ", session invalidated=" + session.isInvalid();
            String expected = "home=redirect:/login, loginForm=login, registerForm=register, logout=redirect:/login, "
                    + "session invalidated=true";
            testCase.record("TC-R04-SC-06", "AuthController: home(), loginForm(), registerForm(), logout() statements",
                    "GET /, GET /login, GET /register, POST /logout", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void seederCreatesInstructor(TestCase testCase) {
            when(userRepository.existsByEmail("instructor@uni.edu")).thenReturn(false);
            String actual = runSeeder();
            String expected = "saved instructor@uni.edu as INSTRUCTOR";
            testCase.record("TC-R04-SC-07", "DataSeeder.run(): statements that create the instructor account",
                    "no account with email instructor@uni.edu", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 4-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R04-BC-01 | login: user.isLocked() -> FALSE, password == null -> TRUE              | NULL      | Invalid email or password (failed count 1)
                TC-R04-BC-02 | login: password == null -> FALSE, !matches -> TRUE (wrong password)   | Wrong1234 | Invalid email or password (failed count 1)
                TC-R04-BC-03 | login: password == null -> FALSE, !matches -> FALSE (correct password) | Secure123 | logged in as STUDENT (failed count 0)
                """)
        void loginBranches(String id, String condition, String password, String expected, TestCase testCase) {
            String actual = login("alice@uni.edu", password);
            testCase.record(id, condition, "email=\"alice@uni.edu\", password=" + quoted(password), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R04-BC-04 | dashboardPath: role == INSTRUCTOR -> TRUE  | INSTRUCTOR | /instructor/dashboard
                TC-R04-BC-05 | dashboardPath: role == INSTRUCTOR -> FALSE | STUDENT    | /student/dashboard
                """)
        void dashboardBranches(String id, String condition, Role role, String expected, TestCase testCase) {
            String actual = new SessionUser(1L, "User", role).dashboardPath();
            testCase.record(id, condition, "role=" + role, expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R04-BC-06 | preHandle: session == null -> TRUE (user = null, instanceof -> FALSE)       | NO_SESSION | blocked, redirect to /login
                TC-R04-BC-07 | preHandle: session == null -> FALSE, user attribute missing (instanceof -> FALSE) | EMPTY | blocked, redirect to /login
                TC-R04-BC-08 | preHandle: instanceof SessionUser -> TRUE, role matches -> FALSE           | INSTRUCTOR | blocked, redirect to /login
                TC-R04-BC-09 | preHandle: instanceof SessionUser -> TRUE, role matches -> TRUE            | STUDENT    | allowed
                """)
        void roleGuardBranches(String id, String condition, String sessionState, String expected, TestCase testCase)
                throws Exception {
            MockHttpSession session = switch (sessionState) {
                case "NO_SESSION" -> null;
                case "EMPTY" -> new MockHttpSession();
                default -> sessionWith(new SessionUser(1L, "User", Role.valueOf(sessionState)));
            };
            String actual = guard(Role.STUDENT, session);
            testCase.record(id, condition, "page requires STUDENT, session: " + sessionState, expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R04-BC-10 | DataSeeder.run: !existsByEmail -> TRUE (create instructor) | false | saved instructor@uni.edu as INSTRUCTOR
                TC-R04-BC-11 | DataSeeder.run: !existsByEmail -> FALSE (already there)   | true  | nothing saved
                """)
        void seederBranches(String id, String condition, boolean exists, String expected, TestCase testCase) {
            when(userRepository.existsByEmail("instructor@uni.edu")).thenReturn(exists);
            String actual = runSeeder();
            testCase.record(id, condition, "instructor account exists=" + exists, expected, actual);
            assertEquals(expected, actual);
        }
    }
}
