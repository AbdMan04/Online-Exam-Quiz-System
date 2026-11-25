package com.quizapp.whitebox;

import com.quizapp.model.Attempt;
import com.quizapp.model.AttemptStatus;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AttemptService;
import com.quizapp.service.GradingService;
import com.quizapp.support.MutableClock;
import com.quizapp.support.WhiteBoxTest;
import com.quizapp.web.SessionUser;
import com.quizapp.web.StudentController;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Shared setup for the quiz-taking white-box tests (R14-R20): the real {@link AttemptService} with mocked
 * repositories and a controllable clock (10:00), student 5 "Alice", and published quiz 10 ("Java Basics",
 * 30 minutes, passing 60, max 2 attempts) with questions 101 and 102 (5 points each, option 0 correct).
 */
@WhiteBoxTest
abstract class AttemptWhiteBoxSupport {

    static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 25, 10, 0);
    static final Long INSTRUCTOR_ID = 1L;
    static final Long STUDENT_ID = 5L;
    static final Long OTHER_STUDENT_ID = 6L;
    static final Long QUIZ_ID = 10L;
    static final Long ATTEMPT_ID = 50L;
    static final Long MISSING_ID = 999L;
    static final SessionUser STUDENT_SESSION = new SessionUser(STUDENT_ID, "Alice", Role.STUDENT);

    @Mock
    protected AttemptRepository attemptRepository;
    @Mock
    protected QuizRepository quizRepository;
    @Mock
    protected UserRepository userRepository;
    @Mock
    protected QuestionRepository questionRepository;

    protected final GradingService gradingService = new GradingService();
    protected MutableClock clock;
    protected AttemptService attemptService;
    protected User instructor;
    protected User student;
    protected Quiz quiz;
    protected Question question1;
    protected Question question2;

    @BeforeEach
    void givenPublishedQuizAndStudent() {
        clock = new MutableClock(T0);
        attemptService = new AttemptService(attemptRepository, quizRepository, userRepository, clock);
        instructor = Fixtures.user(INSTRUCTOR_ID, "Dr. Smith", Role.INSTRUCTOR);
        student = Fixtures.user(STUDENT_ID, "Alice", Role.STUDENT);
        quiz = Fixtures.withId(new Quiz("Java Basics", 30, 60, 2, instructor), QUIZ_ID);
        question1 = Fixtures.question(101L, "Question 1", 5, 2, 0);
        question2 = Fixtures.question(102L, "Question 2", 5, 2, 0);
        quiz.addQuestion(question1);
        quiz.addQuestion(question2);
        quiz.publish();
        lenient().when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        lenient().when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        lenient().when(attemptRepository.save(any(Attempt.class)))
                .thenAnswer(call -> Fixtures.withIdIfMissing(call.getArgument(0), ATTEMPT_ID));
    }

    /** Alice's attempt 50 on quiz 10, started at {@code startedAt}; the repository can find it by id. */
    protected Attempt attemptStartedAt(LocalDateTime startedAt) {
        Attempt attempt = Fixtures.withId(new Attempt(quiz, student, startedAt), ATTEMPT_ID);
        lenient().when(attemptRepository.findById(ATTEMPT_ID)).thenReturn(Optional.of(attempt));
        return attempt;
    }

    /** Makes the attempt Alice's open (IN_PROGRESS) attempt on quiz 10. */
    protected Attempt openAttemptStartedAt(LocalDateTime startedAt) {
        Attempt attempt = attemptStartedAt(startedAt);
        lenient().when(attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(QUIZ_ID, STUDENT_ID,
                AttemptStatus.IN_PROGRESS)).thenReturn(Optional.of(attempt));
        return attempt;
    }

    protected void attemptsUsed(long count) {
        lenient().when(attemptRepository.countByQuizIdAndStudentId(QUIZ_ID, STUDENT_ID)).thenReturn(count);
    }

    /** Sets the clock to 10:mm:ss on the test day. */
    protected void clockAt(int minutes, int seconds) {
        clock.setTo(T0.plusMinutes(minutes).plusSeconds(seconds));
    }

    protected StudentController studentController() {
        return new StudentController(attemptService, gradingService);
    }

    static String time(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.toLocalTime().toString();
    }
}
