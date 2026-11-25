package com.quizapp.junitfeatures;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.validation.QuestionValidator;
import com.quizapp.validation.QuizValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.SimpleArgumentsAggregator;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.TypedArgumentConverter;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * R6-R11 inputs supplied in different ways.
 *
 * <p>JUnit features: {@code @AggregateWith} (CSV columns combined into a {@link QuizInput}),
 * {@code @ConvertWith} (a custom converter turning "LEN:101" into a 101-character text), {@code @MethodSource}
 * with {@link Named} arguments, and {@code @ArgumentsSource} (a provider generating every option/correct-answer
 * combination).
 */
@Tag("parameterized")
@DisplayName("Quiz and question inputs from different argument sources")
class QuizInputSourcesTest {

    /** "LEN:n" -> a text of n characters; any other value unchanged. */
    static class LengthSpec extends TypedArgumentConverter<String, String> {
        LengthSpec() {
            super(String.class, String.class);
        }

        @Override
        protected String convert(String source) {
            return source != null && source.startsWith("LEN:") ? "T".repeat(Integer.parseInt(source.substring(4))) : source;
        }
    }

    /** Combines the columns title, duration, passing score, max attempts into one {@link QuizInput}. */
    static class QuizInputAggregator extends SimpleArgumentsAggregator {
        @Override
        protected Object aggregateArguments(ArgumentsAccessor row, Class<?> targetType, AnnotatedElementContext context,
                                            int parameterIndex) {
            return new QuizInput(new LengthSpec().convert(row.getString(1)), row.getInteger(2), row.getInteger(3),
                    row.getInteger(4));
        }
    }

    /** Every combination of 0-7 options and 0-3 correct answers, with the validity the spec demands. */
    static class OptionCombinations implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            return IntStream.rangeClosed(0, 7).boxed().flatMap(options -> IntStream.rangeClosed(0, Math.min(3, options))
                    .mapToObj(correct -> Arguments.of(options, correct, options >= 2 && options <= 5 && correct == 1)));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            every field at its minimum | LEN:3       | 5   | 0   | 1
            every field at its maximum | LEN:100     | 180 | 100 | 5
            the Gherkin example        | Java Basics | 30  | 60  | 2
            """)
    @DisplayName("R6: valid settings are accepted (columns aggregated into a QuizInput)")
    void validSettings(String label, @AggregateWith(QuizInputAggregator.class) QuizInput input) {
        assertDoesNotThrow(() -> QuizValidator.validate(input));
    }

    @ParameterizedTest(name = "title {0} characters long")
    @CsvSource({"LEN:2", "LEN:101", "LEN:500"})
    @DisplayName("R7: titles outside 3-100 characters are rejected (converted with a custom converter)")
    void invalidTitleLengths(@ConvertWith(LengthSpec.class) String title) {
        BusinessException error = assertThrows(BusinessException.class,
                () -> QuizValidator.validate(new QuizInput(title, 30, 60, 2)));
        assertEquals(Messages.INVALID_TITLE, error.getMessage());
    }

    static Stream<Arguments> invalidSettings() {
        return Stream.of(
                Arguments.of(Named.of("duration 4", new QuizInput("Java Basics", 4, 60, 2)), Messages.INVALID_DURATION),
                Arguments.of(Named.of("duration 181", new QuizInput("Java Basics", 181, 60, 2)), Messages.INVALID_DURATION),
                Arguments.of(Named.of("passing -1", new QuizInput("Java Basics", 30, -1, 2)), Messages.INVALID_PASSING_SCORE),
                Arguments.of(Named.of("passing 101", new QuizInput("Java Basics", 30, 101, 2)), Messages.INVALID_PASSING_SCORE),
                Arguments.of(Named.of("attempts 0", new QuizInput("Java Basics", 30, 60, 0)), Messages.INVALID_ATTEMPTS),
                Arguments.of(Named.of("attempts 6", new QuizInput("Java Basics", 30, 60, 6)), Messages.INVALID_ATTEMPTS));
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @MethodSource("invalidSettings")
    @DisplayName("R8/R9: each out-of-range setting gets its own message (method source with named arguments)")
    void invalidSettingsMessages(QuizInput input, String expectedMessage) {
        BusinessException error = assertThrows(BusinessException.class, () -> QuizValidator.validate(input));
        assertEquals(expectedMessage, error.getMessage());
    }

    @ParameterizedTest(name = "{0} options, {1} correct -> valid = {2}")
    @ArgumentsSource(OptionCombinations.class)
    @DisplayName("R11: every option-count / correct-count combination (custom arguments provider)")
    void everyOptionCombination(int optionCount, int correctCount, boolean expectedValid) {
        List<OptionInput> options = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            options.add(new OptionInput("Option " + (i + 1), i < correctCount));
        }
        assertEquals(expectedValid, QuestionValidator.isValid(new QuestionInput("Q?", 5, options)));
    }
}
