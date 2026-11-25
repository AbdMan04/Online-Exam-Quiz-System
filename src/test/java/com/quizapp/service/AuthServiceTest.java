package com.quizapp.service;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - R1 to R5")
class AuthServiceTest {

    private static final String PASSWORD = "Secure123";
    private static final String WRONG_PASSWORD = "Wrong1234";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost keeps tests fast
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, encoder);
    }

    private User existingStudent() {
        return new User("Alice Smith", "alice@uni.edu", encoder.encode(PASSWORD), Role.STUDENT);
    }

    private static String messageOf(Executable action) {
        return assertThrows(BusinessException.class, action).getMessage();
    }

    @Nested
    @DisplayName("Register (R1-R3)")
    class Register {

        @Test
        @DisplayName("R1 valid data creates a STUDENT with a hashed password and normalized email")
        void validRegistration() {
            when(userRepository.existsByEmail("alice@uni.edu")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

            User user = authService.register("  Alice Smith ", " Alice@Uni.edu ", PASSWORD);

            assertAll(
                    () -> assertEquals("Alice Smith", user.getName()),
                    () -> assertEquals("alice@uni.edu", user.getEmail()),
                    () -> assertEquals(Role.STUDENT, user.getRole()),
                    () -> assertNotEquals(PASSWORD, user.getPasswordHash()),
                    () -> assertTrue(encoder.matches(PASSWORD, user.getPasswordHash()))
            );
        }

        @Test
        @DisplayName("R1 blank name is rejected")
        void blankName() {
            assertEquals(Messages.NAME_REQUIRED, messageOf(() -> authService.register(" ", "a@uni.edu", PASSWORD)));
        }

        @Test
        @DisplayName("R1 malformed email is rejected")
        void badEmail() {
            assertEquals(Messages.INVALID_EMAIL, messageOf(() -> authService.register("Al", "alice-uni.edu", PASSWORD)));
        }

        @Test
        @DisplayName("R2 password of length 7 is rejected")
        void shortPassword() {
            assertEquals(Messages.INVALID_PASSWORD, messageOf(() -> authService.register("Al", "a@uni.edu", "abc1234")));
        }

        @Test
        @DisplayName("R3 an email that already exists is rejected and nothing is saved")
        void duplicateEmail() {
            when(userRepository.existsByEmail("alice@uni.edu")).thenReturn(true);

            assertEquals(Messages.EMAIL_TAKEN, messageOf(() -> authService.register("Al", "ALICE@uni.edu", PASSWORD)));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login (R4-R5)")
    class Login {

        @Test
        @DisplayName("R4 correct credentials return the user and reset the failed counter")
        void successfulLogin() {
            User user = existingStudent();
            user.recordFailedLogin();
            when(userRepository.findByEmail("alice@uni.edu")).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            User result = authService.login("Alice@uni.edu", PASSWORD);

            assertEquals(Role.STUDENT, result.getRole());
            assertEquals(0, result.getFailedLoginAttempts());
        }

        @Test
        @DisplayName("R4 unknown email is rejected")
        void unknownEmail() {
            when(userRepository.findByEmail("nobody@uni.edu")).thenReturn(Optional.empty());

            assertEquals(Messages.BAD_CREDENTIALS, messageOf(() -> authService.login("nobody@uni.edu", PASSWORD)));
        }

        @Test
        @DisplayName("R4 wrong password is rejected and counted")
        void wrongPassword() {
            User user = existingStudent();
            when(userRepository.findByEmail("alice@uni.edu")).thenReturn(Optional.of(user));

            assertEquals(Messages.BAD_CREDENTIALS, messageOf(() -> authService.login("alice@uni.edu", WRONG_PASSWORD)));
            assertEquals(1, user.getFailedLoginAttempts());
            assertFalse(user.isLocked());
        }

        @Test
        @DisplayName("R5 the 3rd wrong password locks the account")
        void thirdFailureLocks() {
            User user = existingStudent();
            when(userRepository.findByEmail("alice@uni.edu")).thenReturn(Optional.of(user));

            messageOf(() -> authService.login("alice@uni.edu", WRONG_PASSWORD));
            messageOf(() -> authService.login("alice@uni.edu", WRONG_PASSWORD));
            String third = messageOf(() -> authService.login("alice@uni.edu", WRONG_PASSWORD));

            assertEquals(Messages.ACCOUNT_LOCKED, third);
            assertTrue(user.isLocked());
        }

        @Test
        @DisplayName("R5 a locked account rejects even the correct password")
        void lockedRejectsCorrectPassword() {
            User user = existingStudent();
            for (int i = 0; i < 3; i++) {
                user.recordFailedLogin();
            }
            when(userRepository.findByEmail("alice@uni.edu")).thenReturn(Optional.of(user));

            assertEquals(Messages.ACCOUNT_LOCKED, messageOf(() -> authService.login("alice@uni.edu", PASSWORD)));
        }
    }
}
