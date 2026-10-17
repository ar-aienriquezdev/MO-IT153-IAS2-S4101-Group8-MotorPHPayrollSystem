package test;

import db.DatabaseConnection;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class SecurityDatabaseHardeningTest {

    @Test
    void applicationUsesDedicatedNonAdministrativeAccount()
            throws Exception {

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             Statement statement =
                     conn.createStatement();
             ResultSet rs =
                     statement.executeQuery(
                             "SELECT USER(), CURRENT_USER()"
                     )) {

            assertTrue(rs.next());

            String loginIdentity =
                    rs.getString(1)
                            .toLowerCase(Locale.ROOT);

            String privilegeIdentity =
                    rs.getString(2)
                            .toLowerCase(Locale.ROOT);

            assertFalse(
                    loginIdentity.startsWith("root@"),
                    "Application must not authenticate as root."
            );

            assertFalse(
                    privilegeIdentity.startsWith("root@"),
                    "Application must not execute with root privileges."
            );

            assertTrue(
                    privilegeIdentity.startsWith(
                            "motorph_app@"
                    ),
                    "Expected the dedicated motorph_app account."
            );
        }
    }

    @Test
    void jdbcSessionIsEncryptedWithTls()
            throws Exception {

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             Statement statement =
                     conn.createStatement();
             ResultSet rs =
                     statement.executeQuery(
                             "SHOW SESSION STATUS LIKE 'Ssl_cipher'"
                     )) {

            assertTrue(
                    rs.next(),
                    "Ssl_cipher status should be available."
            );

            String cipher =
                    rs.getString("Value");

            assertNotNull(cipher);

            assertFalse(
                    cipher.isBlank(),
                    "JDBC session must negotiate a TLS cipher."
            );
        }
    }

    @Test
    void serverRequiresSecureTransport()
            throws Exception {

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             Statement statement =
                     conn.createStatement();
             ResultSet rs =
                     statement.executeQuery(
                             "SHOW VARIABLES LIKE "
                             + "'require_secure_transport'"
                     )) {

            assertTrue(rs.next());

            assertEquals(
                    "ON",
                    rs.getString("Value")
                            .toUpperCase(Locale.ROOT),
                    "MySQL must require secure transport."
            );
        }
    }

    @Test
    void databaseListenerIsRestrictedToLoopback()
            throws Exception {

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             Statement statement =
                     conn.createStatement();
             ResultSet rs =
                     statement.executeQuery(
                             "SHOW VARIABLES LIKE 'bind_address'"
                     )) {

            assertTrue(rs.next());

            String bindAddress =
                    rs.getString("Value")
                            .trim()
                            .toLowerCase(Locale.ROOT);

            assertTrue(
                    bindAddress.equals("127.0.0.1")
                    || bindAddress.equals("localhost")
                    || bindAddress.equals("::1"),
                    "Database listener must be restricted "
                    + "to loopback."
            );
        }
    }

    @Test
    void applicationAccountHasOnlyExpectedDatabasePrivileges()
            throws Exception {

        StringBuilder grants =
                new StringBuilder();

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             Statement statement =
                     conn.createStatement();
             ResultSet rs =
                     statement.executeQuery(
                             "SHOW GRANTS"
                     )) {

            while (rs.next()) {

                grants.append(
                        rs.getString(1)
                                .toLowerCase(Locale.ROOT)
                );

                grants.append('\n');
            }
        }

        String grantText =
                grants.toString();

        assertTrue(
                grantText.contains("payrollsystem_db")
        );

        assertTrue(
                grantText.contains("select")
        );

        assertTrue(
                grantText.contains("insert")
        );

        assertTrue(
                grantText.contains("update")
        );

        assertTrue(
                grantText.contains("delete")
        );

        assertFalse(
                grantText.contains("all privileges")
        );

        assertFalse(
                grantText.contains("grant option")
        );

        String[] forbiddenPrivileges = {
            "super",
            "system_user",
            "create user",
            "shutdown",
            "reload",
            "process",
            "file",
            "drop",
            "alter",
            "create"
        };

        for (String forbidden :
                forbiddenPrivileges) {

            assertFalse(
                    grantText.contains(forbidden),
                    "Unexpected database privilege: "
                    + forbidden
            );
        }
    }

    @Test
    void insecureJdbcTlsOverrideIsRejected() {

        String previousUrl =
                System.getProperty("db.url");

        String previousUser =
                System.getProperty("db.user");

        String previousPass =
                System.getProperty("db.pass");

        try {

            System.setProperty(
                    "db.url",
                    "jdbc:mysql://127.0.0.1:3306/"
                    + "payrollsystem_db?sslMode=DISABLED"
            );

            System.setProperty(
                    "db.user",
                    "synthetic-test-user"
            );

            System.setProperty(
                    "db.pass",
                    "synthetic-test-password"
            );

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> DatabaseConnection
                                    .getInstance()
                                    .getConnection()
                    );

            assertTrue(
                    exception.getMessage()
                            .toLowerCase(Locale.ROOT)
                            .contains("tls")
            );

        } finally {

            restoreProperty(
                    "db.url",
                    previousUrl
            );

            restoreProperty(
                    "db.user",
                    previousUser
            );

            restoreProperty(
                    "db.pass",
                    previousPass
            );
        }
    }

    private static void restoreProperty(
            String name,
            String previousValue
    ) {

        if (previousValue == null) {

            System.clearProperty(name);

        } else {

            System.setProperty(
                    name,
                    previousValue
            );
        }
    }
}