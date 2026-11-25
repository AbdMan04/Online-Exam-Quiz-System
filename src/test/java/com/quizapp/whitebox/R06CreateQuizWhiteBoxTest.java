package com.quizapp.whitebox;

import com.quizapp.dto.QuizInput;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.validation.QuizValidator;
import com.quizapp.web.QuizForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * R6 white-box tests. Functions under test: {@code QuizService.createQuiz} and {@code listQuizzes},
 * the decision chain of {@code QuizValidator.validate}, {@code QuizForm} and the instructor
 * dashboard / create-quiz handlers of {@code InstructorController}.
 */
@DisplayName("R6 - Create quiz (white-box)")
class R06CreateQuizWhiteBoxTest extends QuizWhiteBoxSupport {

    private static final QuizInput VALID = new QuizInput("Java Basics", 30, 60, 2);

    private static QuizForm form(String title, int duration, int passing, int attempts) {
        QuizForm form = new QuizForm();
        form.setTitle(title);
        form.setDurationMinutes(duration);
        form.setPassingScore(passing);
        form.setMaxAttempts(attempts);
        return form;
    }

    @Nested
    @DisplayName("Table 6-3: Statement coverage")
    class StatementCoverage {

        @Test
        void createQuizSuccess(TestCase testCase) {
            String actual = ActualOutput.of(() -> {
                Quiz created = quizService.createQuiz(INSTRUCTOR_ID, new QuizInput("  Java Basics ", 30, 60, 2));
                return "saved " + created.getStatus() + " quiz \"" + created.getTitle() + "\" ("
                        + created.getDurationMinutes() + " min, pass " + created.getPassingScore() + ", "
                        + created.getMaxAttempts() + " attempts) owned by instructor " + created.getInstructor().getId();
            });
            String expected = "saved DRAFT quiz \"Java Basics\" (30 min, pass 60, 2 attempts) owned by instructor 1";
            testCase.record("TC-R06-SC-01", "createQuiz(): validate, find instructor, create, save",
                    "instructor 1, title=\"  Java Basics \", 30, 60, 2", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void createQuizUnknownInstructor(TestCase testCase) {
            String actual;
            try {
                quizService.createQuiz(MISSING_ID, VALID);
                actual = "created";
            } catch (IllegalStateException e) {
                actual = "IllegalStateException: " + e.getMessage();
            }
            String expected = "IllegalStateException: Unknown instructor 999";
            testCase.record("TC-R06-SC-02", "createQuiz(): orElseThrow lambda for an unknown instructor id (programming error)",
                    "instructor 999 (not in database), valid settings", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void listQuizzes(TestCase testCase) {
            when(quizRepository.findByInstructorIdOrderByIdDesc(INSTRUCTOR_ID)).thenReturn(List.of(quiz));
            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller().dashboard(controller().currentUser(INSTRUCTOR_SESSION), model);
            String actual = "view=" + view + ", quizzes=" + ((List<?>) model.get("quizzes")).size();
            String expected = "view=instructor/dashboard, quizzes=1";
            testCase.record("TC-R06-SC-03", "InstructorController.currentUser() + dashboard(); QuizService.listQuizzes()",
                    "instructor 1 who owns 1 quiz", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void newQuizForm(TestCase testCase) {
            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller().newQuiz(model);
            QuizForm form = (QuizForm) model.get("quizForm");
            String actual = "view=" + view + ", defaults: title " + form.getTitle() + ", duration "
                    + form.getDurationMinutes() + ", passing " + form.getPassingScore() + ", attempts "
                    + form.getMaxAttempts();
            String expected = "view=instructor/quiz-form, defaults: title null, duration 30, passing 60, attempts 1";
            testCase.record("TC-R06-SC-04", "InstructorController.newQuiz(); QuizForm defaults and getters",
                    "GET /instructor/quizzes/new", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerCreateSuccess(TestCase testCase) {
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
            String view = controller().createQuiz(INSTRUCTOR_SESSION, form("Java Basics", 30, 60, 2),
                    new ExtendedModelMap(), redirect);
            String actual = "view=" + view + ", " + flash(redirect);
            String expected = "view=redirect:/instructor/quizzes/10, flash message=Quiz created";
            testCase.record("TC-R06-SC-05", "InstructorController.createQuiz(): try-block; QuizForm setters and toInput()",
                    "form: \"Java Basics\", 30, 60, 2", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerCreateError(TestCase testCase) {
            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller().createQuiz(INSTRUCTOR_SESSION, form("Ja", 30, 60, 2), model,
                    new RedirectAttributesModelMap());
            String actual = "view=" + view + ", error=" + model.get("error");
            String expected = "view=instructor/quiz-form, error=Invalid title length";
            testCase.record("TC-R06-SC-06", "InstructorController.createQuiz(): catch-block (form shown again with error)",
                    "form: \"Ja\", 30, 60, 2", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 6-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R06-BC-01 | validate: !isValidTitle -> TRUE                                       | Ja          | 30  | 60  | 2 | Invalid title length
                TC-R06-BC-02 | validate: !isValidTitle -> FALSE, duration out of range -> TRUE        | Java Basics | 300 | 60  | 2 | Duration must be 5-180 minutes
                TC-R06-BC-03 | validate: duration out of range -> FALSE, passing out of range -> TRUE | Java Basics | 30  | 150 | 2 | Passing score must be 0-100
                TC-R06-BC-04 | validate: passing out of range -> FALSE, attempts out of range -> TRUE | Java Basics | 30  | 60  | 9 | Max attempts must be 1-5
                TC-R06-BC-05 | validate: attempts out of range -> FALSE (all checks pass)            | Java Basics | 30  | 60  | 2 | valid
                """)
        void validateChain(String id, String condition, String title, int duration, int passing, int attempts,
                           String expected, TestCase testCase) {
            String actual = ActualOutput.of(() -> {
                QuizValidator.validate(new QuizInput(title, duration, passing, attempts));
                return "valid";
            });
            testCase.record(id, condition, "title=\"" + title + "\", duration=" + duration + ", passing=" + passing
                    + ", attempts=" + attempts, expected, actual);
            assertEquals(expected, actual);
        }
    }
}
