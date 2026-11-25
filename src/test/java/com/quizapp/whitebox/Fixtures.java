package com.quizapp.whitebox;

import com.quizapp.model.AnswerOption;
import com.quizapp.model.Question;
import com.quizapp.model.Role;
import com.quizapp.model.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Entities with fixed ids for white-box tests (ids are normally assigned by the database).
 */
final class Fixtures {

    private Fixtures() {
    }

    /** Sets the database id of any entity. */
    static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    /** Gives an entity an id only if it does not have one yet (used when a mocked repository "saves"). */
    static <T> T withIdIfMissing(T entity, Long id) {
        return ReflectionTestUtils.getField(entity, "id") == null ? withId(entity, id) : entity;
    }

    static User user(Long id, String name, Role role) {
        return withId(new User(name, name.toLowerCase().replace(' ', '.') + "@uni.edu", "hash", role), id);
    }

    /**
     * A question with {@code optionCount} options; option {@code correctIndex} is correct (-1 = none).
     * Option ids are {@code questionId * 10 + index}.
     */
    static Question question(Long id, String text, int points, int optionCount, int correctIndex) {
        List<AnswerOption> options = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            options.add(withId(new AnswerOption("Option " + (i + 1), i == correctIndex), id * 10 + i));
        }
        return withId(new Question(text, points, options), id);
    }
}
