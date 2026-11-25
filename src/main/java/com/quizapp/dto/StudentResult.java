package com.quizapp.dto;

import com.quizapp.model.LetterGrade;
import com.quizapp.model.Outcome;

import java.math.BigDecimal;

/** R20: one row of the instructor report - a student's best attempt on a quiz. */
public record StudentResult(String studentName, String studentEmail, int submittedAttempts,
                            BigDecimal bestScore, Outcome outcome, LetterGrade letterGrade) {
}
