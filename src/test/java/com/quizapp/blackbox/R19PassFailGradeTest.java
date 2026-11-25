package com.quizapp.blackbox;

import com.quizapp.dto.GradeResult;
import com.quizapp.model.Attempt;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R19: Pass/fail and letter grade.
 * Given a score and the quiz passing score of 60, when the result is generated, then the result is "PASS"
 * if score >= 60, otherwise "FAIL", and the letter grade is A (>=90), B (80-89), C (70-79), D (60-69), or F (<60).
 *
 * <p>The quiz is worth 100 points (questions of 10x9, 5, 2, 1, 1, 1), so any whole-number score 0-100 can be
 * reached through the user interface. Sub-point boundaries such as 59.99 are covered by the white-box tests.
 */
@DisplayName("R19 - Pass/fail and letter grade")
class R19PassFailGradeTest extends QuizBlackBoxSupport {

    private static final int[] POINTS = {10, 10, 10, 10, 10, 10, 10, 10, 10, 5, 2, 1, 1, 1};

    /** Answers that earn exactly {@code target} of the 100 points (largest questions first). */
    private static String answersFor(int target) {
        StringBuilder answers = new StringBuilder();
        int remaining = target;
        for (int points : POINTS) {
            boolean takeIt = points <= remaining;
            answers.append(takeIt ? 'R' : 'W');
            remaining -= takeIt ? points : 0;
        }
        return answers.toString();
    }

    private String resultFor(int score, int passingScore) {
        Quiz quiz = publishedQuiz(30, passingScore, 1, POINTS);
        Long student = newStudent();
        return ActualOutput.of(() -> {
            Attempt submitted = takeQuiz(quiz, student, answersFor(score));
            GradeResult result = gradingService.grade(attemptService.getAttempt(submitted.getId(), student));
            return result.outcome() + ", grade " + result.letterGrade() + " (" + result.score() + " %)";
        });
    }

    @Nested
    @DisplayName("Table 19-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R19-EP-01 | Score in A range (90-100), passing 60 | 95 | 60 | PASS, grade A (95.00 %)
                TC-R19-EP-02 | Score in B range (80-89), passing 60  | 85 | 60 | PASS, grade B (85.00 %)
                TC-R19-EP-03 | Score in C range (70-79), passing 60  | 75 | 60 | PASS, grade C (75.00 %)
                TC-R19-EP-04 | Score in D range (60-69), passing 60  | 65 | 60 | PASS, grade D (65.00 %)
                TC-R19-EP-05 | Score in F range (below 60), passing 60 | 40 | 60 | FAIL, grade F (40.00 %)
                TC-R19-EP-06 | Score below a higher passing score (80) | 75 | 80 | FAIL, grade C (75.00 %)
                TC-R19-EP-07 | Passing score 0, score 0              | 0  | 0  | PASS, grade F (0.00 %)
                """)
        void partitions(String id, String condition, int score, int passing, String expected, TestCase testCase) {
            String actual = resultFor(score, passing);
            testCase.record(id, condition, "score " + score + " of 100 points, passing score " + passing, expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 19-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R19-BVA-01 | Score = min (0)                    | 0   | FAIL, grade F (0.00 %)
                TC-R19-BVA-02 | Score = passing - 1 = D min - 1 (59) | 59 | FAIL, grade F (59.00 %)
                TC-R19-BVA-03 | Score = passing = D min (60)       | 60  | PASS, grade D (60.00 %)
                TC-R19-BVA-04 | Score = C min - 1 (69)             | 69  | PASS, grade D (69.00 %)
                TC-R19-BVA-05 | Score = C min (70)                 | 70  | PASS, grade C (70.00 %)
                TC-R19-BVA-06 | Score = B min - 1 (79)             | 79  | PASS, grade C (79.00 %)
                TC-R19-BVA-07 | Score = B min (80)                 | 80  | PASS, grade B (80.00 %)
                TC-R19-BVA-08 | Score = A min - 1 (89)             | 89  | PASS, grade B (89.00 %)
                TC-R19-BVA-09 | Score = A min (90)                 | 90  | PASS, grade A (90.00 %)
                TC-R19-BVA-10 | Score = max (100)                  | 100 | PASS, grade A (100.00 %)
                """)
        void boundaries(String id, String condition, int score, String expected, TestCase testCase) {
            String actual = resultFor(score, 60);
            testCase.record(id, condition, "score " + score + " of 100 points, passing score 60", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
