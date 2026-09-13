package service;

import db.DatabaseConnection;
import pojo.User;
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

                if (!rs.next()) {
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

                Instant now = Instant.now();

                /*
                 * Reject login while the lockout window is active.
                 */
                if (lockedUntil != null
                        && lockedUntil.toInstant().isAfter(now)) {

                    return null;
                }

                /*
                 * If the old lockout period has already expired,
                 * clear the previous counters before continuing.
                 */
                if (lockedUntil != null
                        && !lockedUntil.toInstant().isAfter(now)) {

                    resetLoginState(
                            conn,
                            userId
                    );

                    failedAttempts = 0;
                }

                boolean passwordValid =
                        PasswordUtil.verify(
                                plaintextPassword,
                                storedHash
                        );

                /*
                 * Wrong password:
                 * increment failed-attempt counter and apply
                 * a 15-minute lock after the fifth failure.
                 */
                if (!passwordValid) {

                    int newFailedAttempts =
                            failedAttempts + 1;

                    Timestamp newLockedUntil = null;

                    if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {

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

                    return null;
                }

                /*
                 * Successful authentication clears all
                 * failed-attempt and lockout state.
                 */
                resetLoginState(
                        conn,
                        userId
                );

                User user = new User();

                user.setUserID(userId);

                /*
                 * Do not expose the password hash through
                 * the authenticated session object.
                 */
                user.setPassword(null);

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

    private void updateFailureState(
            Connection conn,
            String userId,
            int failedAttempts,
            Timestamp lockedUntil
    ) throws SQLException {

        String sql =
                "UPDATE authentication " +
                "SET failed_attempts = ?, locked_until = ? " +
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

    private void resetLoginState(
            Connection conn,
            String userId
    ) throws SQLException {

        String sql =
                "UPDATE authentication " +
                "SET failed_attempts = 0, locked_until = NULL " +
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

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next()
                        ? rs.getInt("employeeID")
                        : -1;
            }
        }
    }

    public boolean doesUserExist(
            String userInput
    ) throws SQLException {

        String sql =
                "SELECT 1 " +
                "FROM authentication a " +
                "LEFT JOIN employee e ON e.userID = a.userID " +
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

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next();
            }
        }
    }
    
    
        /**
     * Returns the lockout expiration timestamp for an account.
     *
     * @param userInput User ID or email address
     * @return lockout expiration timestamp, or null if the account
     *         does not exist or is not currently assigned a lockout time
     * @throws SQLException if the database query fails
     */
    public Timestamp getLockedUntil(String userInput)
            throws SQLException {

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