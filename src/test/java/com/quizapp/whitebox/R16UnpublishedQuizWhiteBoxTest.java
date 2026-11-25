package com.quizapp.whitebox;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.Quiz;
import com.quizapp.support.ActualOutput;
import com.quizapp.support.TestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * R16 white-box tests. Functions under test: the quiz lookup at the start of {@code AttemptService.start}
 * ({@code findById(...).filter(Quiz::isPublished).orElseThrow(...)}), {@code Quiz.isPublished} and the
 * {@code StudentController} error handler that shows the message on the dashboard.
 */
@DisplayName("R16 - Unpublished quiz (white-box)")
class R16UnpublishedQuizWhiteBoxTest extends AttemptWhiteBoxSupport {

    private static final Long DRAFT_QUIZ_ID = 11L;

    private String start(Long quizId) {
        return ActualOutput.of(() -> "attempt " + attemptService.start(quizId, STUDENT_ID).getId() + " started");
    }

    private void givenDraftQuiz11() {
        Quiz draft = Fixtures.withId(new Quiz("Draft quiz", 30, 60, 2, instructor), DRAFT_QUIZ_ID);
        when(quizRepository.findById(DRAFT_QUIZ_ID)).thenReturn(Optional.of(draft));
    }

    @Nested
    @DisplayName("Table 16-3: Statement coverage")
    class StatementCoverage {

        @Test
        void throwNotAvailable(TestCase testCase) {
            givenDraftQuiz11();
            String actual = start(DRAFT_QUIZ_ID);
            String expected = "Quiz not available";
            testCase.record("TC-R16-SC-01", "start(): orElseThrow lambda 'throw QUIZ_NOT_AVAILABLE'",
                    "quiz 11 (DRAFT)", expected, actual);
            assertEquals(expected, actual);
        }

        @Test
        void errorHandler(TestCase testCase) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            FlashMap flashMap = new FlashMap();
            request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flashMap);
            String view = studentController().backToDashboard(new BusinessException(Messages.QUIZ_NOT_AVAILABLE), request);
            String actual = "view=" + view + ", flash error=" + flashMap.get("error");
            String expected = "view=redirect:/student/dashboard, flash error=Quiz not available";
            testCase.record("TC-R16-SC-02", "StudentController.backToDashboard() exception handler",
                    "BusinessException(\"Quiz not available\") from POST start", expected, actual);
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Table 16-4: Branch coverage")
    class BranchCoverage {

        @ParameterizedTest(name = "{0}: {1}")
        @CsvSource(delimiter = '|', textBlock = """
                TC-R16-BC-01 | findById -> empty (quiz does not exist)                   | 999 | Quiz not available
                TC-R16-BC-02 | quiz found, isPublished(): status == PUBLISHED -> FALSE   | 11  | Quiz not available
                TC-R16-BC-03 | quiz found, isPublished(): status == PUBLISHED -> TRUE    | 10  | attempt 50 started
                """)
        void lookupBranches(String id, String condition, Long quizId, String expected, TestCase testCase) {
            if (quizId.equals(DRAFT_QUIZ_ID)) {
                givenDraftQuiz11();
            }
            String actual = start(quizId);
            String state = quizId.equals(QUIZ_ID) ? "PUBLISHED" : quizId.equals(DRAFT_QUIZ_ID) ? "DRAFT" : "missing";
            testCase.record(id, condition, "start quiz " + quizId + " (" + state + ")", expected, actual);
            assertEquals(expected, actual);
        }
    }
}
