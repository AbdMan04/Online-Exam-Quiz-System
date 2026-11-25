package com.quizapp.junitfeatures;

import com.quizapp.service.GradingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R18 rounding cases kept in a data file ({@code src/test/resources/test-data/score-rounding.csv}),
 * so new cases can be added without touching Java code.
 *
 * <p>JUnit feature: {@code @CsvFileSource} (with a header line).
 */
@Tag("parameterized")
@DisplayName("R18 score rounding (cases from a CSV file)")
class ScoreRoundingFileTest {

    private final GradingService grading = new GradingService();

    @ParameterizedTest(name = "{0} of {1} points -> {2} % ({3})")
    @CsvFileSource(resources = "/test-data/score-rounding.csv", numLinesToSkip = 1)
    void scoreIsRoundedHalfUpToTwoDecimals(int earned, int total, BigDecimal expected, String note) {
        assertEquals(expected, grading.calculateScore(earned, total), note);
    }
}
