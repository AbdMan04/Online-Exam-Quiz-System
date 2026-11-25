package com.quizapp.whitebox;

import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.QuizService;
import com.quizapp.service.ReportService;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.web.InstructorController;
import com.quizapp.web.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Shared setup for the quiz-authoring white-box tests (R6-R13): the real {@link QuizService} with mocked
 * repositories, instructor 1 who owns quiz 10 ("Java Basics", DRAFT, no questions, no attempts),
 * and an {@link InstructorController} wired to that service.
 */
@WhiteBoxTest
abstract class QuizWhiteBoxSupport {

    static final Long INSTRUCTOR_ID = 1L;
    static final Long OTHER_INSTRUCTOR_ID = 2L;
    static final Long QUIZ_ID = 10L;
    static final Long MISSING_ID = 999L;
    static final SessionUser INSTRUCTOR_SESSION = new SessionUser(INSTRUCTOR_ID, "Dr. Smith", Role.INSTRUCTOR);

    @Mock
    protected QuizRepository quizRepository;
    @Mock
    protected QuestionRepository questionRepository;
    @Mock
    protected AttemptRepository attemptRepository;
    @Mock
    protected UserRepository userRepository;
    @Mock
    protected ReportService reportService;

    protected QuizService quizService;
    protected User instructor;
    protected Quiz quiz;

    @BeforeEach
    void givenInstructorOwnsQuiz10() {
        quizService = new QuizService(quizRepository, questionRepository, attemptRepository, userRepository);
        instructor = Fixtures.user(INSTRUCTOR_ID, "Dr. Smith", Role.INSTRUCTOR);
        quiz = Fixtures.withId(new Quiz("Java Basics", 30, 60, 2, instructor), QUIZ_ID);
        lenient().when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        lenient().when(userRepository.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(instructor));
        lenient().when(quizRepository.save(any(Quiz.class)))
                .thenAnswer(call -> Fixtures.withIdIfMissing(call.getArgument(0), QUIZ_ID));
        lenient().when(questionRepository.save(any(Question.class))).thenAnswer(call -> call.getArgument(0));
    }

    protected InstructorController controller() {
        return new InstructorController(quizService, reportService);
    }

    /** Adds saved question {@code n} (id 100 + n, 2 options, 5 points) to quiz 10. */
    protected Question addSavedQuestion(int n) {
        Question question = Fixtures.question(100L + n, "Question " + n, 5, 2, 0);
        quiz.addQuestion(question);
        lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        return question;
    }

    protected void studentsHaveAttempted() {
        lenient().when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);
    }

    static String flash(RedirectAttributesModelMap redirect) {
        Object message = redirect.getFlashAttributes().get("message");
        Object error = redirect.getFlashAttributes().get("error");
        return message != null ? "flash message=" + message : "flash error=" + error;
    }
}
