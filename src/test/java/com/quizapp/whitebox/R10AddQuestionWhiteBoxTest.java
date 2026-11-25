package com.quizapp.whitebox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.web.QuestionForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R10 white-box tests. Functions under test: {@code QuizService.addQuestion} (with findOwnedQuiz and
 * toOptions), {@code Quiz.addQuestion} / {@code isOwnedBy}, {@code QuestionForm.toInput} and accessors,
 * and the add-question handlers of {@code InstructorController}.
 */
@DisplayName("R10 - Add question (white-box)")
class R10AddQuestionWhiteBoxTest extends QuizWhiteBoxSupport {

    private static final QuestionInput VALID = new QuestionInput("  What is the JVM? ", 5, List.of(
            new OptionInput(" A virtual machine ", true), new OptionInput("A compiler", false),
            new OptionInput("An IDE", false), new OptionInput("A database", false)));

    static String describe(QuestionInput input) {
        return input.options().stream()
                .map(option -> option.text() + (option.correct() ? " (correct)" : ""))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    static String describe(Question question) {
        return "\"" + question.getText() + "\" " + question.getPoints() + " pt, options " + question.getOptions().stream()
                .map(option -> option.getText() + (option.isCorrect() ? " (correct)" : ""))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /** A filled-in question form; in {@code options} "NULL" is a missing field and "BLANK" a field of spaces. */
    static QuestionForm form(String text, int points, String options, Integer correctIndex) {
        QuestionForm form = new QuestionForm();
        form.setText(text);
        form.setPoints(points);
        form.setOptions(new ArrayList<>(Arrays.stream(options.split(";", -1))
                .map(R10AddQuestionWhiteBoxTest::field).toList()));
        form.setCorrectIndex(correctIndex);
        return form;
    }

    private static String field(String spec) {
        return switch (spec) {
            case "NULL" -> null;
            case "BLANK" -> "   ";
            default -> spec;
        };
    }

    @Nested
    @DisplayName("Table 10-3: Statement coverage")
    class StatementCoverage {

        @Test
        void addQuestionSuccess(TestCase testCase) {
            String actual = ActualOutput.of(() -> {
                Question added = quizService.addQuestion(QUIZ_ID, INSTRUCTOR_ID, VALID);
                return "added " + describe(added) + "; quiz has " + quiz.getQuestions().size() + " question(s), "
                        + "question belongs to quiz " + added.getQuiz().getId();
            });
            String expected = "added \"What is the JVM?\" 5 pt, options [A virtual machine (correct), A compiler, "
                    + "An IDE, A database]; quiz has 1 question(s), question belongs to quiz 10";
            testCase.record("TC-R10-SC-01",
                    "addQuestion(): find quiz, check attempts, validate, trim texts (toOptions), attach, save",
                    "quiz 10 (DRAFT), text=\"  What is the JVM? \", options " + describe(VALID) + ", 5 points",
                    expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void addQuestionMissingQuiz(TestCase testCase) {
            String actual = ActualOutput.of(() -> {
                quizService.addQuestion(MISSING_ID, INSTRUCTOR_ID, VALID);
                return "added";
            });
            String expected = "Quiz not found";
            testCase.record("TC-R10-SC-02", "findOwnedQuiz(): orElseThrow lambda 'throw QUIZ_NOT_FOUND'",
                    "quiz 999 (does not exist), valid question", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void newQuestionForm(TestCase testCase) {
            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller().newQuestion(INSTRUCTOR_SESSION, QUIZ_ID, model);
            QuestionForm form = (QuestionForm) model.get("questionForm");
            String actual = "view=" + view + ", action=" + model.get("formAction") + ", option fields="
                    + form.getOptions().size();
            String expected = "view=instructor/question-form, action=/instructor/quizzes/10/questions, option fields=5";
            testCase.record("TC-R10-SC-03", "InstructorController.newQuestion(), showQuestionForm(), addQuestionAction()",
                    "GET /instructor/quizzes/10/questions/new", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerAddSuccess(TestCase testCase) {
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
            String view = controller().addQuestion(INSTRUCTOR_SESSION, QUIZ_ID,
                    form("What is the JVM?", 5, "A virtual machine;A compiler;;;", 0), new ExtendedModelMap(), redirect);
            String actual = "view=" + view + ", " + flash(redirect);
            String expected = "view=redirect:/instructor/quizzes/10, flash message=Question added";
            testCase.record("TC-R10-SC-04", "InstructorController.addQuestion(): try-block",
                    "form: \"What is the JVM?\", 5 pt, options [A virtual machine, A compiler], correct=0", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void controllerAddError(TestCase testCase) {
            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller().addQuestion(INSTRUCTOR_SESSION, QUIZ_ID,
                    form("What is the JVM?", 5, "A virtual machine;;;;", 0), model, new RedirectAttributesModelMap());
            String actual = "view=" + view + ", error=" + model.get("error") + ", action=" + model.get("formAction");
            String expected = "view=instructor/question-form, error=Invalid question configuration, "
                    + "action=/instructor/quizzes/10/questions";
            testCase.record("TC-R10-SC-05", "InstructorController.addQuestion(): catch-block (form shown again)",
                    "form: \"What is the JVM?\", 5 pt, only 1 option", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void formAccessors(TestCase testCase) {
            QuestionForm form = form("Q?", 7, "A;B;;;", 1);
            String actual = "text=" + form.getText() + ", points=" + form.getPoints() + ", options="
                    + form.getOptions() + ", correctIndex=" + form.getCorrectIndex();
            String expected = "text=Q?, points=7, options=[A, B, , , ], correctIndex=1";
            testCase.record("TC-R10-SC-06", "QuestionForm setters and getters (used by the page)",
                    "text=\"Q?\", points=7, options A;B;;;, correctIndex=1", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 10-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R10-BC-01 | isOwnedBy: instructor != null -> FALSE                       | NO_INSTRUCTOR | false
                TC-R10-BC-02 | instructor != null -> TRUE, instructor.getId() != null -> FALSE | NO_ID      | false
                TC-R10-BC-03 | getId() != null -> TRUE, getId().equals(1) -> FALSE          | OTHER        | false
                TC-R10-BC-04 | getId() != null -> TRUE, getId().equals(1) -> TRUE           | OWNER        | true
                """)
        void isOwnedByBranches(String id, String condition, String owner, String expected, TestCase testCase) {
            User quizOwner = switch (owner) {
                case "NO_INSTRUCTOR" -> null;
                case "NO_ID" -> new User("Unsaved", "unsaved@uni.edu", "hash", Role.INSTRUCTOR);
                case "OTHER" -> Fixtures.user(OTHER_INSTRUCTOR_ID, "Other", Role.INSTRUCTOR);
                default -> instructor;
            };
            String actual = String.valueOf(new Quiz("Java Basics", 30, 60, 2, quizOwner).isOwnedBy(INSTRUCTOR_ID));
            testCase.record(id, condition, "quiz owner: " + owner + ", isOwnedBy(1)", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NONE", textBlock = """
                TC-R10-BC-05 | toInput: option text != null -> FALSE (field missing, skipped)          | NULL;Yes;No | 1    | [Yes (correct), No]
                TC-R10-BC-06 | toInput: != null -> TRUE, !isBlank -> FALSE (blank field, skipped)     | BLANK;Yes;No | 1    | [Yes (correct), No]
                TC-R10-BC-07 | toInput: filled field kept; equals(correctIndex, i) TRUE for i=0 only   | Yes;No      | 0    | [Yes (correct), No]
                TC-R10-BC-08 | toInput: filled fields kept; no radio chosen (correctIndex null)        | Yes;No      | NONE | [Yes, No]
                """)
        void toInputBranches(String id, String condition, String options, Integer correctIndex, String expected,
                             TestCase testCase) {
            String actual = describe(form("Q?", 5, options, correctIndex).toInput());
            testCase.record(id, condition, "option fields: " + options + ", correctIndex=" + correctIndex,
                    expected, actual);
            assertEquals(expected, actual);
        }
    }
}
