package com.quizapp.blackbox;

import com.quizapp.model.Attempt;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import com.quizapp.support.TestClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * R17: Timer expiry.
 * Given an attempt has been running for the full duration, when the time expires, then the attempt is
 * auto-submitted with the answers saved so far, and any later answer submission is rejected.
 *
 * <p>Setup for every case: a 30-minute quiz with 2 questions; the student starts at 10:00 and answers
 * question 1 at 10:01. The test then moves the clock and answers question 2.
 */
@DisplayName("R17 - Timer expiry")
class R17TimerExpiryTest extends QuizBlackBoxSupport {

    private Quiz quiz;
    private Long student;
    private Attempt attempt;

    @BeforeEach
    void givenARunningAttemptWithOneAnswer() {
        quiz = publishedQuiz(30, 60, 2, 5, 5);
        student = newStudent();
        attempt = attemptService.start(quiz.getId(), student);
        clock.advance(Duration.ofMinutes(1));
        Question first = questions(quiz).get(0);
        attemptService.submitAnswer(attempt.getId(), student, first.getId(), first.getOptions().get(0).getId());
    }

    /** Answers question 2 with the given option and reports the message plus the attempt's state. */
    private String answerQuestion2(Long studentId, Long optionId) {
        Question second = questions(quiz).get(1);
        String message = ActualOutput.of(() -> {
            attemptService.submitAnswer(attempt.getId(), studentId, second.getId(), optionId);
            return "Answer saved";
        });
        return message + " " + state();
    }

    private String state() {
        Attempt current = attemptService.getAttempt(attempt.getId(), student);
        return "[" + current.getStatus() + ", " + current.getSelectedOptionIds().size() + " answer(s) saved]";
    }

    private Long question2Option() {
        return questions(quiz).get(1).getOptions().get(0).getId();
    }

    private void moveClockTo(int minutes, int seconds) {
        clock.setTo(TestClockConfiguration.START.plusMinutes(minutes).plusSeconds(seconds));
    }

    @Nested
    @DisplayName("Table 17-1: Equivalence partitions")
    class EquivalencePartitions {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-EP-01 | Answer while time is left (10:10)      | 10 | Answer saved [IN_PROGRESS, 2 answer(s) saved]
                TC-R17-EP-02 | Answer after the time is up (10:45)    | 45 | Time is up - attempt already submitted [SUBMITTED, 1 answer(s) saved]
                """)
        void answerByTime(String id, String condition, int minute, String expected, TestCase testCase) {
            moveClockTo(minute, 0);
            String actual = answerQuestion2(student, question2Option());
            testCase.record(id, condition, "answer question 2 at 10:" + minute, expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-EP-03 | Answer after the student submitted (time left) | Time is up - attempt already submitted [SUBMITTED, 1 answer(s) saved]
                """)
        void answerAfterManualSubmit(String id, String condition, String expected, TestCase testCase) {
            moveClockTo(5, 0);
            attemptService.submit(attempt.getId(), student);
            String actual = answerQuestion2(student, question2Option());
            testCase.record(id, condition, "submit at 10:05, then answer question 2", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-EP-04 | Student opens the attempt after the time is up | SUBMITTED, submitted at 10:30, 1 answer(s) kept
                """)
        void openAfterExpiry(String id, String condition, String expected, TestCase testCase) {
            moveClockTo(50, 0);
            Attempt opened = attemptService.getAttempt(attempt.getId(), student);
            String actual = opened.getStatus() + ", submitted at " + opened.getSubmittedAt().toLocalTime()
                    + ", " + opened.getSelectedOptionIds().size() + " answer(s) kept";
            testCase.record(id, condition, "open the attempt at 10:50 (deadline 10:30)", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-EP-05 | Option that belongs to another question | Invalid answer [IN_PROGRESS, 1 answer(s) saved]
                """)
        void optionOfOtherQuestion(String id, String condition, String expected, TestCase testCase) {
            Long optionOfQuestion1 = questions(quiz).get(0).getOptions().get(1).getId();
            String actual = answerQuestion2(student, optionOfQuestion1);
            testCase.record(id, condition, "answer question 2 with an option of question 1", expected, actual);
            assertEquals(expected, actual);
        }

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-EP-06 | Another student answers this attempt | Attempt not found [IN_PROGRESS, 1 answer(s) saved]
                """)
        void otherStudent(String id, String condition, String expected, TestCase testCase) {
            String actual = answerQuestion2(newStudent(), question2Option());
            testCase.record(id, condition, "a different student answers question 2 of this attempt", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 17-2: Boundary values (2-BVA)")
    class BoundaryValues {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R17-BVA-01 | Elapsed = duration - 1 s (10:29:59) | 29 | 59 | Answer saved [IN_PROGRESS, 2 answer(s) saved]
                TC-R17-BVA-02 | Elapsed = duration (10:30:00)       | 30 | 0  | Time is up - attempt already submitted [SUBMITTED, 1 answer(s) saved]
                """)
        void deadline(String id, String condition, int minute, int second, String expected, TestCase testCase) {
            moveClockTo(minute, second);
            String actual = answerQuestion2(student, question2Option());
            testCase.record(id, condition, String.format("answer question 2 at 10:%02d:%02d (deadline 10:30:00)",
                    minute, second), expected, actual);
            assertEquals(expected, actual);
        }
    }
}
