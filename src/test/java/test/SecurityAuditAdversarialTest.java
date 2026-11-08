package test;

import db.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import util.AuditLogger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAuditAdversarialTest {

    /*
     * Every row inserted by these tests uses a user_id carrying
     * this synthetic prefix plus a per-run token, so cleanup can
     * target only the rows this test class created.
     */
    private static final String RUN_TOKEN =
            Long.toString(System.nanoTime());

    private static final int MAX_USER_ID = 64;
    private static final int MAX_ACTION = 64;
    private static final int MAX_IP_ADDRESS = 64;
    private static final int MAX_DETAILS = 4000;

    private static final Path AUDIT_LOG =
            Paths.get("logs", "motorph-audit.log");

    private final Set<String> testUserIds =
            ConcurrentHashMap.newKeySet();

    @Test
    void concurrentBurstPersistsAllFiftyEvents()
            throws Exception {

        String userId =
                "C4_ADV_BURST_" + RUN_TOKEN;

        testUserIds.add(userId);

        int eventCount = 50;

        ExecutorService pool =
                Executors.newFixedThreadPool(10);

        List<Future<?>> futures =
                new ArrayList<>();

        for (int i = 0; i < eventCount; i++) {

            final int sequence = i;

            futures.add(
                    pool.submit(() ->
                            AuditLogger.log(
                                    userId,
                                    "C4_ADV_BURST_EVENT",
                                    "burst-event-seq="
                                            + sequence,
                                    "127.0.0.1"
                            )
                    )
            );
        }

        pool.shutdown();

        assertTrue(
                pool.awaitTermination(
                        60,
                        TimeUnit.SECONDS
                ),
                "Concurrent audit burst did not finish."
        );

        for (Future<?> future : futures) {
            future.get();
        }

        assertEquals(
                eventCount,
                countAuditRows(
                        userId,
                        "C4_ADV_BURST_EVENT"
                ),
                "Every concurrent audit event must be persisted."
        );
    }

    @Test
    void oversizedFieldsAreBoundedBeforePersistence()
            throws Exception {

        String marker =
                "C4_ADV_OVERSIZE_" + RUN_TOKEN;

        String oversizedUserId =
                "C4_ADV_OVERSIZE_USER_"
                        + RUN_TOKEN + "_"
                        + "U".repeat(200);

        String oversizedAction =
                "C4_ADV_OVERSIZE_ACTION_"
                        + "A".repeat(200);

        String oversizedDetails =
                marker + "_"
                        + "D".repeat(8000);

        String oversizedIp =
                "C4_ADV_OVERSIZE_IP_"
                        + "9".repeat(200);

        AuditLogger.log(
                oversizedUserId,
                oversizedAction,
                oversizedDetails,
                oversizedIp
        );

        /*
         * The persisted user_id is the first 64 characters of
         * the oversized input.
         */
        String persistedUserId =
                oversizedUserId.substring(0, MAX_USER_ID);

        testUserIds.add(persistedUserId);

        String sql =
                "SELECT user_id, action, details, ip_addr " +
                "FROM audit_log " +
                "WHERE user_id = ? " +
                "ORDER BY id DESC " +
                "LIMIT 1";

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, persistedUserId);

            try (ResultSet rs = ps.executeQuery()) {

                assertTrue(
                        rs.next(),
                        "Oversized audit event was not persisted."
                );

                assertEquals(
                        MAX_USER_ID,
                        rs.getString("user_id").length(),
                        "user_id must be truncated to its limit."
                );

                assertEquals(
                        MAX_ACTION,
                        rs.getString("action").length(),
                        "action must be truncated to its limit."
                );

                assertEquals(
                        MAX_IP_ADDRESS,
                        rs.getString("ip_addr").length(),
                        "ip_addr must be truncated to its limit."
                );

                assertEquals(
                        MAX_DETAILS,
                        rs.getString("details").length(),
                        "details must be truncated to its limit."
                );

                assertTrue(
                        rs.getString("details")
                                .startsWith(marker),
                        "Truncated details must retain the marker."
                );
            }
        }
    }

    @Test
    void blankActionUsesSafeDefaultEventName()
            throws Exception {

        String userId =
                "C4_ADV_BLANK_" + RUN_TOKEN;

        testUserIds.add(userId);

        AuditLogger.log(
                userId,
                "   ",
                "blank-action-payload",
                "127.0.0.1"
        );

        String sql =
                "SELECT action " +
                "FROM audit_log " +
                "WHERE user_id = ? " +
                "ORDER BY id DESC " +
                "LIMIT 1";

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                assertTrue(
                        rs.next(),
                        "Blank-action audit event "
                        + "was not persisted."
                );

                assertEquals(
                        "UNSPECIFIED_EVENT",
                        rs.getString("action"),
                        "Blank action must fall back "
                        + "to UNSPECIFIED_EVENT."
                );
            }
        }
    }

    @Test
    void newlinePayloadIsEncodedAndRemainsOnePhysicalLogLine()
            throws Exception {

        String userId =
                "C4_ADV_NL_" + RUN_TOKEN;

        testUserIds.add(userId);

        String marker =
                "C4_ADV_NL_MARKER_" + RUN_TOKEN;

        String forgedEntry =
                "[AUDIT] user=forged "
                + "action=FORGED_ACTION_" + RUN_TOKEN + " "
                + "details=forged ip=0.0.0.0";

        String payload =
                marker
                + "_BEGIN\n"
                + forgedEntry
                + "\n"
                + marker + "_END";

        AuditLogger.log(
                userId,
                "C4_ADV_NL_EVENT",
                payload,
                "127.0.0.1"
        );

        /*
         * Exactly one database record may be created, its
         * details must carry the newline only as the escaped
         * text "\n", and no raw line break may remain.
         */
        assertEquals(
                1,
                countAuditRows(
                        userId,
                        "C4_ADV_NL_EVENT"
                ),
                "Newline payload must create exactly "
                + "one audit record."
        );

        String persistedDetails =
                latestDetails(
                        userId,
                        "C4_ADV_NL_EVENT"
                );

        assertTrue(
                persistedDetails.contains("\\n"),
                "Persisted details must contain the "
                + "escaped newline text."
        );

        assertFalse(
                persistedDetails.contains("\n")
                        || persistedDetails.contains("\r"),
                "Persisted details must not contain "
                + "a raw line break."
        );

        assertTrue(
                persistedDetails.contains(forgedEntry),
                "Persisted details must retain the full "
                + "payload as escaped text."
        );

        /*
         * The audit file must show the entire event on a
         * single physical line; the injected text must not
         * appear as a separate audit entry.
         */
        List<String> markerLines =
                auditFileLinesContaining(marker);

        assertEquals(
                1,
                markerLines.size(),
                "The event must occupy exactly one "
                + "physical audit-file line."
        );

        String eventLine = markerLines.get(0);

        assertTrue(
                eventLine.contains("\\n"),
                "The audit-file line must contain the "
                + "escaped newline text."
        );

        assertTrue(
                eventLine.contains(forgedEntry),
                "The injected text must remain on the "
                + "same physical line as the event."
        );

        List<String> forgedLines =
                auditFileLinesContaining(
                        "FORGED_ACTION_" + RUN_TOKEN
                );

        assertEquals(
                1,
                forgedLines.size(),
                "The injected payload must not imitate "
                + "a separate audit entry."
        );

        assertSamePhysicalLine(
                eventLine,
                forgedLines.get(0)
        );
    }

    @AfterEach
    void cleanup()
            throws Exception {

        if (testUserIds.isEmpty()) {
            return;
        }

        String sql =
                "DELETE FROM audit_log " +
                "WHERE user_id = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            for (String userId : testUserIds) {
                ps.setString(1, userId);
                ps.executeUpdate();
            }
        }
    }

    private static int countAuditRows(
            String userId,
            String action
    ) throws Exception {

        String sql =
                "SELECT COUNT(*) " +
                "FROM audit_log " +
                "WHERE user_id = ? AND action = ?";

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, action);

            try (ResultSet rs = ps.executeQuery()) {

                assertTrue(rs.next());

                return rs.getInt(1);
            }
        }
    }

    private static String latestDetails(
            String userId,
            String action
    ) throws Exception {

        String sql =
                "SELECT details " +
                "FROM audit_log " +
                "WHERE user_id = ? AND action = ? " +
                "ORDER BY id DESC " +
                "LIMIT 1";

        try (Connection conn =
                     DatabaseConnection.getInstance()
                             .getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, action);

            try (ResultSet rs = ps.executeQuery()) {

                assertTrue(
                        rs.next(),
                        "Expected audit row not found."
                );

                return rs.getString("details");
            }
        }
    }

    /*
     * The RollingFile appender writes synchronously, but the
     * file is polled briefly so the assertion does not depend
     * on filesystem flush timing.
     */
    private static List<String> auditFileLinesContaining(
            String marker
    ) throws Exception {

        long deadline =
                System.nanoTime()
                + TimeUnit.SECONDS.toNanos(10);

        List<String> matches = new ArrayList<>();

        while (true) {

            matches.clear();

            if (Files.exists(AUDIT_LOG)) {

                try (var lines =
                             Files.lines(
                                     AUDIT_LOG,
                                     StandardCharsets.UTF_8
                             )) {

                    lines.filter(line ->
                                    line.contains(marker))
                            .forEach(matches::add);
                }
            }

            if (!matches.isEmpty()
                    || System.nanoTime() > deadline) {
                return matches;
            }

            Thread.sleep(100);
        }
    }

    private static void assertSamePhysicalLine(
            String expected,
            String actual
    ) {

        assertEquals(
                expected,
                actual,
                "The injected payload must share the "
                + "event's single physical line."
        );
    }
}
