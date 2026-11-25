package com.quizapp.blackbox;

import com.quizapp.model.Quiz;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R16: Unpublished quiz.
 * Given a quiz with status "DRAFT", when a student tries to start it, then the system rejects it with
 * "Quiz not available".
 */
@DisplayName("R16 - Unpublished quiz")
class R16UnpublishedQuizTest extends AttemptSupport {

    @Nested
    @DisplayName("Table 16-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R16-EP-01 | Quiz is DRAFT              | DRAFT     | Quiz not available
                TC-R16-EP-02 | Quiz is PUBLISHED          | PUBLISHED | Attempt started, 1800 s left
                TC-R16-EP-03 | Quiz does not exist        | MISSING   | Quiz not available
                """)
        void partitions(String id, String condition, String state, String expected, TestCase testCase) {
            Long quizId = switch (state) {
                case "DRAFT" -> draftQuizWithQuestions(2).getId();
                case "PUBLISHED" -> publishedQuizWithQuestions(2).getId();
                default -> 999_999L;
            };
            String actual = start(quizId, newStudent(), null);
            testCase.record(id, condition, "student presses Start on a " + state + " quiz", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 16-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R16-BVA-01 | Ready quiz just before it is published | false | Quiz not available
                TC-R16-BVA-02 | Same quiz just after it is published   | true  | Attempt started, 1800 s left
                """)
        void publishMoment(String id, String condition, boolean published, String expected, TestCase testCase) {
            Quiz quiz = draftQuizWithQuestions(2);
            if (published) {
                quizService.publish(quiz.getId(), instructorId());
            }
            String actual = start(quiz.getId(), newStudent(), null);
            testCase.record(id, condition, "quiz with 2 questions, published=" + published + ", student presses Start",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }
}
