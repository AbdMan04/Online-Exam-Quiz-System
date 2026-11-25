package com.quizapp.blackbox;

import com.quizapp.model.Attempt;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R18: Score calculation.
 * Given a student answered questions worth 15 of 20 total points correctly, when the attempt is submitted,
 * then the score is 75 %.
 *
 * <p>Inputs: the points of each question (e.g. "10 5 5") and the student's answers per question
 * (R = right, W = wrong, - = unanswered).
 */
@DisplayName("R18 - Score calculation")
class R18ScoreCalculationTest extends QuizBlackBoxSupport {

    private String scoreOf(String points, String answers) {
        int[] questionPoints = Arrays.stream(points.trim().split("\\s+")).mapToInt(Integer::parseInt).toArray();
        Quiz quiz = publishedQuiz(30, 60, 1, questionPoints);
        Long student = newStudent();
        return ActualOutput.of(() -> {
            Attempt submitted = takeQuiz(quiz, student, answers);
            return "Score " + gradingService.grade(attemptService.getAttempt(submitted.getId(), student)).score() + " %";
        });
    }

    private static String describe(String points, String answers) {
        return "question points [" + points.trim() + "], answers " + answers;
    }

    @Nested
    @DisplayName("Table 18-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R18-EP-01 | Some answers right (15 of 20 points)    | 10 5 5 | RRW | Score 75.00 %
                TC-R18-EP-02 | All answers right                       | 10 5 5 | RRR | Score 100.00 %
                TC-R18-EP-03 | All answers wrong                       | 10 5 5 | WWW | Score 0.00 %
                TC-R18-EP-04 | Nothing answered                        | 10 5 5 | --- | Score 0.00 %
                TC-R18-EP-05 | Some right, rest unanswered (10 of 20)  | 10 5 5 | R-- | Score 50.00 %
                TC-R18-EP-06 | Score with a repeating decimal (1 of 3) | 1 1 1  | R-- | Score 33.33 %
                """)
        void partitions(String id, String condition, String points, String answers, String expected,
                        TestCase testCase) {
            String actual = scoreOf(points, answers);
            testCase.record(id, condition, describe(points, answers), expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 18-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R18-BVA-01 | Earned points = min (0 of 20)                  | 10 5 4 1      | WWWW  | Score 0.00 %
                TC-R18-BVA-02 | Earned points = min + 1 (1 of 20)              | 10 5 4 1      | WWWR  | Score 5.00 %
                TC-R18-BVA-03 | Earned points = max - 1 (19 of 20)             | 10 5 4 1      | RRRW  | Score 95.00 %
                TC-R18-BVA-04 | Earned points = max (20 of 20)                 | 10 5 4 1      | RRRR  | Score 100.00 %
                TC-R18-BVA-05 | Rounding: third decimal exactly 5 (1 of 32)    | 1 10 10 10 1  | R---- | Score 3.13 %
                TC-R18-BVA-06 | Rounding: third decimal below 5 (1 of 3 = 33.333) | 1 1 1      | R--   | Score 33.33 %
                """)
        void boundaries(String id, String condition, String points, String answers, String expected,
                        TestCase testCase) {
            String actual = scoreOf(points, answers);
            testCase.record(id, condition, describe(points, answers), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
