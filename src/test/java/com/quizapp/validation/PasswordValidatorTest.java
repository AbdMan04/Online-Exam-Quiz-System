package com.quizapp.validation;

import com.quizapp.exception.BusinessException;
import com.quizapp.exception.Messages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PasswordValidator (unit tests)")
class PasswordValidatorTest {

    /** Builds a password of the given length that contains a letter and a digit. */
    private static String passwordOfLength(int length) {
        return "a1" + "b".repeat(length - 2);
    }

    @Nested
    @Tag("blackbox")
    @DisplayName("Partitions")
    class EquivalencePartitions {

        @Test
        @DisplayName("EP1 valid: 8-20 chars with letter and digit")
        void validPassword() {
            assertDoesNotThrow(() -> PasswordValidator.validate("Secure123"));
        }

        @ParameterizedTest(name = "EP invalid: \"{0}\"")
        @ValueSource(strings = {"abc12", "abcdefghij1234567890X", "12345678", "abcdefgh"})
        @DisplayName("EP2-EP5 invalid: too short, too long, no letter, no digit")
        void invalidPasswords(String password) {
            BusinessException ex = assertThrows(BusinessException.class, () -> PasswordValidator.validate(password));
            assertEquals(Messages.INVALID_PASSWORD, ex.getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("EP6 invalid: null or empty")
        void nullOrEmpty(String password) {
            assertFalse(PasswordValidator.isValid(password));
        }
    }

    @Nested
    @Tag("blackbox")
    @DisplayName("Length boundaries")
    class BoundaryValues {

        @ParameterizedTest(name = "length {0} -> valid = {1}")
        @CsvSource({
                "7,  false",
                "8,  true",
                "9,  true",
                "19, true",
                "20, true",
                "21, false"
        })
        void lengthBoundaries(int length, boolean expected) {
            assertEquals(expected, PasswordValidator.isValid(passwordOfLength(length)));
        }
    }

    @Nested
    @Tag("whitebox")
    @DisplayName("Each branch of isValid()")
    class Branches {

        @Test
        @DisplayName("B1 null -> false")
        void nullBranch() {
            assertFalse(PasswordValidator.isValid(null));
        }

        @Test
        @DisplayName("B2 length below minimum -> false")
        void tooShortBranch() {
            assertFalse(PasswordValidator.isValid("a1"));
        }

        @Test
        @DisplayName("B3 length above maximum -> false")
        void tooLongBranch() {
            assertFalse(PasswordValidator.isValid(passwordOfLength(21)));
        }

        @Test
        @DisplayName("B4 no letter -> false (short-circuit)")
        void noLetterBranch() {
            assertFalse(PasswordValidator.isValid("12345678"));
        }

        @Test
        @DisplayName("B5 letter but no digit -> false")
        void noDigitBranch() {
            assertFalse(PasswordValidator.isValid("abcdefgh"));
        }

        @Test
        @DisplayName("B6 all checks pass -> true, validate() does not throw")
        void validBranch() {
            assertTrue(PasswordValidator.isValid("Secure123"));
            assertDoesNotThrow(() -> PasswordValidator.validate("Secure123"));
        }
    }
}
