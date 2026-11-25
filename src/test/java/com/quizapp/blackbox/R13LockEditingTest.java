package com.quizapp.blackbox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R13: Lock editing.
 * Given a student has already attempted a quiz, when the instructor tries to edit its questions,
 * then the system rejects it with "Quiz cannot be modified after attempts exist".
 */
@DisplayName("R13 - Lock editing")
class R13LockEditingTest extends QuizBlackBoxSupport {

    private static final QuestionInput EDITED = question("Edited question", 3, 1, 4);
    private static final QuestionInput INVALID = question("Edited question", 1, 1, 4);

    /** A published quiz with 2 questions that the given number of students have started. */
    private Quiz quizWithAttempts(int attempts) {
        Quiz quiz = publishedQuizWithQuestions(2);
        for (int i = 0; i < attempts; i++) {
            someStudentStartsAttempt(quiz);
        }
        return quiz;
    }

    /** Performs one instructor change on the quiz's questions and reports the result. */
    private String change(Quiz quiz, String action) {
        Long questionId = firstQuestion(quiz).getId();
        return ActualOutput.of(() -> switch (action) {
            case "EDIT" -> {
                quizService.editQuestion(questionId, instructorId(), EDITED);
                yield "Question updated";
            }
            case "EDIT_INVALID" -> {
                quizService.editQuestion(questionId, instructorId(), INVALID);
                yield "Question updated";
            }
            case "ADD" -> {
                quizService.addQuestion(quiz.getId(), instructorId(), EDITED);
                yield "Question added";
            }
            case "DELETE" -> {
                quizService.deleteQuestion(questionId, instructorId());
                yield "Question deleted";
            }
            default -> throw new IllegalArgumentException("Unknown action " + action);
        });
    }

    @Nested
    @DisplayName("Table 13-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R13-EP-01 | No attempts yet, edit a question                        | 0 | EDIT         | Question updated
                TC-R13-EP-02 | Attempts exist, edit a question                         | 1 | EDIT         | Quiz cannot be modified after attempts exist
                TC-R13-EP-03 | Attempts exist, add a question                          | 1 | ADD          | Quiz cannot be modified after attempts exist
                TC-R13-EP-04 | Attempts exist, delete a question                       | 1 | DELETE       | Quiz cannot be modified after attempts exist
                TC-R13-EP-05 | Attempts exist, invalid edit (lock is checked first)    | 1 | EDIT_INVALID | Quiz cannot be modified after attempts exist
                """)
        void partitions(String id, String condition, int attempts, String action, String expected, TestCase testCase) {
            String actual = change(quizWithAttempts(attempts), action);
            testCase.record(id, condition, attempts + " attempt(s), action=" + action, expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 13-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R13-BVA-01 | Attempts = 1 - 1 (0), edit allowed | 0 | Question updated
                TC-R13-BVA-02 | Attempts = lock limit (1)          | 1 | Quiz cannot be modified after attempts exist
                """)
        void attemptCount(String id, String condition, int attempts, String expected, TestCase testCase) {
            String actual = change(quizWithAttempts(attempts), "EDIT");
            testCase.record(id, condition, attempts + " attempt(s), edit question 1", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
