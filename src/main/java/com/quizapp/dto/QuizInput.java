package com.quizapp.dto;

/**
 * Quiz settings entered by an instructor (R6-R9). Numbers may be null when a field is left empty.
 */
public record QuizInput(String title, Integer durationMinutes, Integer passingScore, Integer maxAttempts) {
}
