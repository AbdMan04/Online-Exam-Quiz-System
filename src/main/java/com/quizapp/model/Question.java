package com.quizapp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private int points;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<AnswerOption> options = new ArrayList<>();

    protected Question() {
        // required by JPA
    }

    public Question(String text, int points, List<AnswerOption> options) {
        update(text, points, options);
    }

    /** Replaces the question's content (used when an instructor edits it). */
    public void update(String text, int points, List<AnswerOption> newOptions) {
        this.text = text;
        this.points = points;
        this.options.clear();
        newOptions.forEach(this::addOption);
    }

    private void addOption(AnswerOption option) {
        option.attachTo(this);
        options.add(option);
    }

    void attachTo(Quiz quiz) {
        this.quiz = quiz;
    }

    public Optional<AnswerOption> findOption(Long optionId) {
        return options.stream().filter(option -> Objects.equals(option.getId(), optionId)).findFirst();
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public int getPoints() {
        return points;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public List<AnswerOption> getOptions() {
        return Collections.unmodifiableList(options);
    }
}
