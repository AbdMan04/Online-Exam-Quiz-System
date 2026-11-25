package com.quizapp.junitfeatures;

import com.quizapp.model.LetterGrade;
import com.quizapp.model.Outcome;
import com.quizapp.service.GradingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R19 checked exhaustively: every whole score 0-100 gets the right letter grade and PASS/FAIL.
 * The expected values come from an independent table (the grade bands), not from the code under test.
 *
 * <p>JUnit features: {@code @TestFactory} generating {@link DynamicTest}s at run time, grouped in
 * {@link DynamicContainer}s (one container per letter grade).
 */
@Tag("exhaustive")
@DisplayName("R19 exhaustive: all scores 0-100")
class LetterGradeExhaustiveTest {

    private final GradingService grading = new GradingService();

    /** Independent oracle: the grade bands from the specification. */
    private enum Band {
        A(90, 100), B(80, 89), C(70, 79), D(60, 69), F(0, 59);

        final int from;
        final int to;

        Band(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    @TestFactory
    @DisplayName("letter grade for every whole score, grouped by band")
    Stream<DynamicContainer> letterGradePerBand() {
        return Arrays.stream(Band.values()).map(band -> DynamicContainer.dynamicContainer(
                "grade " + band + " (" + band.from + "-" + band.to + ")",
                IntStream.rangeClosed(band.from, band.to).mapToObj(score -> DynamicTest.dynamicTest(
                        "score " + score + " -> " + band,
                        () -> assertEquals(LetterGrade.valueOf(band.name()), grading.letterGrade(BigDecimal.valueOf(score)))))));
    }

    @TestFactory
    @DisplayName("PASS exactly when score >= 60")
    Stream<DynamicTest> passFailForEveryScore() {
        return IntStream.rangeClosed(0, 100).mapToObj(score -> {
            Outcome expected = score >= 60 ? Outcome.PASS : Outcome.FAIL;
            return DynamicTest.dynamicTest("score " + score + " -> " + expected,
                    () -> assertEquals(expected, grading.passFail(BigDecimal.valueOf(score), 60)));
        });
    }
}
