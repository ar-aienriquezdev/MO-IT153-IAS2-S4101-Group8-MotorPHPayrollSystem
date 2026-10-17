package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

public class DatabaseConnection {

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
                requireConfigured(
                        firstDefined(
                                System.getProperty("db.url"),
                                System.getenv("DB_URL")
                        ),
                        "Database URL is not configured. "
                        + "Set DB_URL or the db.url system property."
                ).trim();

        String user =
                requireConfigured(
                        firstDefined(
                                System.getProperty("db.user"),
                                System.getenv("DB_USER")
                        ),
                        "Database username is not configured. "
                        + "Set DB_USER or the db.user system property."
                ).trim();

        String password =
                requireConfigured(
                        firstDefined(
                                System.getProperty("db.pass"),
                                System.getenv("DB_PASS")
                        ),
                        "Database password is not configured. "
                        + "Set DB_PASS or the db.pass system property."
                );

        validateJdbcUrl(url);

        Properties connectionProperties =
                new Properties();

        connectionProperties.setProperty(
                "user",
                user
        );

        connectionProperties.setProperty(
                "password",
                password
        );

        /*
         * Control 5:
         * TLS is required centrally by the application.
         * Connector/J must not silently fall back to
         * an unencrypted database connection.
         */
        connectionProperties.setProperty(
                "sslMode",
                "REQUIRED"
        );

        try {

            return DriverManager.getConnection(
                    url,
                    connectionProperties
            );

        } catch (SQLException ex) {

            throw new RuntimeException(
                    "Error establishing secure database connection",
                    ex
            );
        }
    }

    private static String firstDefined(
            String first,
            String second
    ) {

        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    private static String requireConfigured(
            String value,
            String message
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }

        return value;
    }

    private static void validateJdbcUrl(
            String url
    ) {

        String normalized =
                url.toLowerCase(Locale.ROOT);

        if (!normalized.startsWith("jdbc:mysql://")) {

            throw new IllegalStateException(
                    "DB_URL must be a MySQL JDBC URL."
            );
        }

        /*
         * Reject connection-string parameters that could
         * override the centralized TLS policy.
         */
        String[] tlsOverrides = {
            "sslmode=",
            "usessl=",
            "requiressl=",
            "verifyservercertificate="
        };

        for (String override : tlsOverrides) {

            if (normalized.contains(override)) {

                throw new IllegalStateException(
                        "DB_URL must not override TLS settings. "
                        + "TLS is centrally enforced by DatabaseConnection."
                );
            }
        }
    }
}