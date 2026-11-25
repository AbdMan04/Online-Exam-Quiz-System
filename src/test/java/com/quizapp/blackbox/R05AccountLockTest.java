package com.quizapp.blackbox;

import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import com.quizapp.support.BlackBoxTest;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static com.quizapp.blackbox.R04LoginTest.STUDENT_EMAIL;
import static com.quizapp.blackbox.R04LoginTest.STUDENT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R5: Account lock.
 * Given a user has entered a wrong password 2 times, when the user enters a wrong password a 3rd time,
 * then the account is locked and "Account locked" is shown.
 */
@BlackBoxTest
@DisplayName("R5 - Account lock")
class R05AccountLockTest {

    private static final String WRONG = "Wrong1234";

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void givenARegisteredStudent() {
        authService.register("Alice Smith", STUDENT_EMAIL, STUDENT_PASSWORD);
    }

    /**
     * Logs in with each password in turn (C = correct, W = wrong) and reports the message of the last
     * attempt together with the account state, e.g. "Account locked [locked]".
     */
    private String loginSequence(String sequence) {
        String lastMessage = "";
        for (char step : sequence.toCharArray()) {
            String password = step == 'C' ? STUDENT_PASSWORD : WRONG;
            lastMessage = R04LoginTest.login(authService, STUDENT_EMAIL, password);
        }
        boolean locked = userRepository.findByEmail(STUDENT_EMAIL).orElseThrow().isLocked();
        return lastMessage + (locked ? " [locked]" : " [not locked]");
    }

    private static String describe(String sequence) {
        return sequence.replace("W", "wrong ").replace("C", "correct ").trim().replace(" ", ", ");
    }

    @Nested
    @DisplayName("Table 5-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R05-EP-01 | 3 consecutive wrong passwords                         | WWW   | Account locked [locked]
                TC-R05-EP-02 | Locked account, then the correct password            | WWWC  | Account locked [locked]
                TC-R05-EP-03 | Locked account, then another wrong password          | WWWW  | Account locked [locked]
                TC-R05-EP-04 | Wrong passwords interrupted by a successful login    | WWCWW | Invalid email or password [not locked]
                TC-R05-EP-05 | Fewer than 3 wrong passwords, then correct           | WC    | Opens /student/dashboard [not locked]
                """)
        void partitions(String id, String condition, String sequence, String expected, TestCase testCase) {
            String actual = loginSequence(sequence);
            testCase.record(id, condition, "attempts: " + describe(sequence), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 5-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R05-BVA-01 | Consecutive wrong passwords = lock limit - 1 (2) | WW  | Invalid email or password [not locked]
                TC-R05-BVA-02 | Consecutive wrong passwords = lock limit (3)     | WWW | Account locked [locked]
                """)
        void lockThreshold(String id, String condition, String sequence, String expected, TestCase testCase) {
            String actual = loginSequence(sequence);
            testCase.record(id, condition, "attempts: " + describe(sequence), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
