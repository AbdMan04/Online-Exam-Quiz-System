package com.quizapp.validation;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;

/**
 * R2: a password must be 8-20 characters long and contain at least one letter and one digit.
 */
public final class PasswordValidator {

    private PasswordValidator() {
    }

    public static void validate(String password) {
        if (!isValid(password)) {
            throw new BusinessException(Messages.INVALID_PASSWORD);
        }
    }

    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        int length = password.length();
        if (length < ValidationRules.PASSWORD_MIN_LENGTH || length > ValidationRules.PASSWORD_MAX_LENGTH) {
            return false;
        }
        return containsLetter(password) && containsDigit(password);
    }

    private static boolean containsLetter(String text) {
        return text.chars().anyMatch(Character::isLetter);
    }

    private static boolean containsDigit(String text) {
        return text.chars().anyMatch(Character::isDigit);
    }
}
