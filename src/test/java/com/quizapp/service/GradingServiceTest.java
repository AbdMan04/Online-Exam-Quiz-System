package com.quizapp.service;

import com.quizapp.model.LetterGrade;
import com.quizapp.model.Outcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GradingService - R18 and R19")
class GradingServiceTest {

    private final GradingService grading = new GradingService();

    @Test
    @DisplayName("R18 15 of 20 points is 75.00 %")
    void gherkinExample() {
        assertEquals(new BigDecimal("75.00"), grading.calculateScore(15, 20));
    }

    @ParameterizedTest(name = "{0} of {1} points -> {2} %")
    @CsvSource({"0, 20, 0.00", "20, 20, 100.00", "1, 3, 33.33", "2, 3, 66.67"})
    @DisplayName("R18 scores are rounded half-up to 2 decimals")
    void rounding(int earned, int total, BigDecimal expected) {
        assertEquals(expected, grading.calculateScore(earned, total));
    }

    @Test
    @DisplayName("R18 impossible point totals are rejected")
    void invalidPoints() {
        assertThrows(IllegalArgumentException.class, () -> grading.calculateScore(1, 0));
        assertThrows(IllegalArgumentException.class, () -> grading.calculateScore(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> grading.calculateScore(21, 20));
    }

    @ParameterizedTest(name = "score {0} with passing score 60 -> {1}")
    @CsvSource({"59.99, FAIL", "60.00, PASS", "100, PASS", "0, FAIL"})
    @DisplayName("R19 PASS when the score reaches the passing score")
    void passFail(BigDecimal score, Outcome expected) {
        assertEquals(expected, grading.passFail(score, 60));
    }

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({"100, A", "90, A", "89.99, B", "80, B", "79.99, C", "70, C", "69.99, D", "60, D", "59.99, F", "0, F"})
    @DisplayName("R19 letter grade boundaries")
    void letterGrades(BigDecimal score, LetterGrade expected) {
        assertEquals(expected, grading.letterGrade(score));
    }

    @Test
    @DisplayName("R19 scores outside 0-100 are rejected")
    void invalidScores() {
        assertThrows(IllegalArgumentException.class, () -> grading.letterGrade(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> grading.letterGrade(new BigDecimal("100.01")));
        assertThrows(IllegalArgumentException.class, () -> grading.passFail(null, 60));
    }
}
