package com.quizapp.whitebox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Question;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.web.QuestionForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R13 white-box tests. Functions under test: {@code QuizService.ensureNoAttempts} (through add, edit and delete),
 * {@code editQuestion}, {@code getQuestion} / {@code findOwnedQuestion}, {@code hasAttempts},
 * {@code Question.update}, {@code QuestionForm.from}, and the edit handlers and error handler of
 * {@code InstructorController}.
 */
@DisplayName("R13 - Lock editing (white-box)")
class R13LockEditingWhiteBoxTest extends QuizWhiteBoxSupport {

    private static final QuestionInput EDITED = new QuestionInput("Edited", 4, List.of(
            new OptionInput("Yes", true), new OptionInput("No", false), new OptionInput("Maybe", false)));
    private static final QuestionInput INVALID = new QuestionInput("Edited", 4, List.of(new OptionInput("Yes", true)));

    private String change(String action, QuestionInput input) {
        return ActualOutput.of(() -> switch (action) {
            case "ADD" -> {
                quizService.addQuestion(QUIZ_ID, INSTRUCTOR_ID, input);
                yield "added";
            }
            case "EDIT" -> "updated: " + R10AddQuestionWhiteBoxTest.describe(
                    quizService.editQuestion(101L, INSTRUCTOR_ID, input));
            case "DELETE" -> {
                quizService.deleteQuestion(101L, INSTRUCTOR_ID);
                yield "deleted";
            }
            default -> throw new IllegalArgumentException(action);
        });
    }

    private static String describeForm(QuestionForm form) {
        return "options=" + form.getOptions() + ", correctIndex=" + form.getCorrectIndex();
    }

    @Nested
    @DisplayName("Table 13-3: Statement coverage")
    class StatementCoverage {

        @Test
        void editWithoutAttempts(TestCase testCase) {
            addSavedQuestion(1);
            String actual = change("EDIT", EDITED);
            String expected = "updated: \"Edited\" 4 pt, options [Yes (correct), No, Maybe]";
            testCase.record("TC-R13-SC-01", "editQuestion(): find, check attempts, validate, Question.update(), save",
                    "question 1, no attempts, new content \"Edited\" 4 pt [Yes*, No, Maybe]", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void editWithAttempts(TestCase testCase) {
            addSavedQuestion(1);
            studentsHaveAttempted();
            String actual = change("EDIT", EDITED);
            String expected = Messages.QUIZ_LOCKED;
            testCase.record("TC-R13-SC-02", "ensureNoAttempts(): statement 'throw QUIZ_LOCKED'",
                    "question 1, attempts exist", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void missingQuestion(TestCase testCase) {
            String actual = ActualOutput.of(() -> quizService.getQuestion(MISSING_ID, INSTRUCTOR_ID).getText());
            String expected = "Question not found";
            testCase.record("TC-R13-SC-03", "getQuestion() / findOwnedQuestion(): orElseThrow lambda",
                    "question 999 (does not exist)", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void hasAttempts(TestCase testCase) {
            studentsHaveAttempted();
            String actual = String.valueOf(quizService.hasAttempts(QUIZ_ID));
            testCase.record("TC-R13-SC-04", "hasAttempts(): return statement", "quiz 10, attempts exist", "true", actual);
            assertEquals("true", actual);
        }

        @Test
        void editForm(TestCase testCase) {
            addSavedQuestion(1);
            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller().editQuestionForm(INSTRUCTOR_SESSION, 101L, model);
            QuestionForm form = (QuestionForm) model.get("questionForm");
            String actual = "view=" + view + ", action=" + model.get("formAction") + ", text=" + form.getText();
            String expected = "view=instructor/question-form, action=/instructor/questions/101, text=Question 1";
            testCase.record("TC-R13-SC-05", "InstructorController.editQuestionForm(), editQuestionAction(), QuestionForm.from()",
                    "GET /instructor/questions/101/edit", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R13-SC-06 | InstructorController.editQuestion(): try-block   | false | view=redirect:/instructor/quizzes/10, flash message=Question updated
                TC-R13-SC-07 | InstructorController.editQuestion(): catch-block | true  | view=instructor/question-form, error=Quiz cannot be modified after attempts exist
                """)
        void controllerEdit(String id, String condition, boolean attempts, String expected, TestCase testCase) {
            addSavedQuestion(1);
            if (attempts) {
                studentsHaveAttempted();
            }
            ExtendedModelMap model = new ExtendedModelMap();
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
            String view = controller().editQuestion(INSTRUCTOR_SESSION, 101L,
                    R10AddQuestionWhiteBoxTest.form("Edited", 4, "Yes;No;;;", 0), model, redirect);
            String actual = "view=" + view + ", " + (model.containsAttribute("error")
                    ? "error=" + model.get("error") : flash(redirect));
            testCase.record(id, condition, "POST edit question 1, attempts exist=" + attempts, expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void errorHandler(TestCase testCase) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            FlashMap flashMap = new FlashMap();
            request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flashMap);
            String view = controller().backToDashboard(new BusinessException(Messages.QUIZ_NOT_FOUND), request);
            String actual = "view=" + view + ", flash error=" + flashMap.get("error");
            String expected = "view=redirect:/instructor/dashboard, flash error=Quiz not found";
            testCase.record("TC-R13-SC-08", "InstructorController.backToDashboard() exception handler",
                    "BusinessException(\"Quiz not found\") from any instructor page", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 13-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R13-BC-01 | ensureNoAttempts: existsByQuizId -> TRUE (add)                          | ADD    | true  | VALID   | Quiz cannot be modified after attempts exist
                TC-R13-BC-02 | ensureNoAttempts: existsByQuizId -> FALSE (add)                         | ADD    | false | VALID   | added
                TC-R13-BC-03 | ensureNoAttempts -> TRUE (edit; runs before validation of bad input)    | EDIT   | true  | INVALID | Quiz cannot be modified after attempts exist
                TC-R13-BC-04 | ensureNoAttempts -> TRUE (delete)                                       | DELETE | true  | VALID   | Quiz cannot be modified after attempts exist
                """)
        void lockBranches(String id, String condition, String action, boolean attempts, String inputKind,
                          String expected, TestCase testCase) {
            addSavedQuestion(1);
            if (attempts) {
                studentsHaveAttempted();
            }
            String actual = change(action, inputKind.equals("VALID") ? EDITED : INVALID);
            testCase.record(id, condition, action + " on quiz 10, attempts exist=" + attempts + ", input " + inputKind,
                    expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void notOwner(TestCase testCase) {
            addSavedQuestion(1);
            String actual = ActualOutput.of(() -> quizService.getQuestion(101L, OTHER_INSTRUCTOR_ID).getText());
            String expected = "Question not found";
            testCase.record("TC-R13-BC-05", "findOwnedQuestion: question.getQuiz().isOwnedBy(2) -> FALSE",
                    "question 1 of quiz 10 requested by instructor 2", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R13-BC-06 | QuestionForm.from: isCorrect() -> FALSE for option 1, TRUE for option 2      | 2 | 1 | options=[Option 1, Option 2, , , ], correctIndex=1
                TC-R13-BC-07 | QuestionForm.from: loop ends because i < existing.size() -> FALSE (3 of 5) | 3 | 0 | options=[Option 1, Option 2, Option 3, , ], correctIndex=0
                TC-R13-BC-08 | QuestionForm.from: loop ends because i < form size (5) -> FALSE            | 6 | 5 | options=[Option 1, Option 2, Option 3, Option 4, Option 5], correctIndex=null
                """)
        void fromBranches(String id, String condition, int optionCount, int correctIndex, String expected,
                          TestCase testCase) {
            Question question = Fixtures.question(101L, "Question 1", 5, optionCount, correctIndex);
            String actual = describeForm(QuestionForm.from(question));
            testCase.record(id, condition, "stored question with " + optionCount + " options, correct option index "
                    + correctIndex, expected, actual);
            assertEquals(expected, actual);
        }
    }
}
