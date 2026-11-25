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

    /** R1: the trimmed name must be 1-100 characters. */
    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(Messages.NAME_REQUIRED);
        }
        if (name.trim().length() > ValidationRules.NAME_MAX_LENGTH) {
            throw new BusinessException(Messages.NAME_TOO_LONG);
        }
    }

    /** R1: the trimmed email must match local@domain.tld and be at most 100 characters. */
    public static void validateEmail(String email) {
        if (email == null) {
            throw new BusinessException(Messages.INVALID_EMAIL);
        }
        String trimmed = email.trim();
        if (trimmed.length() > ValidationRules.EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(Messages.INVALID_EMAIL);
        }
    }

    /** Emails are stored and compared trimmed and in lower case. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
