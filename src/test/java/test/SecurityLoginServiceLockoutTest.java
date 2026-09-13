package test;

import db.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pojo.User;
import service.LoginService;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SecurityLoginServiceLockoutTest {

    private static final String TEST_PASSWORD =
            "IAS2-Test-Password!2026";

    private static final String BAD_PASSWORD =
            "Definitely-Wrong-Password!";

    private String fixtureUserId;

    private String originalHash;

    private int originalFailedAttempts;

    private Timestamp originalLockedUntil;

    @BeforeEach
    void prepareFixture() throws Exception {

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT userID, passwordHash, " +
                             "failed_attempts, locked_until " +
                             "FROM authentication " +
                             "ORDER BY userID " +
                             "LIMIT 1"
                     );
             ResultSet rs =
                     ps.executeQuery()) {

            assumeTrue(
                    rs.next(),
                    "No authentication account exists for testing."
            );

            fixtureUserId =
                    rs.getString("userID");

            originalHash =
                    rs.getString("passwordHash");

            originalFailedAttempts =
                    rs.getInt("failed_attempts");

            originalLockedUntil =
                    rs.getTimestamp("locked_until");
        }

        /*
         * Temporarily give the selected local test account
         * a synthetic password so no real employee password
         * needs to be hardcoded in this test.
         */
        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "UPDATE authentication " +
                             "SET passwordHash = ?, " +
                             "failed_attempts = 0, " +
                             "locked_until = NULL " +
                             "WHERE userID = ?"
                     )) {

            ps.setString(
                    1,
                    PasswordUtil.hash(TEST_PASSWORD)
            );

            ps.setString(
                    2,
                    fixtureUserId
            );

            ps.executeUpdate();
        }
    }

    @AfterEach
    void restoreFixture() throws Exception {

        if (fixtureUserId == null) {
            return;
        }

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "UPDATE authentication " +
                             "SET passwordHash = ?, " +
                             "failed_attempts = ?, " +
                             "locked_until = ? " +
                             "WHERE userID = ?"
                     )) {

            ps.setString(
                    1,
                    originalHash
            );

            ps.setInt(
                    2,
                    originalFailedAttempts
            );

            if (originalLockedUntil == null) {

                ps.setNull(
                        3,
                        java.sql.Types.TIMESTAMP
                );

            } else {

                ps.setTimestamp(
                        3,
                        originalLockedUntil
                );
            }

            ps.setString(
                    4,
                    fixtureUserId
            );

            ps.executeUpdate();
        }
    }

    @Test
    void failedLoginIncrementsCounter()
            throws Exception {

        LoginService service =
                new LoginService();

        User result =
                service.login(
                        fixtureUserId,
                        BAD_PASSWORD
                );

        assertNull(result);

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT failed_attempts, locked_until " +
                             "FROM authentication " +
                             "WHERE userID = ?"
                     )) {

            ps.setString(
                    1,
                    fixtureUserId
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                assertTrue(
                        rs.next()
                );

                assertEquals(
                        1,
                        rs.getInt("failed_attempts")
                );

                assertNull(
                        rs.getTimestamp("locked_until")
                );
            }
        }
    }

    @Test
    void fifthFailedLoginLocksAccount()
            throws Exception {

        LoginService service =
                new LoginService();

        for (int attempt = 1;
             attempt <= 5;
             attempt++) {

            assertNull(
                    service.login(
                            fixtureUserId,
                            BAD_PASSWORD
                    )
            );
        }

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT failed_attempts, locked_until " +
                             "FROM authentication " +
                             "WHERE userID = ?"
                     )) {

            ps.setString(
                    1,
                    fixtureUserId
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                assertTrue(
                        rs.next()
                );

                assertEquals(
                        5,
                        rs.getInt("failed_attempts")
                );

                Timestamp lockedUntil =
                        rs.getTimestamp("locked_until");

                assertNotNull(
                        lockedUntil
                );

                assertTrue(
                        lockedUntil.toInstant()
                                .isAfter(
                                        Instant.now()
                                )
                );
            }
        }

        /*
         * Even the correct password must be rejected
         * while the account is still locked.
         */
        assertNull(
                service.login(
                        fixtureUserId,
                        TEST_PASSWORD
                )
        );
    }

    @Test
    void successfulLoginResetsFailedAttempts()
            throws Exception {

        LoginService service =
                new LoginService();

        assertNull(
                service.login(
                        fixtureUserId,
                        BAD_PASSWORD
                )
        );

        assertNull(
                service.login(
                        fixtureUserId,
                        BAD_PASSWORD
                )
        );

        User authenticated =
                service.login(
                        fixtureUserId,
                        TEST_PASSWORD
                );

        assertNotNull(
                authenticated
        );

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT failed_attempts, locked_until " +
                             "FROM authentication " +
                             "WHERE userID = ?"
                     )) {

            ps.setString(
                    1,
                    fixtureUserId
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                assertTrue(
                        rs.next()
                );

                assertEquals(
                        0,
                        rs.getInt("failed_attempts")
                );

                assertNull(
                        rs.getTimestamp("locked_until")
                );
            }
        }
    }
}