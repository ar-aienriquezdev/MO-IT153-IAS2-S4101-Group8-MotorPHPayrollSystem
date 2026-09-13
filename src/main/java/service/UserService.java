package service;

import dao.UserDAO;
import daoimpl.UserDAOImpl;
import pojo.User;
import util.PasswordUtil;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.List;

public class UserService {

    private UserDAO userDAO;

    public UserService() {
        userDAO = new UserDAOImpl();
    }

    public User getUserByUserID(String userID) {
        try {
            return userDAO.getUserByUserID(userID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving user by userID",
                    e
            );
        }
    }

    public User getUserByEmail(String email) {
        try {
            return userDAO.getUserByEmail(email);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving user by email",
                    e
            );
        }
    }

    public User getUserByUsername(String username) {
        try {
            return userDAO.getUserByUsername(username);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving user by username",
                    e
            );
        }
    }

    public List<User> getAllUsers() {
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
     * Adds a new user.
     *
     * Any plaintext password is converted to BCrypt before being
     * passed to the DAO.
     */
    public void addUser(User user) {

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
     * Updates a user.
     *
     * If the supplied password is plaintext, it is BCrypt-hashed.
     * Existing BCrypt hashes are not hashed again.
     */
    public void updateUser(User user) {

        ensurePasswordIsHashed(user);

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

    public void deleteUser(String userID) {
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
     * Makes sure passwords are never sent to the DAO in plaintext.
     */
    private void ensurePasswordIsHashed(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User must not be null."
            );
        }

        String password = user.getPassword();

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "User password must not be null or empty."
            );
        }

        /*
         * Avoid double-hashing a password that is already BCrypt.
         */
        if (!PasswordUtil.isHash(password)) {
            user.setPassword(
                    PasswordUtil.hash(password)
            );
        }
    }
}