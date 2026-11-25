package com.quizapp.service;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuizInput;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.AnswerOption;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import com.quizapp.validation.QuestionValidator;
import com.quizapp.validation.QuizValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quiz creation by instructors: R6-R9 create, R10-R11 questions, R12 publish, R13 lock after attempts.
 */
@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final UserRepository userRepository;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository,
                       AttemptRepository attemptRepository, UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
    }

    /** R6-R9: validates the settings and saves a new DRAFT quiz. */
    public Quiz createQuiz(Long instructorId, QuizInput input) {
        QuizValidator.validate(input);
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalStateException("Unknown instructor " + instructorId));
        Quiz quiz = new Quiz(input.title().trim(), input.durationMinutes(), input.passingScore(),
                input.maxAttempts(), instructor);
        return quizRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public List<Quiz> listQuizzes(Long instructorId) {
        return quizRepository.findByInstructorIdOrderByIdDesc(instructorId);
    }

    @Transactional(readOnly = true)
    public Quiz getQuiz(Long quizId, Long instructorId) {
        return findOwnedQuiz(quizId, instructorId);
    }

    @Transactional(readOnly = true)
    public Question getQuestion(Long questionId, Long instructorId) {
        return findOwnedQuestion(questionId, instructorId);
    }

    @Transactional(readOnly = true)
    public boolean hasAttempts(Long quizId) {
        return attemptRepository.existsByQuizId(quizId);
    }

    /** R10, R11, R13: adds a valid question while no attempts exist. */
    public Question addQuestion(Long quizId, Long instructorId, QuestionInput input) {
        Quiz quiz = findOwnedQuiz(quizId, instructorId);
        ensureNoAttempts(quiz);
        QuestionValidator.validate(input);
        Question question = new Question(input.text().trim(), input.points(), toOptions(input));
        quiz.addQuestion(question);
        return questionRepository.save(question);
    }

    /** R11, R13: replaces a question's content while no attempts exist. */
    public Question editQuestion(Long questionId, Long instructorId, QuestionInput input) {
        Question question = findOwnedQuestion(questionId, instructorId);
        ensureNoAttempts(question.getQuiz());
        QuestionValidator.validate(input);
        question.update(input.text().trim(), input.points(), toOptions(input));
        return questionRepository.save(question);
    }

    /** R12, R13: removes a question; a published quiz keeps at least one. */
    public void deleteQuestion(Long questionId, Long instructorId) {
        Question question = findOwnedQuestion(questionId, instructorId);
        Quiz quiz = question.getQuiz();
        ensureNoAttempts(quiz);
        if (quiz.isPublished() && quiz.getQuestionCount() == 1) {
            throw new BusinessException(Messages.QUIZ_NEEDS_QUESTION);
        }
        quiz.removeQuestion(question);
        questionRepository.delete(question);
    }

    /** R12: DRAFT with at least one question becomes PUBLISHED. */
    public Quiz publish(Long quizId, Long instructorId) {
        Quiz quiz = findOwnedQuiz(quizId, instructorId);
        if (quiz.isPublished()) {
            throw new BusinessException(Messages.QUIZ_ALREADY_PUBLISHED);
        }
        if (quiz.getQuestionCount() == 0) {
            throw new BusinessException(Messages.QUIZ_NEEDS_QUESTION);
        }
        quiz.publish();
        return quizRepository.save(quiz);
    }

    private Quiz findOwnedQuiz(Long quizId, Long instructorId) {
        return quizRepository.findById(quizId)
                .filter(quiz -> quiz.isOwnedBy(instructorId))
                .orElseThrow(() -> new BusinessException(Messages.QUIZ_NOT_FOUND));
    }

    private Question findOwnedQuestion(Long questionId, Long instructorId) {
        return questionRepository.findById(questionId)
                .filter(question -> question.getQuiz().isOwnedBy(instructorId))
                .orElseThrow(() -> new BusinessException(Messages.QUESTION_NOT_FOUND));
    }

    /** R13 */
    private void ensureNoAttempts(Quiz quiz) {
        if (attemptRepository.existsByQuizId(quiz.getId())) {
            throw new BusinessException(Messages.QUIZ_LOCKED);
        }
    }

    private static List<AnswerOption> toOptions(QuestionInput input) {
        return input.options().stream()
                .map(option -> new AnswerOption(option.text().trim(), option.correct()))
                .toList();
    }
}
