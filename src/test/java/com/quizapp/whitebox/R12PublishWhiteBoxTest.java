package com.quizapp.whitebox;

import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R12 white-box tests. Functions under test: {@code QuizService.publish} and {@code deleteQuestion}
 * (the "published quiz keeps one question" rule), {@code Quiz.publish} / {@code isPublished} /
 * {@code removeQuestion}, and the quiz-detail, publish and delete handlers of {@code InstructorController}.
 */
@DisplayName("R12 - Publish quiz (white-box)")
class R12PublishWhiteBoxTest extends QuizWhiteBoxSupport {

    /** Puts quiz 10 in the given state: number of questions and DRAFT/PUBLISHED. */
    private void givenQuiz(int questions, boolean published) {
        for (int i = 1; i <= questions; i++) {
            addSavedQuestion(i);
        }
        if (published) {
            quiz.publish();
        }
    }

    private String publish() {
        return ActualOutput.of(() -> "status " + quizService.publish(QUIZ_ID, INSTRUCTOR_ID).getStatus());
    }

    private String deleteQuestion1() {
        return ActualOutput.of(() -> {
            quizService.deleteQuestion(101L, INSTRUCTOR_ID);
            return "deleted (" + quiz.getQuestionCount() + " left)";
        });
    }

    private static String state(int questions, boolean published) {
        return (published ? "PUBLISHED" : "DRAFT") + " quiz with " + questions + " question(s)";
    }

    @Nested
    @DisplayName("Table 12-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-SC-01 | publish(): 'quiz.publish()' and save statements      | 1 | false | status PUBLISHED
                TC-R12-SC-02 | publish(): statement 'throw QUIZ_ALREADY_PUBLISHED'   | 1 | true  | Quiz is already published
                TC-R12-SC-03 | publish(): statement 'throw QUIZ_NEEDS_QUESTION'     | 0 | false | Quiz needs at least 1 question
                """)
        void publishStatements(String id, String condition, int questions, boolean published, String expected,
                               TestCase testCase) {
            givenQuiz(questions, published);
            String actual = publish();
            testCase.record(id, condition, "publish " + state(questions, published), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-SC-04 | deleteQuestion(): statement 'throw QUIZ_NEEDS_QUESTION'             | 1 | true  | Quiz needs at least 1 question
                TC-R12-SC-05 | deleteQuestion(): 'removeQuestion' and repository delete statements | 1 | false | deleted (0 left)
                """)
        void deleteStatements(String id, String condition, int questions, boolean published, String expected,
                              TestCase testCase) {
            givenQuiz(questions, published);
            String actual = deleteQuestion1();
            testCase.record(id, condition, "delete question 1 of " + state(questions, published), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-SC-06 | InstructorController.publish(): try-block   | 1 | view=redirect:/instructor/quizzes/10, flash message=Quiz published
                TC-R12-SC-07 | InstructorController.publish(): catch-block | 0 | view=redirect:/instructor/quizzes/10, flash error=Quiz needs at least 1 question
                """)
        void controllerPublish(String id, String condition, int questions, String expected, TestCase testCase) {
            givenQuiz(questions, false);
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
            String actual = "view=" + controller().publish(INSTRUCTOR_SESSION, QUIZ_ID, redirect) + ", " + flash(redirect);
            testCase.record(id, condition, "POST publish, " + state(questions, false), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-SC-08 | InstructorController.deleteQuestion(): try-block   | false | view=redirect:/instructor/quizzes/10, flash message=Question deleted
                TC-R12-SC-09 | InstructorController.deleteQuestion(): catch-block | true  | view=redirect:/instructor/quizzes/10, flash error=Quiz needs at least 1 question
                """)
        void controllerDelete(String id, String condition, boolean published, String expected, TestCase testCase) {
            givenQuiz(1, published);
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
            String actual = "view=" + controller().deleteQuestion(INSTRUCTOR_SESSION, 101L, redirect) + ", "
                    + flash(redirect);
            testCase.record(id, condition, "POST delete question 1, " + state(1, published), expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void quizDetail(TestCase testCase) {
            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller().quizDetail(INSTRUCTOR_SESSION, QUIZ_ID, model);
            String actual = "view=" + view + ", quiz=" + ((Quiz) model.get("quiz")).getTitle()
                    + ", locked=" + model.get("locked");
            String expected = "view=instructor/quiz-detail, quiz=Java Basics, locked=false";
            testCase.record("TC-R12-SC-10", "InstructorController.quizDetail(); QuizService.getQuiz() and hasAttempts()",
                    "GET /instructor/quizzes/10", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 12-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-BC-01 | publish: isPublished() -> TRUE                                  | 1 | true  | Quiz is already published
                TC-R12-BC-02 | publish: isPublished() -> FALSE, questionCount == 0 -> TRUE    | 0 | false | Quiz needs at least 1 question
                TC-R12-BC-03 | publish: questionCount == 0 -> FALSE                            | 2 | false | status PUBLISHED
                """)
        void publishBranches(String id, String condition, int questions, boolean published, String expected,
                             TestCase testCase) {
            givenQuiz(questions, published);
            String actual = publish();
            testCase.record(id, condition, "publish " + state(questions, published), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-BC-04 | deleteQuestion: isPublished() -> FALSE (count not checked)       | 1 | false | deleted (0 left)
                TC-R12-BC-05 | deleteQuestion: isPublished() -> TRUE, questionCount == 1 -> TRUE  | 1 | true  | Quiz needs at least 1 question
                TC-R12-BC-06 | deleteQuestion: isPublished() -> TRUE, questionCount == 1 -> FALSE | 2 | true  | deleted (1 left)
                """)
        void deleteBranches(String id, String condition, int questions, boolean published, String expected,
                            TestCase testCase) {
            givenQuiz(questions, published);
            String actual = deleteQuestion1();
            testCase.record(id, condition, "delete question 1 of " + state(questions, published), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
