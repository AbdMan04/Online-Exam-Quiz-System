package com.quizapp.blackbox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.model.Attempt;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AttemptService;
import com.quizapp.service.AuthService;
import com.quizapp.service.GradingService;
import com.quizapp.service.QuizService;
import com.quizapp.service.ReportService;
import com.quizapp.support.BlackBoxTest;
import com.quizapp.support.MutableClock;
import com.quizapp.support.TestClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared "Given" steps for the quiz requirements (R6-R20): the seeded instructor, quizzes in a
 * given state, questions, students taking a quiz, and a controllable clock. Everything goes through
 * the public services.
 */
@BlackBoxTest
abstract class QuizBlackBoxSupport {

    static final String INSTRUCTOR_EMAIL = "instructor@uni.edu";

    @Autowired
    protected QuizService quizService;
    @Autowired
    protected AttemptService attemptService;
    @Autowired
    protected AuthService authService;
    @Autowired
    protected GradingService gradingService;
    @Autowired
    protected ReportService reportService;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected MutableClock clock;

    private int studentCounter;

    @BeforeEach
    void resetClock() {
        clock.setTo(TestClockConfiguration.START);
    }

    protected Long instructorId() {
        return userRepository.findByEmail(INSTRUCTOR_EMAIL).orElseThrow().getId();
    }

    /** A second instructor (instructors are not self-registered, so the account is created directly). */
    protected Long otherInstructorId() {
        return userRepository.save(new User("Other Instructor", "other.instructor@uni.edu", "unused", Role.INSTRUCTOR))
                .getId();
    }

    protected Quiz draftQuiz() {
        return quizService.createQuiz(instructorId(), new QuizInput("Java Basics", 30, 60, 2));
    }

    protected Quiz draftQuizWithQuestions(int count) {
        Quiz quiz = draftQuiz();
        for (int i = 1; i <= count; i++) {
            quizService.addQuestion(quiz.getId(), instructorId(), question("Question " + i, 4, 1, 5));
        }
        return quiz;
    }

    protected Quiz publishedQuizWithQuestions(int count) {
        Quiz quiz = draftQuizWithQuestions(count);
        return quizService.publish(quiz.getId(), instructorId());
    }

    /**
     * A published quiz with one 2-option question per entry in {@code points} (option 1 is the correct one).
     */
    protected Quiz publishedQuiz(int durationMinutes, int passingScore, int maxAttempts, int... points) {
        Quiz quiz = quizService.createQuiz(instructorId(),
                new QuizInput("Graded quiz", durationMinutes, passingScore, maxAttempts));
        for (int i = 0; i < points.length; i++) {
            quizService.addQuestion(quiz.getId(), instructorId(), question("Question " + (i + 1), 2, 1, points[i]));
        }
        return quizService.publish(quiz.getId(), instructorId());
    }

    /** Registers a new student ("Student 1", "Student 2", ...) and returns their id. */
    protected Long newStudent() {
        studentCounter++;
        return authService.register("Student " + studentCounter, "student" + studentCounter + "@uni.edu", "Secure123")
                .getId();
    }

    protected void someStudentStartsAttempt(Quiz quiz) {
        attemptService.start(quiz.getId(), newStudent());
    }

    protected Question firstQuestion(Quiz quiz) {
        return questions(quiz).get(0);
    }

    protected List<Question> questions(Quiz quiz) {
        return quizService.getQuiz(quiz.getId(), instructorId()).getQuestions();
    }

    /**
     * Starts an attempt and answers question i according to {@code answers.charAt(i)}:
     * R = right option, W = wrong option, - = left unanswered. Returns the attempt, still running.
     */
    protected Attempt answer(Quiz quiz, Long studentId, String answers) {
        Attempt attempt = attemptService.start(quiz.getId(), studentId);
        List<Question> questions = questions(quiz);
        for (int i = 0; i < answers.length(); i++) {
            char choice = answers.charAt(i);
            if (choice != '-') {
                Question question = questions.get(i);
                int optionIndex = choice == 'R' ? 0 : 1;
                attemptService.submitAnswer(attempt.getId(), studentId, question.getId(),
                        question.getOptions().get(optionIndex).getId());
            }
        }
        return attempt;
    }

    /** {@link #answer} and then submit. */
    protected Attempt takeQuiz(Quiz quiz, Long studentId, String answers) {
        Attempt attempt = answer(quiz, studentId, answers);
        return attemptService.submit(attempt.getId(), studentId);
    }

    /** A question with {@code optionCount} options of which the first {@code correctCount} are correct. */
    static QuestionInput question(String text, int optionCount, int correctCount, int points) {
        List<OptionInput> options = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            options.add(new OptionInput("Option " + (i + 1), i < correctCount));
        }
        return new QuestionInput(text, points, options);
    }

    static String describeQuestion(QuestionInput input) {
        long correct = input.options().stream().filter(OptionInput::correct).count();
        String text = input.text() == null ? "null" : input.text().length() > 30
                ? input.text().length() + " characters" : "\"" + input.text() + "\"";
        return "text=" + text + ", options=" + input.options().size() + ", correct=" + correct
                + ", points=" + input.points();
    }
}
