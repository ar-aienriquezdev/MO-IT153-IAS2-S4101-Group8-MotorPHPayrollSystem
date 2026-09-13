package util;

import db.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Records security-relevant audit events in both the MySQL audit_log
 * table and the local Log4j2 audit file.
 *
 * Audit failures must not interrupt normal application operation.
 */
public final class AuditLogger {

    private static final Logger LOGGER =
            LogManager.getLogger(AuditLogger.class);

    private static final int MAX_USER_ID = 64;
    private static final int MAX_ACTION = 64;
    private static final int MAX_IP_ADDRESS = 64;
    private static final int MAX_DETAILS = 4000;

    private AuditLogger() {
        // Utility class.
    }

    public static void log(
            String userId,
            String action,
            String details,
            String ipAddress
    ) {

        String safeUserId =
                truncate(userId, MAX_USER_ID);

        String safeAction =
                truncate(action, MAX_ACTION);

        String safeDetails =
                truncate(details, MAX_DETAILS);

        String safeIpAddress =
                truncate(ipAddress, MAX_IP_ADDRESS);

        if (safeAction == null || safeAction.isBlank()) {
            safeAction = "UNSPECIFIED_EVENT";
        }

        String sql =
                "INSERT INTO audit_log " +
                "(ts, user_id, action, details, ip_addr) " +
                "VALUES (CURRENT_TIMESTAMP, ?, ?, ?, ?)";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, safeUserId);
            ps.setString(2, safeAction);
            ps.setString(3, safeDetails);
            ps.setString(4, safeIpAddress);

            ps.executeUpdate();

        } catch (Exception ex) {

            /*
             * An audit-log storage failure must not prevent
             * authentication or crash the application.
             */
            LOGGER.error(
                    "Failed to write audit event to database: " +
                    "user={} action={} error={}",
                    safeUserId,
                    safeAction,
                    ex.getMessage()
            );
        }

        /*
         * Never include plaintext passwords or password hashes
         * in audit details.
         */
        LOGGER.info(
                "[AUDIT] user={} action={} details={} ip={}",
                safeUserId,
                safeAction,
                safeDetails,
                safeIpAddress
        );
    }

    private static String truncate(
            String value,
            int maxLength
    ) {

        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}