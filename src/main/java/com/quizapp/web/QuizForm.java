package com.quizapp.web;

import com.quizapp.dto.QuizInput;

/**
 * Form backing object for the "create quiz" page.
 */
public class QuizForm {

    private String title;
    private Integer durationMinutes = 30;
    private Integer passingScore = 60;
    private Integer maxAttempts = 1;

    public QuizInput toInput() {
        return new QuizInput(title, durationMinutes, passingScore, maxAttempts);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(Integer passingScore) {
        this.passingScore = passingScore;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
