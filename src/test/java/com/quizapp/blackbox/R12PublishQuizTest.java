package com.quizapp.blackbox;

import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R12: Publish quiz.
 * Given a DRAFT quiz with at least 1 question, when the instructor publishes it, then the status becomes
 * "PUBLISHED", and a quiz with 0 questions is rejected with "Quiz needs at least 1 question".
 */
@DisplayName("R12 - Publish quiz")
class R12PublishQuizTest extends QuizBlackBoxSupport {

    private String publish(Long quizId, Long instructorId) {
        return ActualOutput.of(() -> "Status " + quizService.publish(quizId, instructorId).getStatus());
    }

    private String deleteFirstQuestion(Quiz quiz) {
        return ActualOutput.of(() -> {
            quizService.deleteQuestion(firstQuestion(quiz).getId(), instructorId());
            return "Question deleted (" + quizService.getQuiz(quiz.getId(), instructorId()).getQuestionCount() + " left)";
        });
    }

    @Nested
    @DisplayName("Table 12-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-EP-01 | DRAFT quiz with questions               | DRAFT     | 3 | OWN   | Status PUBLISHED
                TC-R12-EP-02 | DRAFT quiz without questions            | DRAFT     | 0 | OWN   | Quiz needs at least 1 question
                TC-R12-EP-03 | Quiz already PUBLISHED                  | PUBLISHED | 3 | OWN   | Quiz is already published
                TC-R12-EP-04 | Quiz of another instructor              | DRAFT     | 3 | OTHER | Quiz not found
                """)
        void publishPartitions(String id, String condition, String state, int questions, String owner, String expected,
                               TestCase testCase) {
            Quiz quiz = "PUBLISHED".equals(state) ? publishedQuizWithQuestions(questions) : draftQuizWithQuestions(questions);
            Long instructor = "OTHER".equals(owner) ? otherInstructorId() : instructorId();

            String actual = publish(quiz.getId(), instructor);
            testCase.record(id, condition, "publish " + state + " quiz with " + questions + " question(s), "
                    + owner.toLowerCase() + " instructor", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-EP-05 | Delete the only question of a PUBLISHED quiz | Quiz needs at least 1 question
                """)
        void deleteLastPublishedQuestion(String id, String condition, String expected, TestCase testCase) {
            String actual = deleteFirstQuestion(publishedQuizWithQuestions(1));
            testCase.record(id, condition, "delete question 1 of PUBLISHED quiz with 1 question", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 12-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-BVA-01 | Questions = min - 1 (0) when publishing | 0 | Quiz needs at least 1 question
                TC-R12-BVA-02 | Questions = min (1) when publishing     | 1 | Status PUBLISHED
                """)
        void questionCountOnPublish(String id, String condition, int questions, String expected, TestCase testCase) {
            String actual = publish(draftQuizWithQuestions(questions).getId(), instructorId());
            testCase.record(id, condition, "publish DRAFT quiz with " + questions + " question(s)", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R12-BVA-03 | PUBLISHED quiz, questions left after delete = min - 1 (0) | 1 | Quiz needs at least 1 question
                TC-R12-BVA-04 | PUBLISHED quiz, questions left after delete = min (1)     | 2 | Question deleted (1 left)
                """)
        void questionsLeftAfterDelete(String id, String condition, int questions, String expected, TestCase testCase) {
            String actual = deleteFirstQuestion(publishedQuizWithQuestions(questions));
            testCase.record(id, condition, "delete 1 question of PUBLISHED quiz with " + questions + " question(s)",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }
}
