package test;

import db.DatabaseConnection;
import org.junit.jupiter.api.Test;
import pojo.User;
import service.LoginService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityLoginServiceReadCoverageTest {

    @Test
    void unknownAccountLoginReturnsNull() throws Exception {

        LoginService service =
                new LoginService();

        User result =
                service.login(
                        "C6_ACCOUNT_THAT_DOES_NOT_EXIST",
                        "Synthetic-Invalid!2026"
                );

        assertNull(
                result
        );
    }

    @Test
    void unknownAccountHelperQueriesReturnExpectedValues()
            throws Exception {

        LoginService service =
                new LoginService();

        String unknown =
                "C6_ACCOUNT_THAT_DOES_NOT_EXIST";

        assertFalse(
                service.doesUserExist(
                        unknown
                )
        );

        assertEquals(
                -1,
                service.getEmployeeIDByUserID(
                        unknown
                )
        );

        assertNull(
                service.getLockedUntil(
                        unknown
                )
        );
    }

    @Test
    void existingUserIDIsDetectedAndMapsToEmployee()
            throws Exception {

        ExistingAccount account =
                findExistingEmployeeAccount();

        assertNotNull(
                account
        );

        LoginService service =
                new LoginService();

        assertTrue(
                service.doesUserExist(
                        account.userID()
                )
        );

        int employeeID =
                service.getEmployeeIDByUserID(
                        account.userID()
                );

        assertTrue(
                employeeID > 0
        );

        /*
         * The value may legitimately be null or non-null depending
         * on current lockout state. The purpose here is to cover
         * the ResultSet-found branch without changing that state.
         */
        assertDoesNotThrow(
                () -> service.getLockedUntil(
                        account.userID()
                )
        );
    }

    @Test
    void existingEmailIsDetectedWhenAvailable()
            throws Exception {

        ExistingAccount account =
                findExistingEmployeeAccount();

        assertNotNull(
                account
        );

        if (account.email() == null
                || account.email().isBlank()) {

            return;
        }

        LoginService service =
                new LoginService();

        assertTrue(
                service.doesUserExist(
                        account.email()
                )
        );
    }

    private static ExistingAccount
            findExistingEmployeeAccount()
            throws SQLException {

        String sql =
                "SELECT a.userID, e.email " +
                "FROM authentication a " +
                "JOIN employee e " +
                "ON e.userID = a.userID " +
                "WHERE e.employeeID IS NOT NULL " +
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
                    ),
                    resultSet.getString(
                            "email"
                    )
            );
        }
    }

    private record ExistingAccount(
            String userID,
            String email
    ) {
    }
}