package util;

import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility methods for securely hashing and verifying passwords with BCrypt.
 *
 * Plaintext passwords must never be stored in the database.
 */
public final class PasswordUtil {

    /*
     * IAS1 selected BCrypt for password storage.
     * Cost factor 12 provides a stronger baseline than the previous
     * implementation while remaining practical for the desktop system.
     */
    private static final int COST = 12;

    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private PasswordUtil() {
        // Utility class; prevent instantiation.
    }

    /**
     * Hashes a plaintext password using BCrypt and a unique random salt.
     *
     * @param plaintext plaintext password
     * @return BCrypt hash
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty.");
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
     * @return true if value has valid BCrypt structure
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
    public static boolean verify(String plaintext, String bcryptHash) {
        if (plaintext == null || bcryptHash == null || !isHash(bcryptHash)) {
            return false;
        }

        try {
            return BCrypt.checkpw(plaintext, bcryptHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}