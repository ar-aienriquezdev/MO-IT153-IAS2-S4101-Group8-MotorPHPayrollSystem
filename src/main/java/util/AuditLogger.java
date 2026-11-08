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
                sanitizeAndTruncate(userId, MAX_USER_ID);

        String safeAction =
                sanitizeAndTruncate(action, MAX_ACTION);

        String safeDetails =
                sanitizeAndTruncate(details, MAX_DETAILS);

        String safeIpAddress =
                sanitizeAndTruncate(ipAddress, MAX_IP_ADDRESS);

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

    /*
     * Neutralizes characters that can create or mimic extra
     * physical lines in the audit file before the field-length
     * limit is applied, so one audit event can never appear
     * as multiple log entries.
     */
    private static String sanitizeAndTruncate(
            String value,
            int maxLength
    ) {

        if (value == null) {
            return null;
        }

        StringBuilder sanitized =
                new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);

            switch (c) {

                case '\r':
                    sanitized.append("\\r");
                    break;

                case '\n':
                    sanitized.append("\\n");
                    break;

                case '\u2028':
                    sanitized.append("\\u2028");
                    break;

                case '\u2029':
                    sanitized.append("\\u2029");
                    break;

                default:
                    if (isControlCharacter(c)) {
                        sanitized.append(
                                String.format(
                                        "\\u%04X",
                                        (int) c
                                )
                        );
                    } else {
                        sanitized.append(c);
                    }
            }
        }

        return truncate(sanitized.toString(), maxLength);
    }

    /*
     * ISO control characters are the C0 range, DEL, and
     * the C1 range. Carriage return, line feed, and the
     * Unicode line/paragraph separators are handled above.
     */
    private static boolean isControlCharacter(
            char c
    ) {

        return c <= 0x1F
                || c == 0x7F
                || (c >= 0x80 && c <= 0x9F);
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