package com.quizapp.service;

import com.quizapp.dto.QuizReport;
import com.quizapp.dto.StudentResult;
import com.quizapp.exception.BusinessException;
import com.quizapp.model.Attempt;
import com.quizapp.model.Quiz;
import com.quizapp.model.User;
import com.quizapp.repository.AttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * R20: instructor report - each student's best attempt and the class average of the best scores.
 */
@Service
@Transactional(noRollbackFor = BusinessException.class)
public class ReportService {

    private final QuizService quizService;
    private final AttemptService attemptService;
    private final GradingService gradingService;
    private final AttemptRepository attemptRepository;

    public ReportService(QuizService quizService, AttemptService attemptService,
                         GradingService gradingService, AttemptRepository attemptRepository) {
        this.quizService = quizService;
        this.attemptService = attemptService;
        this.gradingService = gradingService;
        this.attemptRepository = attemptRepository;
    }

    /** R20: report for one of the instructor's quizzes. */
    public QuizReport quizReport(Long quizId, Long instructorId) {
        Quiz quiz = quizService.getQuiz(quizId, instructorId);
        List<Attempt> attempts = attemptRepository.findByQuizIdOrderByStartedAtAsc(quizId);
        attempts.forEach(attemptService::checkExpiry); // R17: time-expired attempts count as submitted

        Map<Long, List<Attempt>> submittedByStudent = attempts.stream()
                .filter(attempt -> !attempt.isInProgress())
                .collect(Collectors.groupingBy(attempt -> attempt.getStudent().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<StudentResult> students = submittedByStudent.values().stream()
                .map(studentAttempts -> studentResult(quiz, studentAttempts))
                .sorted(Comparator.comparing(StudentResult::studentName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        BigDecimal average = classAverage(students.stream().map(StudentResult::bestScore).toList());
        return new QuizReport(quiz, students, average);
    }

    /** R20: the highest of a student's attempt scores. */
    public BigDecimal bestScore(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("A best score needs at least one score");
        }
        return scores.stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    /** R20: mean of the students' best scores, rounded half-up to 2 decimals; 0.00 when there are none. */
    public BigDecimal classAverage(List<BigDecimal> bestScores) {
        if (bestScores == null || bestScores.isEmpty()) {
            return BigDecimal.ZERO.setScale(GradingService.SCORE_SCALE);
        }
        BigDecimal total = bestScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(bestScores.size()), GradingService.SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private StudentResult studentResult(Quiz quiz, List<Attempt> studentAttempts) {
        List<BigDecimal> scores = studentAttempts.stream()
                .map(attempt -> gradingService.grade(attempt).score())
                .toList();
        BigDecimal best = bestScore(scores);
        User student = studentAttempts.get(0).getStudent();
        return new StudentResult(student.getName(), student.getEmail(), studentAttempts.size(), best,
                gradingService.passFail(best, quiz.getPassingScore()), gradingService.letterGrade(best));
    }
}
