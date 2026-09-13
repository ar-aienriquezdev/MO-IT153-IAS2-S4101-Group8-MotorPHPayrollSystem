package util;

public class SessionManager {

    // Authenticated user session.
    private static String userID;
    private static int employeeID = -1;
    private static String userRole;

    // Selected employee used by Records -> Update flows.
    private static int selectedEmployeeID = -1;

    private SessionManager() {
    }

    /**
     * Preferred IAS2 session initializer. The authenticated role is retained
     * so backend services can enforce role-based authorization.
     */
    public static void setSession(
            String userID,
            int employeeID,
            String userRole
    ) {
        SessionManager.userID = userID;
        SessionManager.employeeID = employeeID;
        SessionManager.userRole = userRole;
    }

    /**
     * Backward-compatible initializer retained for older tests/pages.
     * AuthorizationService will resolve the role from the database on demand
     * when this overload is used.
     */
    public static void setSession(
            String userID,
            int employeeID
    ) {
        setSession(userID, employeeID, null);
    }

    public static String getUserID() {
        return userID;
    }

    public static int getEmployeeID() {
        return employeeID;
    }

    public static String getUserRole() {
        return userRole;
    }

    /**
     * Used by AuthorizationService after resolving a legacy session's role.
     */
    public static void setUserRole(String userRole) {
        SessionManager.userRole = userRole;
    }

    public static boolean hasActiveSession() {
        return userID != null
                && !userID.isBlank()
                && employeeID > 0;
    }

    /** Used by Records -> Update flow to stash the chosen row's ID. */
    public static void setSelectedEmployeeID(int id) {
        selectedEmployeeID = id;
    }

    public static int getSelectedEmployeeID() {
        return selectedEmployeeID;
    }

    public static void clearSession() {
        userID = null;
        employeeID = -1;
        userRole = null;
        selectedEmployeeID = -1;
    }
}
