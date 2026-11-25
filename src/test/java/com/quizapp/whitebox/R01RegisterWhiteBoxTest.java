package com.quizapp.whitebox;

import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.validation.AccountValidator;
import com.quizapp.web.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static com.quizapp.support.ActualOutput.quoted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * R1 white-box tests. Functions under test:
 * {@code AccountValidator.validateName}, {@code validateEmail}, {@code normalizeEmail},
 * {@code AuthService.register} (success path) and {@code AuthController.register}.
 */
@WhiteBoxTest
@DisplayName("R1 - Register (white-box)")
class R01RegisterWhiteBoxTest {

    private static final String VALID = "valid";

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, new BCryptPasswordEncoder(4));
    }

    private static String validateName(String name) {
        return ActualOutput.of(() -> {
            AccountValidator.validateName(name);
            return VALID;
        });
    }

    private static String validateEmail(String email) {
        return ActualOutput.of(() -> {
            AccountValidator.validateEmail(email);
            return VALID;
        });
    }

    private void newEmailIsFree() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Nested
    @DisplayName("Table 1-3: Statement coverage")
    class StatementCoverage {

        @Test
        void validRegistrationRunsTheWholeSuccessPath(TestCase testCase) {
            newEmailIsFree();
            String actual = ActualOutput.of(() -> {
                User user = authService.register("Alice Smith", " Alice@Uni.edu ", "Secure123");
                return "saved " + user.getEmail() + " as " + user.getRole();
            });
            String expected = "saved alice@uni.edu as STUDENT";
            testCase.record("TC-R01-SC-01",
                    "register(): every statement of validateRegistration, validateName, validateEmail, normalizeEmail and the save path",
                    "name=\"Alice Smith\", email=\" Alice@Uni.edu \", password=\"Secure123\"", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R01-SC-02 | validateName(): statement 'throw NAME_REQUIRED'    | NULL    | Name is required
                TC-R01-SC-03 | validateName(): statement 'throw NAME_TOO_LONG'    | LEN:101 | Name must be at most 100 characters
                """)
        void nameThrowStatements(String id, String condition, String nameSpec, String expected, TestCase testCase) {
            String name = Specs.text(nameSpec);
            String actual = validateName(name);
            testCase.record(id, condition, "name=" + Specs.describe(name), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R01-SC-04 | validateEmail(): first 'throw INVALID_EMAIL' (null)       | NULL          | Invalid email
                TC-R01-SC-05 | validateEmail(): second 'throw INVALID_EMAIL' (bad format) | alice-uni.edu | Invalid email
                """)
        void emailThrowStatements(String id, String condition, String email, String expected, TestCase testCase) {
            String actual = validateEmail(email);
            testCase.record(id, condition, "email=" + quoted(email), expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerSuccessStatements(TestCase testCase) {
            newEmailIsFree();
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
            String view = new AuthController(authService)
                    .register("Alice Smith", "alice@uni.edu", "Secure123", new ExtendedModelMap(), redirect);
            String actual = "view=" + view + ", flash message=" + redirect.getFlashAttributes().get("message");
            String expected = "view=redirect:/login, flash message=Registration successful";
            testCase.record("TC-R01-SC-06", "AuthController.register(): try-block statements (success)",
                    "name=\"Alice Smith\", email=\"alice@uni.edu\", password=\"Secure123\"", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerErrorStatements(TestCase testCase) {
            ExtendedModelMap model = new ExtendedModelMap();
            String view = new AuthController(authService)
                    .register(" ", "alice@uni.edu", "Secure123", model, new RedirectAttributesModelMap());
            String actual = "view=" + view + ", error=" + model.get("error") + ", email kept=" + model.get("email");
            String expected = "view=register, error=Name is required, email kept=alice@uni.edu";
            testCase.record("TC-R01-SC-07", "AuthController.register(): catch-block statements (error shown, input kept)",
                    "name=\" \", email=\"alice@uni.edu\", password=\"Secure123\"", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 1-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R01-BC-01 | validateName: name == null -> TRUE                              | NULL    | Name is required
                TC-R01-BC-02 | validateName: name == null -> FALSE, name.isBlank() -> TRUE     | '   '   | Name is required
                TC-R01-BC-03 | validateName: isBlank() -> FALSE, length > 100 -> TRUE          | LEN:101 | Name must be at most 100 characters
                TC-R01-BC-04 | validateName: length > 100 -> FALSE                             | LEN:100 | valid
                """)
        void validateNameBranches(String id, String condition, String nameSpec, String expected, TestCase testCase) {
            String name = Specs.text(nameSpec);
            String actual = validateName(name);
            testCase.record(id, condition, "name=" + Specs.describe(name), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", textBlock = """
                TC-R01-BC-05 | validateEmail: email == null -> TRUE                             | NULL          | Invalid email
                TC-R01-BC-06 | validateEmail: email == null -> FALSE, length > 100 -> TRUE      | LEN:101       | Invalid email
                TC-R01-BC-07 | validateEmail: length > 100 -> FALSE, pattern matches -> FALSE   | alice-uni.edu | Invalid email
                TC-R01-BC-08 | validateEmail: pattern matches -> TRUE                           | alice@uni.edu | valid
                """)
        void validateEmailBranches(String id, String condition, String emailSpec, String expected, TestCase testCase) {
            String email = Specs.email(emailSpec);
            String actual = validateEmail(email);
            testCase.record(id, condition, "email=" + Specs.describe(email), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL", emptyValue = "", textBlock = """
                TC-R01-BC-09 | normalizeEmail: email == null -> TRUE  | NULL             | ''
                TC-R01-BC-10 | normalizeEmail: email == null -> FALSE | ' Alice@Uni.EDU ' | alice@uni.edu
                """)
        void normalizeEmailBranches(String id, String condition, String email, String expected, TestCase testCase) {
            String actual = AccountValidator.normalizeEmail(email);
            testCase.record(id, condition, "email=" + quoted(email), quoted(expected), quoted(actual));
            assertEquals(expected, actual);
        }
    }
}
