package service;

import db.DatabaseConnection;
import pojo.User;
import util.AuditLogger;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

public class LoginService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    /**
     * Authenticates a user using BCrypt verification and timed
     * account lockout after repeated failed attempts.
     *
     * Authentication-related security events are also written
     * through AuditLogger for traceability.
     */
    public User login(String userInput, String plaintextPassword)
            throws SQLException {

        String sql =
                "SELECT a.userID, a.passwordHash, a.accountStatus, " +
                "       a.failed_attempts, a.locked_until, " +
                "       ur.role, e.email, e.positionID, " +
                "       CONCAT(e.firstName,' ',e.lastName) AS username " +
                "FROM authentication a " +
                "JOIN userrole ur ON a.roleID = ur.roleID " +
                "LEFT JOIN employee e ON e.userID = a.userID " +
                "WHERE a.userID = ? OR e.email = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, userInput);
            ps.setString(2, userInput);

            try (ResultSet rs = ps.executeQuery()) {

                /*
                 * Account identifier was not found.
                 *
                 * We intentionally do not place the entered username/email
                 * into the audit details to avoid unnecessarily recording
                 * user-controlled authentication input.
                 */
                if (!rs.next()) {

                    AuditLogger.log(
                            null,
                            "LOGIN_FAILED",
                            "Unknown account identifier",
                            null
                    );

                    return null;
                }

                String userId =
                        rs.getString("userID");

                String storedHash =
                        rs.getString("passwordHash");

                String accountStatus =
                        rs.getString("accountStatus");

                int failedAttempts =
                        rs.getInt("failed_attempts");

                Timestamp lockedUntil =
                        rs.getTimestamp("locked_until");

                String role =
                        rs.getString("role");

                String email =
                        rs.getString("email");

                int positionId =
                        rs.getInt("positionID");

                String username =
                        rs.getString("username");

                Instant now =
                        Instant.now();

                /*
                 * Reject authentication while the account's
                 * lockout window is still active.
                 *
                 * The ACCOUNT_LOCKED event represents the moment
                 * the lock was created. LOGIN_BLOCKED_LOCKED records
                 * any later login attempts while that lock remains active.
                 */
                if (lockedUntil != null
                        && lockedUntil.toInstant().isAfter(now)) {

                    AuditLogger.log(
                            userId,
                            "LOGIN_BLOCKED_LOCKED",
                            "Login blocked while account lockout is active; "
                                    + "locked_until="
                                    + lockedUntil,
                            null
                    );

                    return null;
                }

                /*
                 * If a previous lockout period has already expired,
                 * clear the stale failed-attempt and lockout state.
                 */
                if (lockedUntil != null
                        && !lockedUntil.toInstant().isAfter(now)) {

                    resetLoginState(
                            conn,
                            userId
                    );

                    failedAttempts = 0;
                }

                /*
                 * Password verification happens in the Java
                 * application layer using BCrypt.
                 *
                 * The plaintext password is never included
                 * in the SQL query or audit log.
                 */
                boolean passwordValid =
                        PasswordUtil.verify(
                                plaintextPassword,
                                storedHash
                        );

                /*
                 * Incorrect password:
                 *
                 * Increment the failed-attempt counter.
                 * After the fifth failure, apply a 15-minute lock.
                 */
                if (!passwordValid) {

                    int newFailedAttempts =
                            failedAttempts + 1;

                    Timestamp newLockedUntil =
                            null;

                    if (newFailedAttempts
                            >= MAX_FAILED_ATTEMPTS) {

                        newFailedAttempts =
                                MAX_FAILED_ATTEMPTS;

                        newLockedUntil =
                                Timestamp.from(
                                        now.plus(
                                                Duration.ofMinutes(
                                                        LOCK_MINUTES
                                                )
                                        )
                                );
                    }

                    updateFailureState(
                            conn,
                            userId,
                            newFailedAttempts,
                            newLockedUntil
                    );

                    /*
                     * Fifth failed attempt:
                     * the account has now entered the lockout state.
                     */
                    if (newLockedUntil != null) {

                        AuditLogger.log(
                                userId,
                                "ACCOUNT_LOCKED",
                                "failed_attempts="
                                        + newFailedAttempts
                                        + "; locked_until="
                                        + newLockedUntil,
                                null
                        );

                    /*
                     * Failed attempts before the lockout threshold.
                     */
                    } else {

                        AuditLogger.log(
                                userId,
                                "LOGIN_FAILED",
                                "failed_attempts="
                                        + newFailedAttempts,
                                null
                        );
                    }

                    return null;
                }

                /*
                 * Successful authentication clears any previous
                 * failed-attempt state.
                 */
                resetLoginState(
                        conn,
                        userId
                );

                /*
                 * Record successful authentication.
                 */
                AuditLogger.log(
                        userId,
                        "LOGIN_SUCCESS",
                        "Authentication completed successfully",
                        null
                );

                User user =
                        new User();

                user.setUserID(
                        userId
                );

                /*
                 * Do not expose the stored BCrypt password hash
                 * through the authenticated User/session object.
                 */
                user.setPassword(
                        null
                );

                user.setAccountStatus(
                        accountStatus
                );

                user.setUserRole(
                        role
                );

                user.setEmail(
                        email
                );

                user.setPositionID(
                        positionId
                );

                user.setUsername(
                        username
                );

                return user;
            }
        }
    }

    /**
     * Updates the failed-login state of an authentication account.
     */
    private void updateFailureState(
            Connection conn,
            String userId,
            int failedAttempts,
            Timestamp lockedUntil
    ) throws SQLException {

        String sql =
                "UPDATE authentication " +
                "SET failed_attempts = ?, " +
                "locked_until = ? " +
                "WHERE userID = ?";

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    failedAttempts
            );

            if (lockedUntil == null) {

                ps.setNull(
                        2,
                        java.sql.Types.TIMESTAMP
                );

            } else {

                ps.setTimestamp(
                        2,
                        lockedUntil
                );
            }

            ps.setString(
                    3,
                    userId
            );

            ps.executeUpdate();
        }
    }

    /**
     * Clears failed-attempt and lockout state after either:
     *
     * 1. a successful login, or
     * 2. expiration of the previous lockout period.
     */
    private void resetLoginState(
            Connection conn,
            String userId
    ) throws SQLException {

        String sql =
                "UPDATE authentication " +
                "SET failed_attempts = 0, " +
                "locked_until = NULL " +
                "WHERE userID = ?";

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(
                    1,
                    userId
            );

            ps.executeUpdate();
        }
    }

    /**
     * Retrieves the employee ID associated with a user account.
     */
    public int getEmployeeIDByUserID(
            String userID
    ) throws SQLException {

        String sql =
                "SELECT employeeID " +
                "FROM employee " +
                "WHERE userID = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(
                    1,
                    userID
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                return rs.next()
                        ? rs.getInt("employeeID")
                        : -1;
            }
        }
    }

    /**
     * Checks whether an account exists using either its User ID
     * or associated employee email address.
     */
    public boolean doesUserExist(
            String userInput
    ) throws SQLException {

        String sql =
                "SELECT 1 " +
                "FROM authentication a " +
                "LEFT JOIN employee e " +
                "ON e.userID = a.userID " +
                "WHERE a.userID = ? OR e.email = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(
                    1,
                    userInput
            );

            ps.setString(
                    2,
                    userInput
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                return rs.next();
            }
        }
    }

    /**
     * Returns the lockout expiration timestamp for an account.
     *
     * This method is used by the Swing login UI to display the
     * temporary-lockout message and expiration time.
     *
     * Lockout enforcement itself remains inside login(), not
     * inside the UI.
     *
     * @param userInput User ID or email address
     * @return lockout expiration timestamp, or null if the account
     *         does not exist or does not currently have a lockout time
     * @throws SQLException if the database query fails
     */
    public Timestamp getLockedUntil(
            String userInput
    ) throws SQLException {

        String sql =
                "SELECT a.locked_until " +
                "FROM authentication a " +
                "LEFT JOIN employee e " +
                "ON e.userID = a.userID " +
                "WHERE a.userID = ? OR e.email = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(
                    1,
                    userInput
            );

            ps.setString(
                    2,
                    userInput
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return rs.getTimestamp(
                            "locked_until"
                    );
                }

                return null;
            }
        }
    }
}