package util;

/**
 * Raised when an authenticated user attempts an operation that the current
 * backend authorization policy does not allow.
 */
public class AuthorizationException extends SecurityException {

    public AuthorizationException(String message) {
        super(message);
    }
}
