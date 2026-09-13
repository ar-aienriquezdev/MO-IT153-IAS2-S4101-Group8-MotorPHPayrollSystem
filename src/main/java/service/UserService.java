package service;

import dao.UserDAO;
import daoimpl.UserDAOImpl;
import pojo.User;
import util.AuthorizationService;
import util.PasswordUtil;
import util.Permission;
import util.SessionManager;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.List;
import java.util.Set;

public class UserService {

    private static final Set<String> ALLOWED_ACCOUNT_STATUSES =
            Set.of(
                    "Active",
                    "Pending",
                    "Rejected",
                    "Deactivated"
            );

    private UserDAO userDAO;

    public UserService() {
        userDAO = new UserDAOImpl();
    }

    /**
     * A user may retrieve their own account. HR and IT may retrieve another
     * account when performing authorized account-management tasks.
     */
    public User getUserByUserID(String userID) {

        AuthorizationService.requireSelfUserOr(
                userID,
                Permission.MANAGE_ACCOUNT_STATUS
        );

        try {
            return userDAO.getUserByUserID(userID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving user by userID",
                    e
            );
        }
    }

    /** IT-only account lookup by email. */
    public User getUserByEmail(String email) {

        AuthorizationService.requirePermission(
                Permission.VIEW_USER_ACCOUNTS
        );

        try {
            return userDAO.getUserByEmail(email);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving user by email",
                    e
            );
        }
    }

    /** IT-only account lookup by full name. */
    public User getUserByUsername(String username) {

        AuthorizationService.requirePermission(
                Permission.VIEW_USER_ACCOUNTS
        );

        try {
            return userDAO.getUserByUsername(username);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving user by username",
                    e
            );
        }
    }

    /** IT-only list of all application user accounts. */
    public List<User> getAllUsers() {

        AuthorizationService.requirePermission(
                Permission.VIEW_USER_ACCOUNTS
        );

        try {
            return userDAO.getAllUsers();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving all users",
                    e
            );
        }
    }

    /**
     * Full user-account creation is restricted to IT.
     */
    public void addUser(User user) {

        AuthorizationService.requirePermission(
                Permission.MANAGE_USER_ACCOUNTS
        );

        ensurePasswordIsHashed(user);

        try {
            userDAO.addUser(user);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error adding user",
                    e
            );
        }
    }

    /**
     * Full account update can change password, role, and status, so it is
     * restricted to IT. Self-service password changes and HR status changes
     * use narrower methods below.
     */
    public void updateUser(User user) {

        AuthorizationService.requirePermission(
                Permission.MANAGE_USER_ACCOUNTS
        );

        ensurePasswordIsHashed(user);
        performUserUpdate(user);
    }

    /**
     * Updates only the currently authenticated user's password. The caller
     * cannot use this method to change their own role or account status.
     */
    public void updateOwnPassword(String plaintextPassword) {

        AuthorizationService.requireAuthenticated();

        if (plaintextPassword == null || plaintextPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "New password must not be null or blank."
            );
        }

        String currentUserID =
                SessionManager.getUserID();

        try {
            User currentUser =
                    userDAO.getUserByUserID(currentUserID);

            if (currentUser == null) {
                throw new IllegalStateException(
                        "Current user account could not be loaded."
                );
            }

            currentUser.setPassword(
                    PasswordUtil.hash(plaintextPassword)
            );

            performUserUpdate(currentUser);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error updating current user's password",
                    e
            );
        }
    }

    /**
     * HR and IT may change account workflow status without gaining the ability
     * to alter a user's password or role.
     */
    public void updateAccountStatus(
            String userID,
            String newStatus
    ) {

        AuthorizationService.requirePermission(
                Permission.MANAGE_ACCOUNT_STATUS
        );

        if (newStatus == null
                || ALLOWED_ACCOUNT_STATUSES.stream()
                .noneMatch(s -> s.equalsIgnoreCase(newStatus))) {

            throw new IllegalArgumentException(
                    "Unsupported account status: " + newStatus
            );
        }

        try {
            User user =
                    userDAO.getUserByUserID(userID);

            if (user == null) {
                throw new IllegalArgumentException(
                        "User not found: " + userID
                );
            }

            String canonicalStatus =
                    ALLOWED_ACCOUNT_STATUSES.stream()
                    .filter(s -> s.equalsIgnoreCase(newStatus))
                    .findFirst()
                    .orElseThrow();

            user.setAccountStatus(canonicalStatus);

            performUserUpdate(user);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error updating account status",
                    e
            );
        }
    }

    /** HR and IT may remove an account when the business workflow allows it. */
    public void deleteUser(String userID) {

        AuthorizationService.requirePermission(
                Permission.MANAGE_ACCOUNT_STATUS
        );

        try {
            userDAO.deleteUser(userID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error deleting user",
                    e
            );
        }
    }

    /**
     * Shared update helper retaining the reconnect behavior from the original
     * implementation.
     */
    private void performUserUpdate(User user) {

        try {
            userDAO.updateUser(user);

        } catch (SQLException e) {

            String message = e.getMessage();

            boolean closedConnection =
                    e instanceof SQLNonTransientConnectionException
                    || (message != null
                    && message.toLowerCase()
                    .contains("connection is closed"));

            if (closedConnection) {
                try {
                    userDAO = new UserDAOImpl();
                    userDAO.updateUser(user);
                    return;

                } catch (SQLException retryException) {
                    throw new RuntimeException(
                            "Error updating user after reconnect",
                            retryException
                    );
                }
            }

            throw new RuntimeException(
                    "Error updating user",
                    e
            );
        }
    }

    /** Make sure passwords sent through full account administration are hashed. */
    private void ensurePasswordIsHashed(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User must not be null."
            );
        }

        String password =
                user.getPassword();

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "User password must not be null or empty."
            );
        }

        if (!PasswordUtil.isHash(password)) {
            user.setPassword(
                    PasswordUtil.hash(password)
            );
        }
    }
}
