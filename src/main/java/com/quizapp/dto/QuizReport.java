package com.quizapp.dto;

import com.quizapp.model.Outcome;
import com.quizapp.model.Quiz;

import java.math.BigDecimal;
import java.util.List;

/** R20: a quiz's results - each student's best score and the class average of those best scores. */
public record QuizReport(Quiz quiz, List<StudentResult> students, BigDecimal classAverage) {

    public long passedCount() {
        return students.stream().filter(student -> student.outcome() == Outcome.PASS).count();
    }
}
