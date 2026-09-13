package test;

import db.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import util.AuditLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAuditLoggerTest {

    private Long insertedId;

    private static final String TEST_USER =
            "AUDITTEST";

    private static final String TEST_ACTION =
            "TEST_AUDIT_EVENT";

    @Test
    void auditEventIsWrittenToDatabase()
            throws Exception {

        AuditLogger.log(
                TEST_USER,
                TEST_ACTION,
                "Synthetic IAS2 audit test",
                "127.0.0.1"
        );

        String sql =
                "SELECT id, user_id, action, details, " +
                "ip_addr, ts " +
                "FROM audit_log " +
                "WHERE user_id = ? AND action = ? " +
                "ORDER BY id DESC " +
                "LIMIT 1";

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, TEST_USER);
            ps.setString(2, TEST_ACTION);

            try (ResultSet rs =
                         ps.executeQuery()) {

                assertTrue(
                        rs.next(),
                        "Expected an audit_log row."
                );

                insertedId =
                        rs.getLong("id");

                assertEquals(
                        TEST_USER,
                        rs.getString("user_id")
                );

                assertEquals(
                        TEST_ACTION,
                        rs.getString("action")
                );

                assertEquals(
                        "Synthetic IAS2 audit test",
                        rs.getString("details")
                );

                assertEquals(
                        "127.0.0.1",
                        rs.getString("ip_addr")
                );

                assertNotNull(
                        rs.getTimestamp("ts")
                );
            }
        }
    }

    @AfterEach
    void cleanup()
            throws Exception {

        if (insertedId == null) {
            return;
        }

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "DELETE FROM audit_log " +
                             "WHERE id = ?"
                     )) {

            ps.setLong(
                    1,
                    insertedId
            );

            ps.executeUpdate();
        }
    }
}