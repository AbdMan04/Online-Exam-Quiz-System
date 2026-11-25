package com.quizapp.service;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.AnswerOption;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.QuizStatus;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuizService - R6 to R13")
class QuizServiceTest {

    private static final Long INSTRUCTOR_ID = 1L;
    private static final Long OTHER_INSTRUCTOR_ID = 2L;
    private static final Long QUIZ_ID = 10L;
    private static final Long QUESTION_ID = 100L;

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuizService quizService;

    private User instructor;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Smith", "smith@uni.edu", "hash", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(instructor, "id", INSTRUCTOR_ID);
        quiz = new Quiz("Java Basics", 30, 60, 2, instructor);
        ReflectionTestUtils.setField(quiz, "id", QUIZ_ID);
    }

    private static QuestionInput validQuestion() {
        return new QuestionInput("What is the JVM?", 5, List.of(
                new OptionInput("A virtual machine", true),
                new OptionInput("A compiler", false),
                new OptionInput("An IDE", false),
                new OptionInput("A database", false)));
    }

    private Question addExistingQuestion() {
        Question question = new Question("Q1", 5, List.of(new AnswerOption("A", true), new AnswerOption("B", false)));
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        quiz.addQuestion(question);
        return question;
    }

    private static String messageOf(Executable action) {
        return assertThrows(BusinessException.class, action).getMessage();
    }

    @Nested
    @DisplayName("Create quiz (R6-R9)")
    class CreateQuiz {

        @Test
        @DisplayName("R6 valid settings create a DRAFT quiz owned by the instructor")
        void createsDraft() {
            when(userRepository.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(instructor));
            when(quizRepository.save(any(Quiz.class))).thenAnswer(call -> call.getArgument(0));

            Quiz created = quizService.createQuiz(INSTRUCTOR_ID, new QuizInput("  Java Basics ", 30, 60, 2));

            assertEquals("Java Basics", created.getTitle());
            assertEquals(QuizStatus.DRAFT, created.getStatus());
            assertEquals(INSTRUCTOR_ID, created.getInstructor().getId());
        }

        @Test
        @DisplayName("R8 invalid settings are rejected before anything is saved")
        void invalidNotSaved() {
            assertEquals(Messages.INVALID_DURATION,
                    messageOf(() -> quizService.createQuiz(INSTRUCTOR_ID, new QuizInput("Java Basics", 200, 60, 2))));
            verify(quizRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Questions (R10, R11, R13)")
    class Questions {

        @Test
        @DisplayName("R10 a valid question is added to the quiz")
        void addsQuestion() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(false);
            when(questionRepository.save(any(Question.class))).thenAnswer(call -> call.getArgument(0));

            Question added = quizService.addQuestion(QUIZ_ID, INSTRUCTOR_ID, validQuestion());

            assertEquals(1, quiz.getQuestionCount());
            assertEquals(4, added.getOptions().size());
            assertEquals(quiz, added.getQuiz());
        }

        @Test
        @DisplayName("R11 an invalid question is rejected")
        void invalidQuestion() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            QuestionInput noCorrect = new QuestionInput("Q?", 5,
                    List.of(new OptionInput("A", false), new OptionInput("B", false)));

            assertEquals(Messages.INVALID_QUESTION, messageOf(() -> quizService.addQuestion(QUIZ_ID, INSTRUCTOR_ID, noCorrect)));
            assertEquals(0, quiz.getQuestionCount());
        }

        @Test
        @DisplayName("R13 adding a question after attempts exist is rejected")
        void addLockedByAttempts() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);

            assertEquals(Messages.QUIZ_LOCKED, messageOf(() -> quizService.addQuestion(QUIZ_ID, INSTRUCTOR_ID, validQuestion())));
        }

        @Test
        @DisplayName("R13 editing a question after attempts exist is rejected")
        void editLockedByAttempts() {
            addExistingQuestion();
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(quiz.getQuestions().get(0)));
            when(attemptRepository.existsByQuizId(QUIZ_ID)).thenReturn(true);

            assertEquals(Messages.QUIZ_LOCKED,
                    messageOf(() -> quizService.editQuestion(QUESTION_ID, INSTRUCTOR_ID, validQuestion())));
        }

        @Test
        @DisplayName("editing replaces text, points and options")
        void editsQuestion() {
            Question question = addExistingQuestion();
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(questionRepository.save(question)).thenReturn(question);

            quizService.editQuestion(QUESTION_ID, INSTRUCTOR_ID, validQuestion());

            assertEquals("What is the JVM?", question.getText());
            assertEquals(4, question.getOptions().size());
        }

        @Test
        @DisplayName("another instructor's quiz is reported as not found")
        void notOwner() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));

            assertEquals(Messages.QUIZ_NOT_FOUND,
                    messageOf(() -> quizService.addQuestion(QUIZ_ID, OTHER_INSTRUCTOR_ID, validQuestion())));
        }
    }

    @Nested
    @DisplayName("Publish (R12)")
    class Publish {

        @Test
        @DisplayName("R12 a DRAFT quiz with a question becomes PUBLISHED")
        void publishes() {
            addExistingQuestion();
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
            when(quizRepository.save(quiz)).thenReturn(quiz);

            assertEquals(QuizStatus.PUBLISHED, quizService.publish(QUIZ_ID, INSTRUCTOR_ID).getStatus());
        }

        @Test
        @DisplayName("R12 a quiz with 0 questions cannot be published")
        void noQuestions() {
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));

            assertEquals(Messages.QUIZ_NEEDS_QUESTION, messageOf(() -> quizService.publish(QUIZ_ID, INSTRUCTOR_ID)));
            assertEquals(QuizStatus.DRAFT, quiz.getStatus());
        }

        @Test
        @DisplayName("R12 publishing twice is rejected")
        void alreadyPublished() {
            addExistingQuestion();
            quiz.publish();
            when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));

            assertEquals(Messages.QUIZ_ALREADY_PUBLISHED, messageOf(() -> quizService.publish(QUIZ_ID, INSTRUCTOR_ID)));
        }

        @Test
        @DisplayName("R12 the last question of a PUBLISHED quiz cannot be deleted")
        void keepLastQuestionOfPublishedQuiz() {
            Question question = addExistingQuestion();
            quiz.publish();
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

            assertEquals(Messages.QUIZ_NEEDS_QUESTION, messageOf(() -> quizService.deleteQuestion(QUESTION_ID, INSTRUCTOR_ID)));
            assertEquals(1, quiz.getQuestionCount());
        }

        @Test
        @DisplayName("a question of a DRAFT quiz can be deleted")
        void deleteFromDraft() {
            Question question = addExistingQuestion();
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

            quizService.deleteQuestion(QUESTION_ID, INSTRUCTOR_ID);

            assertEquals(0, quiz.getQuestionCount());
            verify(questionRepository).delete(question);
        }
    }
}
