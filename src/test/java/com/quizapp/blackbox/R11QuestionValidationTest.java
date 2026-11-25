package com.quizapp.blackbox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R11: Question validation.
 * Given a question has fewer than 2 or more than 5 options, no correct answer, more than one correct answer,
 * or points outside 1-10, when the instructor adds it, then the system rejects it with "Invalid question configuration".
 */
@DisplayName("R11 - Question validation")
class R11QuestionValidationTest extends QuizBlackBoxSupport {

    private static final String ADDED = "Question added";

    private String addToDraftQuiz(QuestionInput input) {
        return ActualOutput.of(() -> {
            quizService.addQuestion(draftQuiz().getId(), instructorId(), input);
            return ADDED;
        });
    }

    @Nested
    @DisplayName("Table 11-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R11-EP-01 | 2-5 options, exactly 1 correct, 1-10 points | 4 | 1 | 5  | Question added
                TC-R11-EP-02 | Fewer than 2 options                        | 1 | 1 | 5  | Invalid question configuration
                TC-R11-EP-03 | More than 5 options                         | 8 | 1 | 5  | Invalid question configuration
                TC-R11-EP-04 | No correct option                           | 4 | 0 | 5  | Invalid question configuration
                TC-R11-EP-05 | More than one correct option                | 4 | 3 | 5  | Invalid question configuration
                TC-R11-EP-06 | Points below 1                              | 4 | 1 | -2 | Invalid question configuration
                TC-R11-EP-07 | Points above 10                             | 4 | 1 | 20 | Invalid question configuration
                """)
        void partitions(String id, String condition, int options, int correct, int points, String expected,
                        TestCase testCase) {
            QuestionInput input = question("What is the JVM?", options, correct, points);
            String actual = addToDraftQuiz(input);
            testCase.record(id, condition, describeQuestion(input), expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R11-EP-08 | Question text blank      | '   '            | A virtual machine | Invalid question configuration
                TC-R11-EP-09 | An option's text blank   | What is the JVM? | '   '             | Invalid question configuration
                """)
        void blankTexts(String id, String condition, String text, String firstOption, String expected,
                        TestCase testCase) {
            QuestionInput input = new QuestionInput(text, 5, List.of(
                    new OptionInput(firstOption, true), new OptionInput("A compiler", false)));
            String actual = addToDraftQuiz(input);
            testCase.record(id, condition, "text=\"" + text + "\", options=[\"" + firstOption + "\" (correct), \"A compiler\"], points=5",
                    expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 11-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R11-BVA-01 | Options = min - 1 (1)         | 1 | 1 | 5  | Invalid question configuration
                TC-R11-BVA-02 | Options = min (2)             | 2 | 1 | 5  | Question added
                TC-R11-BVA-03 | Options = max (5)             | 5 | 1 | 5  | Question added
                TC-R11-BVA-04 | Options = max + 1 (6)         | 6 | 1 | 5  | Invalid question configuration
                TC-R11-BVA-05 | Correct options = 1 - 1 (0)   | 4 | 0 | 5  | Invalid question configuration
                TC-R11-BVA-06 | Correct options = exactly (1) | 4 | 1 | 5  | Question added
                TC-R11-BVA-07 | Correct options = 1 + 1 (2)   | 4 | 2 | 5  | Invalid question configuration
                TC-R11-BVA-08 | Points = min - 1 (0)          | 4 | 1 | 0  | Invalid question configuration
                TC-R11-BVA-09 | Points = min (1)              | 4 | 1 | 1  | Question added
                TC-R11-BVA-10 | Points = max (10)             | 4 | 1 | 10 | Question added
                TC-R11-BVA-11 | Points = max + 1 (11)         | 4 | 1 | 11 | Invalid question configuration
                """)
        void boundaries(String id, String condition, int options, int correct, int points, String expected,
                        TestCase testCase) {
            QuestionInput input = question("What is the JVM?", options, correct, points);
            String actual = addToDraftQuiz(input);
            testCase.record(id, condition, describeQuestion(input), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
