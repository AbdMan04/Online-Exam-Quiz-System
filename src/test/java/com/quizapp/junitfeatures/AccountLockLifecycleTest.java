package com.quizapp.junitfeatures;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AuthService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R4/R5 as one story on ONE shared account: two wrong passwords keep it open, the third locks it,
 * and then even the correct password is refused.
 *
 * <p>JUnit features: {@code @TestInstance(PER_CLASS)} (one test object shared by all methods, so the account's
 * state carries over), non-static {@code @BeforeAll} / {@code @AfterAll}, {@code @TestMethodOrder} with
 * {@code @Order}, {@code @RepeatedTest} with {@link RepetitionInfo}, and injected {@link TestInfo} /
 * {@link TestReporter}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("lifecycle")
@DisplayName("R4/R5 lock lifecycle on one shared account")
class AccountLockLifecycleTest {

    private static final String PASSWORD = "Secure123";

    private UserRepository userRepository;
    private AuthService authService;
    private User alice;

    @BeforeAll
    void createOneAccountForTheWholeClass() {
        PasswordEncoder encoder = new BCryptPasswordEncoder(4);
        alice = new User("Alice Smith", "alice@uni.edu", encoder.encode(PASSWORD), Role.STUDENT);
        userRepository = mock(UserRepository.class);
        when(userRepository.findByEmail("alice@uni.edu")).thenReturn(Optional.of(alice));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        authService = new AuthService(userRepository, encoder);
    }

    private String login(String password) {
        return assertThrows(BusinessException.class, () -> authService.login("alice@uni.edu", password)).getMessage();
    }

    @Order(1)
    @RepeatedTest(value = 2, name = "wrong password {currentRepetition} of {totalRepetitions} keeps the account open")
    void firstTwoWrongPasswords(RepetitionInfo repetition, TestReporter reporter) {
        assertEquals(Messages.BAD_CREDENTIALS, login("Wrong1234"));
        assertEquals(repetition.getCurrentRepetition(), alice.getFailedLoginAttempts());
        assertFalse(alice.isLocked());
        reporter.publishEntry("failedLogins", String.valueOf(alice.getFailedLoginAttempts()));
    }

    @Order(2)
    @Test
    @DisplayName("the third wrong password locks the account")
    void thirdWrongPasswordLocks(TestInfo testInfo, TestReporter reporter) {
        assertEquals(Messages.ACCOUNT_LOCKED, login("Wrong1234"));
        assertTrue(alice.isLocked());
        reporter.publishEntry(testInfo.getDisplayName(), "locked after " + alice.getFailedLoginAttempts() + " failures");
    }

    @Order(3)
    @Test
    @DisplayName("after locking, even the correct password is refused")
    void correctPasswordRefusedWhenLocked() {
        assertEquals(Messages.ACCOUNT_LOCKED, login(PASSWORD));
    }

    @AfterAll
    void everyFailureWasSaved() {
        assertEquals(3, alice.getFailedLoginAttempts(), "the correct password after locking is not counted");
        verify(userRepository, atLeast(3)).save(alice);
    }
}
