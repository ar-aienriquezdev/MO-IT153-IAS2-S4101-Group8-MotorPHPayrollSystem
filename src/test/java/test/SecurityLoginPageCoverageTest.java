package test;

import db.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ui.base.AbstractLoginPage;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityLoginPageCoverageTest {

    @AfterEach
    void tearDown() {

        closeOpenDialogs();

        SessionManager.clearSession();
    }

    @Test
    void unknownAccountExecutesUnknownAccountBranch()
            throws Exception {

        LoginHarness page =
                new LoginHarness();

        page.setCredentials(
                "C6_ACCOUNT_THAT_DOES_NOT_EXIST",
                "Synthetic-Wrong!2026"
        );

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::executeLogin
        );

        assertEquals(
                Color.PINK,
                page.username().getBackground()
        );

        page.dispose();
    }

    @Test
    void existingAccountWithIncorrectPasswordExecutesPasswordFailureBranch()
            throws Exception {

        ExistingAccount account =
                findExistingAccount();

        assertNotNull(
                account
        );

        AuthState original =
                readAuthState(
                        account.userID()
                );

        try {

            updateAuthState(
                    account.userID(),
                    0,
                    null
            );

            LoginHarness page =
                    new LoginHarness();

            page.setCredentials(
                    account.userID(),
                    "Synthetic-Wrong!2026"
            );

            closeNextDialogSoon();

            assertDoesNotThrow(
                    page::executeLogin
            );

            assertEquals(
                    Color.PINK,
                    page.password().getBackground()
            );

            page.dispose();

        } finally {

            restoreAuthState(
                    account.userID(),
                    original
            );
        }
    }

    @Test
    void lockedAccountExecutesLockoutDisplayBranch()
            throws Exception {

        ExistingAccount account =
                findExistingAccount();

        assertNotNull(
                account
        );

        AuthState original =
                readAuthState(
                        account.userID()
                );

        try {

            Timestamp lockedUntil =
                    new Timestamp(
                            System.currentTimeMillis()
                                    + 5 * 60 * 1000
                    );

            updateAuthState(
                    account.userID(),
                    5,
                    lockedUntil
            );

            LoginHarness page =
                    new LoginHarness();

            page.setCredentials(
                    account.userID(),
                    "Synthetic-Wrong!2026"
            );

            closeNextDialogSoon();

            assertDoesNotThrow(
                    page::executeLogin
            );

            assertEquals(
                    Color.PINK,
                    page.password().getBackground()
            );

            page.dispose();

        } finally {

            restoreAuthState(
                    account.userID(),
                    original
            );
        }
    }

    private static ExistingAccount
            findExistingAccount()
            throws Exception {

        String sql =
                "SELECT a.userID " +
                "FROM authentication a " +
                "JOIN employee e " +
                "ON e.userID = a.userID " +
                "ORDER BY e.employeeID " +
                "LIMIT 1";

        try (
                Connection connection =
                        DatabaseConnection
                                .getInstance()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            if (!resultSet.next()) {
                return null;
            }

            return new ExistingAccount(
                    resultSet.getString(
                            "userID"
                    )
            );
        }
    }

    private static AuthState readAuthState(
            String userID
    ) throws Exception {

        String sql =
                "SELECT failed_attempts, locked_until " +
                "FROM authentication " +
                "WHERE userID = ?";

        try (
                Connection connection =
                        DatabaseConnection
                                .getInstance()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setString(
                    1,
                    userID
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                assertTrue(
                        resultSet.next()
                );

                return new AuthState(
                        resultSet.getInt(
                                "failed_attempts"
                        ),
                        resultSet.getTimestamp(
                                "locked_until"
                        )
                );
            }
        }
    }

    private static void updateAuthState(
            String userID,
            int failedAttempts,
            Timestamp lockedUntil
    ) throws Exception {

        String sql =
                "UPDATE authentication " +
                "SET failed_attempts = ?, " +
                "locked_until = ? " +
                "WHERE userID = ?";

        try (
                Connection connection =
                        DatabaseConnection
                                .getInstance()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setInt(
                    1,
                    failedAttempts
            );

            statement.setTimestamp(
                    2,
                    lockedUntil
            );

            statement.setString(
                    3,
                    userID
            );

            statement.executeUpdate();
        }
    }

    private static void restoreAuthState(
            String userID,
            AuthState state
    ) throws Exception {

        updateAuthState(
                userID,
                state.failedAttempts(),
                state.lockedUntil()
        );
    }

    private static void closeNextDialogSoon() {

        Thread closer =
                new Thread(
                        () -> {

                            try {
                                Thread.sleep(
                                        200
                                );
                            } catch (
                                    InterruptedException e
                            ) {
                                Thread.currentThread()
                                        .interrupt();
                            }

                            SwingUtilities.invokeLater(
                                    SecurityLoginPageCoverageTest
                                            ::closeOpenDialogs
                            );
                        }
                );

        closer.setDaemon(
                true
        );

        closer.start();
    }

    private static void closeOpenDialogs() {

        for (Window window : Window.getWindows()) {

            if (window instanceof JDialog dialog
                    && dialog.isShowing()) {

                dialog.dispose();
            }
        }
    }

    private static class LoginHarness
            extends AbstractLoginPage {

        LoginHarness() {

            usernameField =
                    new JTextField();

            passwordField =
                    new JPasswordField();
        }

        void setCredentials(
                String username,
                String password
        ) {

            usernameField.setText(
                    username
            );

            passwordField.setText(
                    password
            );
        }

        void executeLogin() {

            performLogin();
        }

        JTextField username() {
            return usernameField;
        }

        JPasswordField password() {
            return passwordField;
        }
    }

    private record ExistingAccount(
            String userID
    ) {
    }

    private record AuthState(
            int failedAttempts,
            Timestamp lockedUntil
    ) {
    }
}