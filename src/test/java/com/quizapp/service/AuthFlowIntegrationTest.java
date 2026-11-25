package com.quizapp.service;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Tag("integration")
@DisplayName("R1-R5 end to end on the H2 database")
class AuthFlowIntegrationTest {

    private static final String EMAIL = "bob@uni.edu";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void removeTestUser() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("register, log in, then lock after 3 wrong passwords (state is persisted)")
    void registerLoginAndLock() {
        authService.register("Bob Lee", EMAIL, "Secure123");
        assertEquals(Role.STUDENT, authService.login(EMAIL, "Secure123").getRole());

        for (int i = 0; i < 3; i++) {
            assertThrows(BusinessException.class, () -> authService.login(EMAIL, "Wrong1234"));
        }

        User stored = userRepository.findByEmail(EMAIL).orElseThrow();
        assertTrue(stored.isLocked());
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(EMAIL, "Secure123"));
        assertEquals(Messages.ACCOUNT_LOCKED, ex.getMessage());
    }

    @Test
    @DisplayName("the seeded instructor can log in")
    void seededInstructor() {
        assertEquals(Role.INSTRUCTOR, authService.login("instructor@uni.edu", "Teach1234").getRole());
    }
}
