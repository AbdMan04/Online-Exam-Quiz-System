package com.quizapp.whitebox;

import com.quizapp.dto.QuizReport;
import com.quizapp.model.Attempt;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.service.QuizService;
import com.quizapp.service.ReportService;
import com.quizapp.support.TestCase;
import com.quizapp.web.InstructorController;
import com.quizapp.web.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * R20 white-box tests. Functions under test: {@code ReportService.quizReport} (including the
 * "submitted only" filter), {@code bestScore}, {@code classAverage}, {@code QuizReport.passedCount} and
 * {@code InstructorController.report}. Quiz 10 has two 5-point questions; passing score 60.
 */
@DisplayName("R20 - Best attempt and statistics (white-box)")
class R20ReportWhiteBoxTest extends AttemptWhiteBoxSupport {

    private ReportService reportService;
    private QuizService quizService;
    private User bob;

    @BeforeEach
    void createReportService() {
        quizService = new QuizService(quizRepository, questionRepository, attemptRepository, userRepository);
        reportService = new ReportService(quizService, attemptService, gradingService, attemptRepository);
        bob = Fixtures.user(7L, "Bob", Role.STUDENT);
    }

    /** A submitted attempt; {@code answers} has one letter per question: R = right, W = wrong. */
    private Attempt submitted(User student, String answers) {
        Attempt attempt = new Attempt(quiz, student, T0);
        attempt.recordAnswer(question1, question1.getOptions().get(answers.charAt(0) == 'R' ? 0 : 1));
        attempt.recordAnswer(question2, question2.getOptions().get(answers.charAt(1) == 'R' ? 0 : 1));
        attempt.submit(T0.plusMinutes(10));
        return attempt;
    }

    private String report(Attempt... attempts) {
        when(attemptRepository.findByQuizIdOrderByStartedAtAsc(QUIZ_ID)).thenReturn(List.of(attempts));
        QuizReport report = reportService.quizReport(QUIZ_ID, INSTRUCTOR_ID);
        String rows = report.students().stream()
                .map(row -> row.studentName() + ": " + row.bestScore() + " " + row.outcome() + " " + row.letterGrade()
                        + " (" + row.submittedAttempts() + ")")
                .collect(Collectors.joining("; "));
        return rows + "; average " + report.classAverage() + "; passed " + report.passedCount() + " of "
                + report.students().size();
    }

    private static List<BigDecimal> scores(String list) {
        if (list.equals("NULL")) {
            return null;
        }
        return list.isBlank() ? List.of() : Arrays.stream(list.split(" ")).map(BigDecimal::new).toList();
    }

    private static String run(Supplier<Object> action) {
        try {
            return String.valueOf(action.get());
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException: " + e.getMessage();
        }
    }

    @Nested
    @DisplayName("Table 20-3: Statement coverage")
    class StatementCoverage {

        @Test
        void fullReport(TestCase testCase) {
            String actual = report(submitted(student, "RW"), submitted(bob, "RW"), submitted(student, "RR"));
            String expected = "Alice: 100.00 PASS A (2); Bob: 50.00 FAIL F (1); average 75.00; passed 1 of 2";
            testCase.record("TC-R20-SC-01",
                    "quizReport(): checkExpiry each, keep submitted, group by student, studentResult(), sort, classAverage()",
                    "Alice: 50 then 100; Bob: 50", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', emptyValue = "", textBlock = """
                TC-R20-SC-02 | bestScore(): statement 'throw' for no scores  | BEST    | '' | IllegalArgumentException: A best score needs at least one score
                TC-R20-SC-03 | classAverage(): statement 'return 0.00'       | AVERAGE | '' | 0.00
                """)
        void emptyLists(String id, String condition, String function, String list, String expected,
                        TestCase testCase) {
            String actual = run(() -> function.equals("BEST")
                    ? reportService.bestScore(scores(list)) : reportService.classAverage(scores(list)));
            testCase.record(id, condition, function.toLowerCase() + " of [" + list + "]", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void reportPage(TestCase testCase) {
            when(attemptRepository.findByQuizIdOrderByStartedAtAsc(QUIZ_ID))
                    .thenReturn(List.of(submitted(student, "RR"), submitted(bob, "WW")));
            ExtendedModelMap model = new ExtendedModelMap();
            SessionUser instructorSession = new SessionUser(INSTRUCTOR_ID, "Dr. Smith", Role.INSTRUCTOR);
            String view = new InstructorController(quizService, reportService).report(instructorSession, QUIZ_ID, model);
            String actual = "view=" + view + ", students=" + ((QuizReport) model.get("report")).students().size();
            String expected = "view=instructor/report, students=2";
            testCase.record("TC-R20-SC-04", "InstructorController.report()", "GET /instructor/quizzes/10/report",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 20-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', emptyValue = "", textBlock = """
                TC-R20-BC-01 | bestScore: scores == null -> TRUE                        | BEST    | NULL           | IllegalArgumentException: A best score needs at least one score
                TC-R20-BC-02 | bestScore: == null -> FALSE, isEmpty() -> TRUE           | BEST    | ''             | IllegalArgumentException: A best score needs at least one score
                TC-R20-BC-03 | bestScore: isEmpty() -> FALSE (highest returned)         | BEST    | 40.00 80.00 60.00 | 80.00
                TC-R20-BC-04 | classAverage: bestScores == null -> TRUE                 | AVERAGE | NULL           | 0.00
                TC-R20-BC-05 | classAverage: == null -> FALSE, isEmpty() -> TRUE        | AVERAGE | ''             | 0.00
                TC-R20-BC-06 | classAverage: isEmpty() -> FALSE (mean, half-up)         | AVERAGE | 100.00 50.00 50.00 | 66.67
                """)
        void listBranches(String id, String condition, String function, String list, String expected,
                          TestCase testCase) {
            String actual = run(() -> function.equals("BEST")
                    ? reportService.bestScore(scores(list)) : reportService.classAverage(scores(list)));
            testCase.record(id, condition, function.toLowerCase() + " of [" + list + "]", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void submittedFilter(TestCase testCase) {
            Attempt running = new Attempt(quiz, bob, T0); // started 10:00, clock at 10:00 -> still running
            String actual = report(submitted(student, "RR"), running);
            String expected = "Alice: 100.00 PASS A (1); average 100.00; passed 1 of 1";
            testCase.record("TC-R20-BC-07",
                    "quizReport filter: !isInProgress() -> TRUE (Alice, submitted) and FALSE (Bob, still running)",
                    "Alice submitted 100; Bob's attempt is still running", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void passedCount(TestCase testCase) {
            String actual = report(submitted(student, "RR"), submitted(bob, "WW"));
            String expected = "Alice: 100.00 PASS A (1); Bob: 0.00 FAIL F (1); average 50.00; passed 1 of 2";
            testCase.record("TC-R20-BC-08",
                    "passedCount: outcome == PASS -> TRUE (Alice) and FALSE (Bob)",
                    "Alice 100, Bob 0, passing 60", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
