package com.quizapp.blackbox;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.UserRepository;
import com.quizapp.service.AttemptService;
import com.quizapp.service.AuthService;
import com.quizapp.service.QuizService;
import com.quizapp.support.BlackBoxTest;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared "Given" steps for the quiz requirements (R6-R20): the seeded instructor, quizzes in a
 * given state, questions, and students taking a quiz. Everything goes through the public services.
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
    protected UserRepository userRepository;

    private int studentCounter;

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

    /** Registers a new student and returns their id. */
    protected Long newStudent() {
        studentCounter++;
        return authService.register("Student " + studentCounter, "student" + studentCounter + "@uni.edu", "Secure123")
                .getId();
    }

    protected void someStudentStartsAttempt(Quiz quiz) {
        attemptService.start(quiz.getId(), newStudent());
    }

    protected Question firstQuestion(Quiz quiz) {
        return quizService.getQuiz(quiz.getId(), instructorId()).getQuestions().get(0);
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
