package util;

import db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Central backend authorization service for MotorPH.
 *
 * The Swing UI may hide or show pages for usability, but sensitive service
 * methods must call this class before returning or modifying protected data.
 */
public final class AuthorizationService {

    private AuthorizationService() {
    }

    /**
     * Returns true when the current authenticated session has the requested
     * permission. Unknown roles and unauthenticated sessions are denied.
     */
    public static boolean hasPermission(Permission permission) {

        if (permission == null || !SessionManager.hasActiveSession()) {
            return false;
        }

        String role = resolveCurrentRole();

        if (role == null || role.isBlank()) {
            return false;
        }

        return permissionsForRole(role).contains(permission);
    }

    /** Require a valid authenticated application session. */
    public static void requireAuthenticated() {

        if (!SessionManager.hasActiveSession()) {
            deny("AUTHENTICATED_SESSION", "No active authenticated session");
        }

        String role = resolveCurrentRole();

        if (role == null || role.isBlank()) {
            deny("KNOWN_ROLE", "Authenticated session has no recognized role");
        }
    }

    /** Require a specific backend permission. */
    public static void requirePermission(Permission permission) {

        requireAuthenticated();

        if (!hasPermission(permission)) {
            deny(
                    permission.name(),
                    "Role " + resolveCurrentRole()
                            + " is not permitted to perform this operation"
            );
        }
    }

    /** Require access to the currently authenticated employee's own record. */
    public static void requireSelfEmployee(int targetEmployeeID) {

        requireAuthenticated();

        if (SessionManager.getEmployeeID() != targetEmployeeID) {
            deny(
                    "SELF_EMPLOYEE",
                    "Attempted employeeID=" + targetEmployeeID
            );
        }
    }

    /**
     * Allow access when the resource belongs to the current employee or when
     * the role has an administrative permission for the same resource type.
     */
    public static void requireSelfEmployeeOr(
            int targetEmployeeID,
            Permission permission
    ) {

        requireAuthenticated();

        if (SessionManager.getEmployeeID() == targetEmployeeID) {
            return;
        }

        requirePermission(permission);
    }

    /** Require access to the currently authenticated user's own account. */
    public static void requireSelfUser(String targetUserID) {

        requireAuthenticated();

        String currentUserID = SessionManager.getUserID();

        if (targetUserID == null
                || currentUserID == null
                || !currentUserID.equalsIgnoreCase(targetUserID)) {

            deny(
                    "SELF_USER",
                    "Attempted userID=" + targetUserID
            );
        }
    }

    /**
     * Allow access when the target account is the current account or when the
     * current role has the supplied administrative permission.
     */
    public static void requireSelfUserOr(
            String targetUserID,
            Permission permission
    ) {

        requireAuthenticated();

        String currentUserID = SessionManager.getUserID();

        if (targetUserID != null
                && currentUserID != null
                && currentUserID.equalsIgnoreCase(targetUserID)) {
            return;
        }

        requirePermission(permission);
    }

    /**
     * Resolve the current role. Normal logins place the role directly into the
     * session. The database fallback exists so older tests that still use the
     * two-argument SessionManager.setSession(...) remain compatible.
     */
    private static String resolveCurrentRole() {

        String sessionRole = SessionManager.getUserRole();

        if (sessionRole != null && !sessionRole.isBlank()) {
            return sessionRole;
        }

        String userID = SessionManager.getUserID();

        if (userID == null || userID.isBlank()) {
            return null;
        }

        String sql =
                "SELECT ur.role "
                + "FROM authentication a "
                + "JOIN userrole ur ON a.roleID = ur.roleID "
                + "WHERE a.userID = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userID);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    String resolvedRole = rs.getString("role");
                    SessionManager.setUserRole(resolvedRole);
                    return resolvedRole;
                }
            }

        } catch (Exception ex) {
            AuditLogger.log(
                    userID,
                    "AUTHORIZATION_ROLE_LOOKUP_FAILED",
                    "Unable to resolve role for current session",
                    null
            );
        }

        return null;
    }

    /** Return the permission set for one MotorPH role. */
    private static Set<Permission> permissionsForRole(String role) {

        String normalized = role.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {

            case "it" -> EnumSet.of(
                    Permission.VIEW_ALL_EMPLOYEES,
                    Permission.MANAGE_EMPLOYEES,
                    Permission.VIEW_USER_ACCOUNTS,
                    Permission.MANAGE_USER_ACCOUNTS,
                    Permission.MANAGE_ACCOUNT_STATUS
            );

            case "hr" -> EnumSet.of(
                    Permission.VIEW_ALL_EMPLOYEES,
                    Permission.MANAGE_EMPLOYEES,
                    Permission.MANAGE_ACCOUNT_STATUS,
                    Permission.VIEW_ALL_ATTENDANCE,
                    Permission.MANAGE_ATTENDANCE,
                    Permission.VIEW_PAYROLL
            );

            case "finance" -> EnumSet.of(
                    Permission.VIEW_ALL_ATTENDANCE,
                    Permission.VIEW_PAYROLL
            );

            case "immediate supervisor" -> EnumSet.of(
                    Permission.VIEW_ALL_EMPLOYEES,
                    Permission.VIEW_ALL_ATTENDANCE,
                    Permission.APPROVE_LEAVE,
                    Permission.APPROVE_OVERTIME
            );

            case "employee" -> EnumSet.noneOf(Permission.class);

            default -> EnumSet.noneOf(Permission.class);
        };
    }

    /** Log and throw a uniform authorization failure. */
    private static void deny(String required, String details) {

        String userID = SessionManager.getUserID();
        String role = SessionManager.getUserRole();

        AuditLogger.log(
                userID,
                "AUTHORIZATION_DENIED",
                "required=" + required
                        + "; role=" + role
                        + "; " + details,
                null
        );

        throw new AuthorizationException(
                "Access denied: " + details
        );
    }
}
