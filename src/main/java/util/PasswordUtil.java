package util;

import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Centralized password-security utility.
 *
 * Provides:
 * 1. BCrypt password hashing
 * 2. BCrypt password verification
 * 3. BCrypt hash-format detection
 * 4. Centralized strong-password policy validation
 *
 * Plaintext passwords must never be stored in the database.
 */
public final class PasswordUtil {

    /*
     * IAS1 selected BCrypt for password storage.
     *
     * Cost factor 12 provides a stronger baseline than the previous
     * implementation while remaining practical for the desktop system.
     */
    private static final int COST = 12;

    /*
     * Centralized password-policy baseline.
     *
     * All password creation/change paths should use this same policy
     * instead of maintaining separate rules in individual UI classes.
     */
    private static final int MIN_PASSWORD_LENGTH = 15;

    private static final Pattern LOWERCASE_PATTERN =
            Pattern.compile("[a-z]");

    private static final Pattern UPPERCASE_PATTERN =
            Pattern.compile("[A-Z]");

    private static final Pattern DIGIT_PATTERN =
            Pattern.compile("\\d");

    private static final Pattern SPECIAL_CHARACTER_PATTERN =
            Pattern.compile("[^A-Za-z0-9\\s]");

    /*
     * Supports the standard BCrypt prefixes generated/accepted by
     * common BCrypt implementations.
     */
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile(
                    "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$"
            );

    private PasswordUtil() {
        // Utility class; prevent instantiation.
    }

    /**
     * Hashes a plaintext password using BCrypt and a unique random salt.
     *
     * This method intentionally performs hashing only.
     *
     * Password-policy enforcement is performed separately through
     * validatePolicy()/meetsPolicy(). This separation allows the existing
     * one-time migration utility to securely hash legacy credentials even
     * when those historical passwords do not satisfy the new password
     * policy.
     *
     * @param plaintext plaintext password
     * @return BCrypt hash
     */
    public static String hash(String plaintext) {

        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password must not be null or empty."
            );
        }

        return BCrypt.hashpw(
                plaintext,
                BCrypt.gensalt(COST)
        );
    }

    /**
     * Checks whether a value appears to be a valid BCrypt-formatted hash.
     *
     * @param value value to inspect
     * @return true if the value has valid BCrypt structure
     */
    public static boolean isHash(String value) {

        return value != null
                && BCRYPT_PATTERN.matcher(value).matches();
    }

    /**
     * Verifies plaintext input against an existing BCrypt hash.
     *
     * @param plaintext plaintext password entered by the user
     * @param bcryptHash stored BCrypt hash
     * @return true if the password matches
     */
    public static boolean verify(
            String plaintext,
            String bcryptHash
    ) {

        if (plaintext == null
                || bcryptHash == null
                || !isHash(bcryptHash)) {

            return false;
        }

        try {
            return BCrypt.checkpw(
                    plaintext,
                    bcryptHash
            );

        } catch (IllegalArgumentException ex) {
            /*
             * Malformed or otherwise unsupported hashes fail safely
             * instead of allowing authentication to proceed.
             */
            return false;
        }
    }

    /**
     * Evaluates a plaintext password against the centralized
     * MotorPH strong-password policy.
     *
     * Requirements:
     * - At least 15 characters
     * - At least one lowercase letter
     * - At least one uppercase letter
     * - At least one digit
     * - At least one non-alphanumeric, non-whitespace special character
     *
     * @param password plaintext password to validate
     * @return true when all policy requirements are satisfied
     */
    public static boolean meetsPolicy(String password) {

        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH) {

            return false;
        }

        return LOWERCASE_PATTERN.matcher(password).find()
                && UPPERCASE_PATTERN.matcher(password).find()
                && DIGIT_PATTERN.matcher(password).find()
                && SPECIAL_CHARACTER_PATTERN.matcher(password).find();
    }

    /**
     * Enforces the centralized password policy.
     *
     * Service-layer password creation/change operations should call
     * this method before hashing and persistence so the policy remains
     * effective even when presentation-layer validation is bypassed.
     *
     * @param password plaintext password to validate
     * @throws IllegalArgumentException when the password does not satisfy
     *                                  the required policy
     */
    public static void validatePolicy(String password) {

        if (!meetsPolicy(password)) {

            throw new IllegalArgumentException(
                    "Password must be at least "
                    + MIN_PASSWORD_LENGTH
                    + " characters long and contain at least one "
                    + "lowercase letter, one uppercase letter, one digit, "
                    + "and one special character."
            );
        }
    }

    /**
     * Returns the configured minimum password length.
     *
     * This can be reused by UI components or tests without duplicating
     * the policy value.
     *
     * @return minimum password length
     */
    public static int getMinimumPasswordLength() {
        return MIN_PASSWORD_LENGTH;
    }
}