package com.quizapp.service;

import com.quizapp.dto.QuizAvailability;
import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import com.quizapp.model.AnswerOption;
import com.quizapp.model.Attempt;
import com.quizapp.model.AttemptStatus;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.QuizStatus;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Taking a quiz: R14 start, R15 attempt limit, R16 unpublished quiz, R17 timer expiry.
 */
@Service
@Transactional(noRollbackFor = BusinessException.class) // an auto-submit (R17) must survive a rejected request
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AttemptService(AttemptRepository attemptRepository, QuizRepository quizRepository,
                          UserRepository userRepository, Clock clock) {
        this.attemptRepository = attemptRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** Published quizzes with the student's attempt usage (student dashboard). */
    @Transactional(readOnly = true)
    public List<QuizAvailability> listAvailableQuizzes(Long studentId) {
        return quizRepository.findByStatusOrderByIdDesc(QuizStatus.PUBLISHED).stream()
                .map(quiz -> new QuizAvailability(quiz,
                        attemptRepository.countByQuizIdAndStudentId(quiz.getId(), studentId),
                        findOpenAttempt(quiz.getId(), studentId).map(Attempt::getId).orElse(null)))
                .toList();
    }

    /** R14-R16: starts a new attempt, or resumes the student's unexpired open attempt. */
    public Attempt start(Long quizId, Long studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .filter(Quiz::isPublished)
                .orElseThrow(() -> new BusinessException(Messages.QUIZ_NOT_AVAILABLE));
        Optional<Attempt> open = findOpenAttempt(quizId, studentId);
        if (open.isPresent() && !checkExpiry(open.get())) {
            return open.get();
        }
        if (attemptRepository.countByQuizIdAndStudentId(quizId, studentId) >= quiz.getMaxAttempts()) {
            throw new BusinessException(Messages.NO_ATTEMPTS_LEFT);
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Unknown student " + studentId));
        return attemptRepository.save(new Attempt(quiz, student, now()));
    }

    /** Returns the student's attempt, auto-submitting it first if its time is up (R17). */
    public Attempt getAttempt(Long attemptId, Long studentId) {
        Attempt attempt = findOwnedAttempt(attemptId, studentId);
        checkExpiry(attempt);
        return attempt;
    }

    /** R17: saves one answer while the attempt is still running. */
    public void submitAnswer(Long attemptId, Long studentId, Long questionId, Long optionId) {
        Attempt attempt = findOwnedAttempt(attemptId, studentId);
        checkExpiry(attempt);
        if (!attempt.isInProgress()) {
            throw new BusinessException(Messages.TIME_EXPIRED);
        }
        Question question = attempt.getQuiz().findQuestion(questionId)
                .orElseThrow(() -> new BusinessException(Messages.INVALID_ANSWER));
        AnswerOption option = question.findOption(optionId)
                .orElseThrow(() -> new BusinessException(Messages.INVALID_ANSWER));
        attempt.recordAnswer(question, option);
        attemptRepository.save(attempt);
    }

    /** The student finishes the attempt. Submitting twice, or after expiry, changes nothing. */
    public Attempt submit(Long attemptId, Long studentId) {
        Attempt attempt = findOwnedAttempt(attemptId, studentId);
        if (!checkExpiry(attempt) && attempt.isInProgress()) {
            attempt.submit(now());
            attemptRepository.save(attempt);
        }
        return attempt;
    }

    /**
     * R17: when an in-progress attempt has reached its deadline, auto-submits it (keeping the saved answers).
     *
     * @return true if the attempt was auto-submitted by this call
     */
    public boolean checkExpiry(Attempt attempt) {
        if (attempt.isInProgress() && attempt.isExpiredAt(now())) {
            attempt.submit(attempt.getDeadline());
            attemptRepository.save(attempt);
            return true;
        }
        return false;
    }

    public long remainingSeconds(Attempt attempt) {
        return attempt.remainingSecondsAt(now());
    }

    private Optional<Attempt> findOpenAttempt(Long quizId, Long studentId) {
        return attemptRepository.findFirstByQuizIdAndStudentIdAndStatus(quizId, studentId, AttemptStatus.IN_PROGRESS);
    }

    private Attempt findOwnedAttempt(Long attemptId, Long studentId) {
        return attemptRepository.findById(attemptId)
                .filter(attempt -> attempt.getStudent().getId().equals(studentId))
                .orElseThrow(() -> new BusinessException(Messages.ATTEMPT_NOT_FOUND));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
