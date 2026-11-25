package com.quizapp.dto;

import java.util.List;

/**
 * A question entered by an instructor (R10-R11).
 */
public record QuestionInput(String text, Integer points, List<OptionInput> options) {

    public record OptionInput(String text, boolean correct) {
    }
}
