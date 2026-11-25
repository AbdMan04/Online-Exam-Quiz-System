package com.quizapp.blackbox;

import com.quizapp.dto.QuizReport;
import com.quizapp.dto.StudentResult;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.TestClockConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R20: Best attempt and statistics.
 * Given a student has multiple attempts on a quiz, when the instructor views the report, then the student's
 * highest score is reported, and the class average is calculated from all students' best scores.
 *
 * <p>Input notation: each student's submitted scores separated by "/", students separated by ";",
 * e.g. "40/80; 50" = student 1 scored 40 then 80, student 2 scored 50. The quiz has 10 questions of
 * 10 points, max 3 attempts, 30 minutes.
 */
@DisplayName("R20 - Best attempt and statistics")
class R20BestAttemptAndStatisticsTest extends QuizBlackBoxSupport {

    private Quiz newQuiz() {
        return publishedQuiz(30, 60, 3, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10);
    }

    /** Answers earning {@code score} % on the 10 x 10-point quiz. */
    private static String answersFor(int score) {
        int right = score / 10;
        return "R".repeat(right) + "W".repeat(10 - right);
    }

    /** Every listed student takes the quiz with the given scores ("40/80; 50"). Empty = no attempts. */
    private void studentsTake(Quiz quiz, String scores) {
        if (scores.isBlank()) {
            return;
        }
        for (String studentScores : scores.split(";")) {
            Long student = newStudent();
            for (String score : studentScores.trim().split("/")) {
                takeQuiz(quiz, student, answersFor(Integer.parseInt(score.trim())));
            }
        }
    }

    /** The instructor's report as one line: "Student 1: 80.00 (2 attempts); Student 2: ..., average 65.00". */
    private String report(Quiz quiz) {
        return ActualOutput.of(() -> {
            QuizReport report = reportService.quizReport(quiz.getId(), instructorId());
            String rows = report.students().isEmpty() ? "no students" : report.students().stream()
                    .map(R20BestAttemptAndStatisticsTest::row)
                    .collect(Collectors.joining("; "));
            return rows + ", average " + report.classAverage();
        });
    }

    private static String row(StudentResult result) {
        int n = result.submittedAttempts();
        return result.studentName() + ": " + result.bestScore() + " (" + n + (n == 1 ? " attempt)" : " attempts)");
    }

    @Nested
    @DisplayName("Table 20-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', emptyValue = "", textBlock = """
                TC-R20-EP-01 | One student, best attempt is the middle one | 40/80/60      | Student 1: 80.00 (3 attempts), average 80.00
                TC-R20-EP-02 | One student, best attempt is the first one  | 80/40         | Student 1: 80.00 (2 attempts), average 80.00
                TC-R20-EP-03 | Several students, average of best scores    | 40/80; 50     | Student 1: 80.00 (2 attempts); Student 2: 50.00 (1 attempt), average 65.00
                TC-R20-EP-04 | Average needs rounding (100, 50, 50)        | 100; 50; 50   | Student 1: 100.00 (1 attempt); Student 2: 50.00 (1 attempt); Student 3: 50.00 (1 attempt), average 66.67
                TC-R20-EP-05 | No attempts at all                          | ''            | no students, average 0.00
                """)
        void partitions(String id, String condition, String scores, String expected, TestCase testCase) {
            Quiz quiz = newQuiz();
            studentsTake(quiz, scores);
            String actual = report(quiz);
            testCase.record(id, condition, "scores: " + (scores.isBlank() ? "none" : scores), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R20-EP-06 | A student whose only attempt is still running is not listed | Student 1: 70.00 (1 attempt), average 70.00
                """)
        void runningAttemptNotCounted(String id, String condition, String expected, TestCase testCase) {
            Quiz quiz = newQuiz();
            studentsTake(quiz, "70");
            answer(quiz, newStudent(), answersFor(90)); // student 2 is still answering
            String actual = report(quiz);
            testCase.record(id, condition, "student 1 submitted 70; student 2 answered 90 but has not submitted (time left)",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 20-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', emptyValue = "", textBlock = """
                TC-R20-BVA-01 | Students with a submitted attempt = min - 1 (0) | ''  | no students, average 0.00
                TC-R20-BVA-02 | Students with a submitted attempt = min (1)     | 70  | Student 1: 70.00 (1 attempt), average 70.00
                TC-R20-BVA-03 | Attempts of a student = max (3)                 | 30/90/60 | Student 1: 90.00 (3 attempts), average 90.00
                """)
        void studentCounts(String id, String condition, String scores, String expected, TestCase testCase) {
            Quiz quiz = newQuiz();
            studentsTake(quiz, scores);
            String actual = report(quiz);
            testCase.record(id, condition, "scores: " + (scores.isBlank() ? "none" : scores), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R20-BVA-04 | Unsubmitted attempt, report at deadline - 1 s (10:29:59) | 29 | 59 | Student 1: 70.00 (1 attempt), average 70.00
                TC-R20-BVA-05 | Unsubmitted attempt, report at deadline (10:30:00)       | 30 | 0  | Student 1: 70.00 (1 attempt); Student 2: 90.00 (1 attempt), average 80.00
                """)
        void expiryBeforeReport(String id, String condition, int minute, int second, String expected,
                                TestCase testCase) {
            Quiz quiz = newQuiz();
            studentsTake(quiz, "70");
            answer(quiz, newStudent(), answersFor(90)); // student 2 never presses Submit
            clock.setTo(TestClockConfiguration.START.plusMinutes(minute).plusSeconds(second));
            String actual = report(quiz);
            testCase.record(id, condition, String.format("student 1 submitted 70; student 2 answered 90 and never "
                    + "submitted; report opened at 10:%02d:%02d", minute, second), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
