package com.quizapp.validation;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * R1/R2: validates registration input. Order: name, then email, then password.
 */
public final class AccountValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private AccountValidator() {
    }

    public static void validateRegistration(String name, String email, String password) {
        validateName(name);
        validateEmail(email);
        PasswordValidator.validate(password);
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(Messages.NAME_REQUIRED);
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException(Messages.INVALID_EMAIL);
        }
    }

    /** Emails are stored and compared trimmed and in lower case. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
