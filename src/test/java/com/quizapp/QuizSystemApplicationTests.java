package com.quizapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
@DisplayName("Application context (H2 test database)")
class QuizSystemApplicationTests {

    @Test
    @DisplayName("Spring context starts")
    void contextLoads() {
    }
}
