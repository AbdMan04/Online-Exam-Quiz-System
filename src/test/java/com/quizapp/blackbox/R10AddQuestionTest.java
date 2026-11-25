package com.quizapp.blackbox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R10: Add question.
 * Given a DRAFT quiz and a question with 4 options, 1 correct answer, and 5 points,
 * when the instructor adds the question, then the question is added to the quiz.
 */
@DisplayName("R10 - Add question")
class R10AddQuestionTest extends QuizBlackBoxSupport {

    private static final QuestionInput VALID = question("What is the JVM?", 4, 1, 5);

    /** Adds the question as the given instructor and reports how many questions the quiz now has. */
    private String addQuestion(Long quizId, Long instructorId, QuestionInput input) {
        return ActualOutput.of(() -> {
            quizService.addQuestion(quizId, instructorId, input);
            int count = quizService.getQuiz(quizId, instructorId).getQuestionCount();
            return "Question added (quiz has " + count + " question" + (count == 1 ? ")" : "s)");
        });
    }

    @Nested
    @DisplayName("Table 10-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R10-EP-01 | DRAFT quiz, own quiz, valid question            | DRAFT     | OWN     | Question added (quiz has 1 question)
                TC-R10-EP-02 | PUBLISHED quiz without attempts, valid question | PUBLISHED | OWN     | Question added (quiz has 2 questions)
                TC-R10-EP-03 | Quiz of another instructor                      | DRAFT     | OTHER   | Quiz not found
                TC-R10-EP-04 | Quiz that does not exist                        | DRAFT     | MISSING | Quiz not found
                """)
        void partitions(String id, String condition, String state, String owner, String expected, TestCase testCase) {
            Quiz quiz = "PUBLISHED".equals(state) ? publishedQuizWithQuestions(1) : draftQuiz();
            Long quizId = "MISSING".equals(owner) ? 999_999L : quiz.getId();
            Long instructor = "OTHER".equals(owner) ? otherInstructorId() : instructorId();

            String actual = addQuestion(quizId, instructor, VALID);
            testCase.record(id, condition, state + " quiz (" + owner.toLowerCase() + "), " + describeQuestion(VALID),
                    expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 10-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R10-BVA-01 | Question text length = min - 1 (0)   | 0   | Invalid question configuration
                TC-R10-BVA-02 | Question text length = min (1)       | 1   | Question added (quiz has 1 question)
                TC-R10-BVA-03 | Question text length = max (500)     | 500 | Question added (quiz has 1 question)
                TC-R10-BVA-04 | Question text length = max + 1 (501) | 501 | Invalid question configuration
                """)
        void textLength(String id, String condition, int length, String expected, TestCase testCase) {
            QuestionInput input = question("Q".repeat(length), 4, 1, 5);
            String actual = addQuestion(draftQuiz().getId(), instructorId(), input);
            testCase.record(id, condition, "DRAFT quiz, " + describeQuestion(input), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R10-BVA-05 | Option text length = max (300)     | 300 | Question added (quiz has 1 question)
                TC-R10-BVA-06 | Option text length = max + 1 (301) | 301 | Invalid question configuration
                """)
        void optionTextLength(String id, String condition, int length, String expected, TestCase testCase) {
            QuestionInput input = new QuestionInput("What is the JVM?", 5, List.of(
                    new OptionInput("O".repeat(length), true), new OptionInput("A compiler", false)));
            String actual = addQuestion(draftQuiz().getId(), instructorId(), input);
            testCase.record(id, condition, "DRAFT quiz, first option = " + length + " characters, "
                    + describeQuestion(input), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
