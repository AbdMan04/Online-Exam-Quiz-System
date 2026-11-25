package com.quizapp.whitebox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.validation.QuestionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R11 white-box tests. Functions under test: {@code QuestionValidator.validate}, {@code isValid},
 * {@code hasValidText} and {@code countCorrect}.
 *
 * <p>Options column: {@code n/c} = n options, the first c correct; {@code NULL} = no option list;
 * {@code BLANK} = 2 options, the second with blank text; {@code LEN:301} = 2 options, the first 301 characters long.
 */
@WhiteBoxTest
@DisplayName("R11 - Question validation (white-box)")
class R11QuestionValidationWhiteBoxTest {

    static List<OptionInput> options(String spec) {
        if (spec.equals("NULL")) {
            return null;
        }
        if (spec.equals("BLANK")) {
            return List.of(new OptionInput("Option 1", true), new OptionInput("   ", false));
        }
        if (spec.startsWith("LEN:")) {
            return List.of(new OptionInput(Specs.text(spec), true), new OptionInput("Option 2", false));
        }
        String[] parts = spec.split("/");
        int count = Integer.parseInt(parts[0]);
        int correct = Integer.parseInt(parts[1]);
        List<OptionInput> options = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            options.add(new OptionInput("Option " + (i + 1), i < correct));
        }
        return options;
    }

    private static String validate(QuestionInput input) {
        return ActualOutput.of(() -> {
            QuestionValidator.validate(input);
            return "valid";
        });
    }

    private static String run(String id, String condition, String textSpec, Integer points, String optionSpec,
                              String expected, TestCase testCase) {
        String text = Specs.text(textSpec);
        String actual = validate(new QuestionInput(text, points, options(optionSpec)));
        testCase.record(id, condition, "text=" + Specs.describe(text) + ", points=" + points + ", options=" + optionSpec,
                expected, actual);
        return actual;
    }

    @Nested
    @DisplayName("Table 11-3: Statement coverage")
    class StatementCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL_VALUE", textBlock = """
                TC-R11-SC-01 | All statements of isValid, hasValidText and countCorrect up to 'return count == 1' | Q? | 5 | 4/1   | valid
                TC-R11-SC-02 | isValid(): 'return false' after the points check                                  | Q? | 0 | 4/1   | Invalid question configuration
                TC-R11-SC-03 | isValid(): 'return false' after the options-list check                            | Q? | 5 | NULL  | Invalid question configuration
                TC-R11-SC-04 | isValid(): 'return false' after the option-text check                             | Q? | 5 | BLANK | Invalid question configuration
                """)
        void statements(String id, String condition, String text, Integer points, String optionSpec, String expected,
                        TestCase testCase) {
            assertEquals(expected, run(id, condition, text, points, optionSpec, expected, testCase));
        }

        @Test
        void nullInput(TestCase testCase) {
            String actual = validate(null);
            String expected = "Invalid question configuration";
            testCase.record("TC-R11-SC-05", "isValid(): first 'return false'; validate(): 'throw INVALID_QUESTION'",
                    "question = null", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 11-4: Branch coverage")
    class BranchCoverage {

        @Test
        void inputNull(TestCase testCase) {
            String actual = validate(null);
            String expected = "Invalid question configuration";
            testCase.record("TC-R11-BC-01", "isValid: input == null -> TRUE", "question = null", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', nullValues = "NULL_VALUE", textBlock = """
                TC-R11-BC-02 | input == null -> FALSE; hasValidText: text != null -> FALSE      | NULL_VALUE | 5          | 4/1     | Invalid question configuration
                TC-R11-BC-03 | text != null -> TRUE, !isBlank -> FALSE                          | '   '      | 5          | 4/1     | Invalid question configuration
                TC-R11-BC-04 | !isBlank -> TRUE, length <= 500 -> FALSE                         | LEN:501    | 5          | 4/1     | Invalid question configuration
                TC-R11-BC-05 | text valid; points: != null -> FALSE                             | Q?         | NULL_VALUE | 4/1     | Invalid question configuration
                TC-R11-BC-06 | points: != null -> TRUE, >= 1 -> FALSE                           | Q?         | 0          | 4/1     | Invalid question configuration
                TC-R11-BC-07 | points: >= 1 -> TRUE, <= 10 -> FALSE                             | Q?         | 11         | 4/1     | Invalid question configuration
                TC-R11-BC-08 | points in range; options == null -> TRUE                         | Q?         | 5          | NULL    | Invalid question configuration
                TC-R11-BC-09 | options == null -> FALSE; size >= 2 -> FALSE                     | Q?         | 5          | 1/1     | Invalid question configuration
                TC-R11-BC-10 | size >= 2 -> TRUE, size <= 5 -> FALSE                            | Q?         | 5          | 6/1     | Invalid question configuration
                TC-R11-BC-11 | size in range; anyMatch(invalid option text) -> TRUE (blank)     | Q?         | 5          | BLANK   | Invalid question configuration
                TC-R11-BC-12 | anyMatch(invalid option text) -> TRUE (301 characters)           | Q?         | 5          | LEN:301 | Invalid question configuration
                TC-R11-BC-13 | anyMatch -> FALSE; countCorrect == 1 -> FALSE (0 correct)        | Q?         | 5          | 4/0     | Invalid question configuration
                TC-R11-BC-14 | countCorrect == 1 -> FALSE (2 correct)                           | Q?         | 5          | 4/2     | Invalid question configuration
                TC-R11-BC-15 | countCorrect == 1 -> TRUE; validate: !isValid -> FALSE           | Q?         | 5          | 4/1     | valid
                """)
        void branches(String id, String condition, String text, Integer points, String optionSpec, String expected,
                      TestCase testCase) {
            assertEquals(expected, run(id, condition, text, points, optionSpec, expected, testCase));
        }
    }
}
