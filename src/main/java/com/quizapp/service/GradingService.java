package com.quizapp.service;

import com.quizapp.dto.GradeResult;
import com.quizapp.model.Attempt;
import com.quizapp.model.LetterGrade;
import com.quizapp.model.Outcome;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Grading: R18 score calculation, R19 pass/fail and letter grade.
 * Stateless and side-effect free; all arithmetic is decimal so boundaries like 59.99 / 60 are exact.
 */
@Service
public class GradingService {

    public static final int SCORE_SCALE = 2;

    static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);
    static final BigDecimal GRADE_A_MIN = BigDecimal.valueOf(90);
    static final BigDecimal GRADE_B_MIN = BigDecimal.valueOf(80);
    static final BigDecimal GRADE_C_MIN = BigDecimal.valueOf(70);
    static final BigDecimal GRADE_D_MIN = BigDecimal.valueOf(60);

    /** R18: earned / total x 100, rounded half-up to 2 decimals. */
    public BigDecimal calculateScore(int earnedPoints, int totalPoints) {
        if (totalPoints <= 0) {
            throw new IllegalArgumentException("Total points must be positive");
        }
        if (earnedPoints < 0 || earnedPoints > totalPoints) {
            throw new IllegalArgumentException("Earned points must be between 0 and the total points");
        }
        return BigDecimal.valueOf(earnedPoints)
                .multiply(MAX_SCORE)
                .divide(BigDecimal.valueOf(totalPoints), SCORE_SCALE, RoundingMode.HALF_UP);
    }

    /** R19: PASS when the score reaches the quiz's passing score. */
    public Outcome passFail(BigDecimal score, int passingScore) {
        requireValidScore(score);
        return score.compareTo(BigDecimal.valueOf(passingScore)) >= 0 ? Outcome.PASS : Outcome.FAIL;
    }

    /** R19: A (>=90), B (>=80), C (>=70), D (>=60), otherwise F. */
    public LetterGrade letterGrade(BigDecimal score) {
        requireValidScore(score);
        if (score.compareTo(GRADE_A_MIN) >= 0) {
            return LetterGrade.A;
        }
        if (score.compareTo(GRADE_B_MIN) >= 0) {
            return LetterGrade.B;
        }
        if (score.compareTo(GRADE_C_MIN) >= 0) {
            return LetterGrade.C;
        }
        if (score.compareTo(GRADE_D_MIN) >= 0) {
            return LetterGrade.D;
        }
        return LetterGrade.F;
    }

    /** R18 + R19 for one attempt. */
    public GradeResult grade(Attempt attempt) {
        BigDecimal score = calculateScore(attempt.getEarnedPoints(), attempt.getQuiz().getTotalPoints());
        return new GradeResult(score, passFail(score, attempt.getQuiz().getPassingScore()), letterGrade(score));
    }

    private static void requireValidScore(BigDecimal score) {
        if (score == null || score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
    }
}
