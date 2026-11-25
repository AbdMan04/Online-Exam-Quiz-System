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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private int passingScore;

    @Column(nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status = QuizStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Question> questions = new ArrayList<>();

    protected Quiz() {
        // required by JPA
    }

    public Quiz(String title, int durationMinutes, int passingScore, int maxAttempts, User instructor) {
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.passingScore = passingScore;
        this.maxAttempts = maxAttempts;
        this.instructor = instructor;
    }

    public void addQuestion(Question question) {
        question.attachTo(this);
        questions.add(question);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
    }

    public void publish() {
        status = QuizStatus.PUBLISHED;
    }

    public boolean isPublished() {
        return status == QuizStatus.PUBLISHED;
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public int getTotalPoints() {
        return questions.stream().mapToInt(Question::getPoints).sum();
    }

    public Optional<Question> findQuestion(Long questionId) {
        return questions.stream().filter(question -> Objects.equals(question.getId(), questionId)).findFirst();
    }

    public boolean isOwnedBy(Long instructorId) {
        return instructor != null && instructor.getId() != null && instructor.getId().equals(instructorId);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public QuizStatus getStatus() {
        return status;
    }

    public User getInstructor() {
        return instructor;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}
