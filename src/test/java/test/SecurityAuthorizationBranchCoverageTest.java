package test;

import db.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import util.AuthorizationException;
import util.AuthorizationService;
import util.Permission;
import util.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAuthorizationBranchCoverageTest {

    @AfterEach
    void tearDown() {

        SessionManager.clearSession();
    }

    @Test
    void nullPermissionIsDeniedByBooleanCheck() {

        SessionManager.setSession(
                "C6_AUTH_EMPLOYEE",
                94001,
                "Employee"
        );

        assertFalse(
                AuthorizationService.hasPermission(
                        null
                )
        );
    }

    @Test
    void noSessionHasNoPermission() {

        SessionManager.clearSession();

        assertFalse(
                AuthorizationService.hasPermission(
                        Permission.VIEW_PAYROLL
                )
        );
    }

    @Test
    void selfUserComparisonIsCaseInsensitive() {

        SessionManager.setSession(
                "C6_CASE_USER",
                94002,
                "Employee"
        );

        assertDoesNotThrow(
                () -> AuthorizationService
                        .requireSelfUser(
                                "c6_case_user"
                        )
        );
    }

    @Test
    void nullSelfUserTargetIsDenied() {

        SessionManager.setSession(
                "C6_SELF_USER",
                94003,
                "Employee"
        );

        assertThrows(
                AuthorizationException.class,
                () -> AuthorizationService
                        .requireSelfUser(
                                null
                        )
        );
    }

    @Test
    void itCanUseAdministrativeOverrideForAnotherUser() {

        SessionManager.setSession(
                "C6_IT_OVERRIDE",
                94004,
                "IT"
        );

        assertDoesNotThrow(
                () -> AuthorizationService
                        .requireSelfUserOr(
                                "ANOTHER_USER",
                                Permission.MANAGE_USER_ACCOUNTS
                        )
        );
    }

    @Test
    void employeeCannotUseAdministrativeUserOverride() {

        SessionManager.setSession(
                "C6_EMPLOYEE_OVERRIDE",
                94005,
                "Employee"
        );

        assertThrows(
                AuthorizationException.class,
                () -> AuthorizationService
                        .requireSelfUserOr(
                                "ANOTHER_USER",
                                Permission.MANAGE_USER_ACCOUNTS
                        )
        );
    }

    @Test
    void supervisorCanUseEmployeeResourceOverride() {

        SessionManager.setSession(
                "C6_MANAGER",
                94006,
                "Immediate Supervisor"
        );

        assertDoesNotThrow(
                () -> AuthorizationService
                        .requireSelfEmployeeOr(
                                99999,
                                Permission.APPROVE_LEAVE
                        )
        );
    }

    @Test
    void employeeCannotUseEmployeeResourceOverride() {

        SessionManager.setSession(
                "C6_EMPLOYEE",
                94007,
                "Employee"
        );

        assertThrows(
                AuthorizationException.class,
                () -> AuthorizationService
                        .requireSelfEmployeeOr(
                                99999,
                                Permission.APPROVE_LEAVE
                        )
        );
    }

    @Test
    void unknownRoleFailsClosedForPermissionCheck() {

        SessionManager.setSession(
                "C6_UNKNOWN",
                94008,
                "Unexpected Role"
        );

        assertFalse(
                AuthorizationService.hasPermission(
                        Permission.VIEW_PAYROLL
                )
        );

        assertThrows(
                AuthorizationException.class,
                () -> AuthorizationService
                        .requirePermission(
                                Permission.VIEW_PAYROLL
                        )
        );
    }

    @Test
    void legacySessionInitializerResolvesRoleFromDatabase()
            throws Exception {

        Account account =
                findExistingAccount();

        assertNotNull(
                account
        );

        /*
         * Deliberately use the two-argument initializer.
         * This leaves the role unset and exercises the
         * AuthorizationService database fallback.
         */
        SessionManager.setSession(
                account.userID(),
                account.employeeID()
        );

        assertNull(
                SessionManager.getUserRole()
        );

        assertDoesNotThrow(
                AuthorizationService
                        ::requireAuthenticated
        );

        assertNotNull(
                SessionManager.getUserRole()
        );
    }

    @Test
    void missingLegacyAccountFailsRoleResolutionClosed()
            throws Exception {

        SessionManager.setSession(
                "C6_NONEXISTENT_AUTH_USER",
                94999
        );

        assertThrows(
                AuthorizationException.class,
                AuthorizationService
                        ::requireAuthenticated
        );
    }

    private static Account findExistingAccount()
            throws Exception {

        String sql =
                "SELECT a.userID, e.employeeID " +
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

            return new Account(
                    resultSet.getString(
                            "userID"
                    ),
                    resultSet.getInt(
                            "employeeID"
                    )
            );
        }
    }

    private record Account(
            String userID,
            int employeeID
    ) {
    }
}