package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import service.UserService;
import util.SessionManager;

import static org.junit.jupiter.api.Assertions.*;

class SecurityPasswordPolicyServiceTest {

    @AfterEach
    void clearSession() {
        SessionManager.clearSession();
    }

    @Test
    void directServiceCallCannotBypassPasswordPolicy() {

        /*
         * Simulate an authenticated Employee session.
         *
         * This test intentionally bypasses the Swing credential-update
         * interface and invokes UserService directly.
         */
        SessionManager.setSession(
                "PASSWORD_POLICY_TEST",
                90001,
                "Employee"
        );

        UserService userService =
                new UserService();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.updateOwnPassword(
                                "Weak1!"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("at least 15")
        );
    }
}