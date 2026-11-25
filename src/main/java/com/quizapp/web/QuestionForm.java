package com.quizapp.web;

import com.quizapp.dto.QuestionInput;
import com.quizapp.dto.QuestionInput.OptionInput;
import com.quizapp.model.AnswerOption;
import com.quizapp.model.Question;
import com.quizapp.validation.ValidationRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Form backing object for adding/editing a question. The page always shows the maximum number of
 * option fields; blank ones are ignored, and one radio button marks the correct option.
 */
public class QuestionForm {

    private String text;
    private Integer points = 1;
    private List<String> options = blankOptions();
    private Integer correctIndex;

    public static QuestionForm from(Question question) {
        QuestionForm form = new QuestionForm();
        form.text = question.getText();
        form.points = question.getPoints();
        List<AnswerOption> existing = question.getOptions();
        for (int i = 0; i < existing.size() && i < form.options.size(); i++) {
            form.options.set(i, existing.get(i).getText());
            if (existing.get(i).isCorrect()) {
                form.correctIndex = i;
            }
        }
        return form;
    }

    public QuestionInput toInput() {
        List<OptionInput> filled = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String optionText = options.get(i);
            if (optionText != null && !optionText.isBlank()) {
                filled.add(new OptionInput(optionText, Objects.equals(correctIndex, i)));
            }
        }
        return new QuestionInput(text, points, filled);
    }

    private static List<String> blankOptions() {
        List<String> blanks = new ArrayList<>();
        for (int i = 0; i < ValidationRules.OPTIONS_MAX; i++) {
            blanks.add("");
        }
        return blanks;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Integer getCorrectIndex() {
        return correctIndex;
    }

    public void setCorrectIndex(Integer correctIndex) {
        this.correctIndex = correctIndex;
    }
}
