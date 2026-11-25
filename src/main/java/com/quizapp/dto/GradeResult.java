package com.quizapp.dto;

import com.quizapp.model.LetterGrade;
import com.quizapp.model.Outcome;

import java.math.BigDecimal;

/** R18-R19: the graded result of one submitted attempt. */
public record GradeResult(BigDecimal score, Outcome outcome, LetterGrade letterGrade) {
}
