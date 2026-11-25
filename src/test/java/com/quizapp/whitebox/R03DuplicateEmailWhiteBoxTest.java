package com.quizapp.whitebox;

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

import static com.quizapp.support.ActualOutput.quoted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * R3 white-box tests. Function under test: the duplicate check in {@code AuthService.register}
 * ({@code if (userRepository.existsByEmail(normalizedEmail))}).
 * The repository is a mock that already holds "alice@uni.edu".
 */
@WhiteBoxTest
@DisplayName("R3 - Duplicate email (white-box)")
class R03DuplicateEmailWhiteBoxTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void givenAliceIsRegistered() {
        authService = new AuthService(userRepository, new BCryptPasswordEncoder(4));
        when(userRepository.existsByEmail(any())).thenAnswer(call -> "alice@uni.edu".equals(call.getArgument(0)));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    /** Registers and reports the outcome plus how often the repository saved a user. */
    private String register(String email) {
        String outcome = ActualOutput.of(() -> "saved " + authService.register("New User", email, "Other1234").getEmail());
        long saves = mockingDetails(userRepository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save")).count();
        return outcome + " (save called " + saves + "x)";
    }

    @Nested
    @DisplayName("Table 3-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R03-SC-01 | register(): statements after the duplicate check (create user, save)  | bob@uni.edu   | saved bob@uni.edu (save called 1x)
                TC-R03-SC-02 | register(): statement 'throw EMAIL_TAKEN'                             | alice@uni.edu | Email already registered (save called 0x)
                """)
        void statements(String id, String condition, String email, String expected, TestCase testCase) {
            String actual = register(email);
            testCase.record(id, condition, "existing=\"alice@uni.edu\", email=" + quoted(email), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 3-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R03-BC-01 | existsByEmail(normalized) -> TRUE (exact match)              | alice@uni.edu       | Email already registered (save called 0x)
                TC-R03-BC-02 | existsByEmail(normalized) -> TRUE (match after normalizing) | '  ALICE@Uni.edu  ' | Email already registered (save called 0x)
                TC-R03-BC-03 | existsByEmail(normalized) -> FALSE                           | bob@uni.edu         | saved bob@uni.edu (save called 1x)
                """)
        void duplicateCheck(String id, String condition, String email, String expected, TestCase testCase) {
            String actual = register(email);
            testCase.record(id, condition, "existing=\"alice@uni.edu\", email=" + quoted(email), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
