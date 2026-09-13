package service;

import db.DatabaseConnection;
import pojo.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginService {

    /**
     * Authenticates a user using application-side BCrypt verification.
     *
     * The SQL query retrieves the stored password hash only.
     * Plaintext passwords are never compared inside SQL.
     */
    public User login(String userInput, String plaintextPassword) throws SQLException {

        String sql =
            "SELECT a.userID, a.passwordHash, a.accountStatus, ur.role, " +
            "       e.email, e.positionID, " +
            "       CONCAT(e.firstName,' ',e.lastName) AS username " +
            "  FROM authentication a " +
            "  JOIN userrole ur ON a.roleID = ur.roleID " +
            "  LEFT JOIN employee e ON e.userID = a.userID " +
            " WHERE (a.userID = ? OR e.email = ?)";

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

                String storedHash = rs.getString("passwordHash");

                /*
                 * Password verification happens here in Java,
                 * not in the SQL WHERE clause.
                 */
                if (!PasswordUtil.verify(plaintextPassword, storedHash)) {
                    return null;
                }

                User user = new User();

                user.setUserID(
                        rs.getString("userID")
                );

                /*
                 * Do not expose the stored password hash to the authenticated
                 * User object returned by LoginService.
                 */
                user.setPassword(null);

                user.setAccountStatus(
                        rs.getString("accountStatus")
                );

                user.setUserRole(
                        rs.getString("role")
                );

                user.setEmail(
                        rs.getString("email")
                );

                user.setPositionID(
                        rs.getInt("positionID")
                );

                user.setUsername(
                        rs.getString("username")
                );

                return user;
            }
        }
    }

    public int getEmployeeIDByUserID(String userID) throws SQLException {

        String sql =
                "SELECT employeeID FROM employee WHERE userID = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, userID);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? rs.getInt("employeeID")
                        : -1;
            }
        }
    }

    public boolean doesUserExist(String userInput) throws SQLException {

        String sql =
            "SELECT 1 " +
            "FROM authentication a " +
            "LEFT JOIN employee e ON e.userID = a.userID " +
            "WHERE a.userID = ? OR e.email = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, userInput);
            ps.setString(2, userInput);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}