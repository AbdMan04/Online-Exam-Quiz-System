package com.quizapp.junitfeatures;

import com.quizapp.dto.QuizAvailability;
import com.quizapp.model.Quiz;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R14/R15: the same attempt-limit rules must hold for every "max attempts" setting.
 *
 * <p>JUnit feature: {@code @ParameterizedClass} - the whole class (all its tests) runs once per value,
 * with the value injected into the {@code @Parameter} field.
 */
@Tag("parameterized")
@ParameterizedClass(name = "max attempts = {0}")
@ValueSource(ints = {1, 3, 5})
@DisplayName("R14/R15 attempt limit for each max-attempts setting")
class AttemptLimitPerSettingTest {

    @Parameter
    int maxAttempts;

    private QuizAvailability usage(long used, Long openAttemptId) {
        return new QuizAvailability(new Quiz("Java Basics", 30, 60, maxAttempts, null), used, openAttemptId);
    }

    @Test
    @DisplayName("Start is offered while attempts remain")
    void canStartWhileAttemptsRemain() {
        for (long used = 0; used < maxAttempts; used++) {
            assertTrue(usage(used, null).canStart(), used + " of " + maxAttempts + " used");
            assertEquals(maxAttempts - used, usage(used, null).attemptsLeft());
        }
    }

    @Test
    @DisplayName("Start is not offered once all attempts are used")
    void cannotStartWhenAllUsed() {
        assertFalse(usage(maxAttempts, null).canStart());
        assertEquals(0, usage(maxAttempts, null).attemptsLeft());
    }

    @Test
    @DisplayName("an open attempt can always be resumed, even if it is the last one")
    void openAttemptCanBeResumed() {
        assertTrue(usage(maxAttempts, 50L).canStart());
    }
}
