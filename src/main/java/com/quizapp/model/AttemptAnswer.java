package com.quizapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The option a student selected for one question in one attempt.
 */
@Entity
@Table(name = "attempt_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"}))
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id")
    private AnswerOption selectedOption;

    protected AttemptAnswer() {
        // required by JPA
    }

    AttemptAnswer(Attempt attempt, Question question, AnswerOption selectedOption) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
    }

    void changeSelection(AnswerOption option) {
        this.selectedOption = option;
    }

    public Long getId() {
        return id;
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public Question getQuestion() {
        return question;
    }

    public AnswerOption getSelectedOption() {
        return selectedOption;
    }
}
