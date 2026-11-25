package com.quizapp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One student's attempt at a quiz (R14-R17).
 */
@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttemptAnswer> answers = new ArrayList<>();

    protected Attempt() {
        // required by JPA
    }

    public Attempt(Quiz quiz, User student, LocalDateTime startedAt) {
        this.quiz = quiz;
        this.student = student;
        this.startedAt = startedAt;
    }

    /** R17: the moment the timer runs out. */
    public LocalDateTime getDeadline() {
        return startedAt.plusMinutes(quiz.getDurationMinutes());
    }

    /** R17: reaching the deadline exactly counts as expired. */
    public boolean isExpiredAt(LocalDateTime now) {
        return !now.isBefore(getDeadline());
    }

    public long remainingSecondsAt(LocalDateTime now) {
        return Math.max(0, Duration.between(now, getDeadline()).getSeconds());
    }

    public boolean isInProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    public void submit(LocalDateTime at) {
        status = AttemptStatus.SUBMITTED;
        submittedAt = at;
    }

    /** Saves the student's choice for a question, replacing an earlier choice. */
    public void recordAnswer(Question question, AnswerOption option) {
        answers.stream()
                .filter(answer -> answer.getQuestion().equals(question))
                .findFirst()
                .ifPresentOrElse(answer -> answer.changeSelection(option),
                        () -> answers.add(new AttemptAnswer(this, question, option)));
    }

    /** R18: points of the questions answered with the correct option. */
    public int getEarnedPoints() {
        return answers.stream()
                .filter(answer -> answer.getSelectedOption().isCorrect())
                .mapToInt(answer -> answer.getQuestion().getPoints())
                .sum();
    }

    /** Question id -> selected option id, for showing the student's saved choices. */
    public Map<Long, Long> getSelectedOptionIds() {
        return answers.stream().collect(Collectors.toMap(
                answer -> answer.getQuestion().getId(),
                answer -> answer.getSelectedOption().getId()));
    }

    public Long getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public User getStudent() {
        return student;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public List<AttemptAnswer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }
}
