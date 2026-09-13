package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/payrollsystem_db";

    private static DatabaseConnection instance;

    private DatabaseConnection() {
    }

    public static synchronized DatabaseConnection getInstance() {

        if (instance == null) {
            instance = new DatabaseConnection();
        }

        return instance;
    }

    public Connection getConnection() {

        String url =
                firstDefined(
                        System.getProperty("db.url"),
                        System.getenv("DB_URL"),
                        DEFAULT_URL
                );

        String user =
                firstDefined(
                        System.getProperty("db.user"),
                        System.getenv("DB_USER"),
                        null
                );

        String password =
                firstDefined(
                        System.getProperty("db.pass"),
                        System.getenv("DB_PASS"),
                        null
                );

        if (user == null || user.isBlank()) {
            throw new IllegalStateException(
                    "Database username is not configured. "
                    + "Set DB_USER or the db.user system property."
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Database password is not configured. "
                    + "Set DB_PASS or the db.pass system property."
            );
        }

        try {

            return DriverManager.getConnection(
                    url,
                    user,
                    password
            );

        } catch (SQLException ex) {

            throw new RuntimeException(
                    "Error establishing database connection",
                    ex
            );
        }
    }

    private static String firstDefined(
            String first,
            String second,
            String fallback
    ) {

        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return fallback;
    }
}