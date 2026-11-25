package com.quizapp.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

/**
 * Replaces the application's system clock with a {@link MutableClock} in black-box tests.
 */
@TestConfiguration
public class TestClockConfiguration {

    public static final LocalDateTime START = LocalDateTime.of(2026, 11, 25, 10, 0);

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(START);
    }
}
