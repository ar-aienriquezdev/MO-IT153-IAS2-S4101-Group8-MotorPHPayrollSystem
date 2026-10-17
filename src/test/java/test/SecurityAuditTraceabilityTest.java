package test;

import dao.ManageableRequestDAO;
import dao.UserDAO;
import db.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pojo.Leave;
import pojo.Overtime;
import pojo.User;
import service.LeaveService;
import service.OvertimeService;
import service.UserService;
import util.SessionManager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAuditTraceabilityTest {

    private static final String SUPERVISOR_ACTOR =
            "C4_AUDIT_SUPERVISOR";

    private static final String IT_ACTOR =
            "C4_AUDIT_IT";

    /*
     * Synthetic IDs used only for Control 4 audit-traceability testing.
     * The test DAOs below prevent these operations from changing the
     * actual Leave, Overtime, or User business tables.
     */
    private static final int TEST_LEAVE_ID = 910001;
    private static final int TEST_OVERTIME_ID = 920001;

    /**
     * Remove previous Control 4 synthetic audit rows before the suite.
     *
     * We intentionally DO NOT remove the rows after the suite so they
     * can be inspected in MySQL Workbench as Milestone 2 evidence.
     */
    @BeforeAll
    static void clearPreviousControl4Evidence() throws Exception {

        String sql =
                "DELETE FROM audit_log " +
                "WHERE user_id IN (?, ?)";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, SUPERVISOR_ACTOR);
            ps.setString(2, IT_ACTOR);

            ps.executeUpdate();
        }
    }

    @AfterEach
    void clearApplicationSession() {
        SessionManager.clearSession();
    }

    /*
     * ============================================================
     * LEAVE APPROVAL / REJECTION TRACEABILITY
     * ============================================================
     */

    @Test
    void leaveApprovalProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                SUPERVISOR_ACTOR,
                99001,
                "Immediate Supervisor"
        );

        LeaveService service =
                new LeaveService();

        injectField(
                service,
                "leaveDAO",
                noOpRequestDAO()
        );

        service.updateApprovalStatus(
                TEST_LEAVE_ID,
                1
        );

        String details =
                requireAuditEvent(
                        SUPERVISOR_ACTOR,
                        "LEAVE_APPROVED"
                );

        assertTrue(
                details.contains(
                        "target=leave:" + TEST_LEAVE_ID
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );

        assertTrue(
                details.contains(
                        "new_status_id=1"
                )
        );
    }

    @Test
    void leaveRejectionProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                SUPERVISOR_ACTOR,
                99001,
                "Immediate Supervisor"
        );

        LeaveService service =
                new LeaveService();

        injectField(
                service,
                "leaveDAO",
                noOpRequestDAO()
        );

        service.updateApprovalStatus(
                TEST_LEAVE_ID,
                2
        );

        String details =
                requireAuditEvent(
                        SUPERVISOR_ACTOR,
                        "LEAVE_REJECTED"
                );

        assertTrue(
                details.contains(
                        "target=leave:" + TEST_LEAVE_ID
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );

        assertTrue(
                details.contains(
                        "new_status_id=2"
                )
        );
    }

    /*
     * ============================================================
     * OVERTIME APPROVAL / REJECTION TRACEABILITY
     * ============================================================
     */

    @Test
    void overtimeApprovalProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                SUPERVISOR_ACTOR,
                99001,
                "Immediate Supervisor"
        );

        OvertimeService service =
                new OvertimeService();

        injectField(
                service,
                "overtimeDAO",
                noOpRequestDAO()
        );

        service.updateApprovalStatus(
                TEST_OVERTIME_ID,
                1
        );

        String details =
                requireAuditEvent(
                        SUPERVISOR_ACTOR,
                        "OVERTIME_APPROVED"
                );

        assertTrue(
                details.contains(
                        "target=overtime:"
                                + TEST_OVERTIME_ID
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );

        assertTrue(
                details.contains(
                        "new_status_id=1"
                )
        );
    }

    @Test
    void overtimeRejectionProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                SUPERVISOR_ACTOR,
                99001,
                "Immediate Supervisor"
        );

        OvertimeService service =
                new OvertimeService();

        injectField(
                service,
                "overtimeDAO",
                noOpRequestDAO()
        );

        service.updateApprovalStatus(
                TEST_OVERTIME_ID,
                2
        );

        String details =
                requireAuditEvent(
                        SUPERVISOR_ACTOR,
                        "OVERTIME_REJECTED"
                );

        assertTrue(
                details.contains(
                        "target=overtime:"
                                + TEST_OVERTIME_ID
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );

        assertTrue(
                details.contains(
                        "new_status_id=2"
                )
        );
    }

    /*
     * ============================================================
     * USER-ACCOUNT ADMINISTRATION TRACEABILITY
     * ============================================================
     */

    @Test
    void userAccountCreationProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                IT_ACTOR,
                99002,
                "IT"
        );

        UserService service =
                userServiceWithFakeDAO();

        User user =
                newSecureTestUser(
                        "C4_TARGET_CREATED"
                );

        service.addUser(user);

        String details =
                requireAuditEvent(
                        IT_ACTOR,
                        "USER_ACCOUNT_CREATED"
                );

        assertTrue(
                details.contains(
                        "target=user:C4_TARGET_CREATED"
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );
    }

    @Test
    void userAccountUpdateProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                IT_ACTOR,
                99002,
                "IT"
        );

        UserService service =
                userServiceWithFakeDAO();

        User user =
                newSecureTestUser(
                        "C4_TARGET_UPDATED"
                );

        service.updateUser(user);

        String details =
                requireAuditEvent(
                        IT_ACTOR,
                        "USER_ACCOUNT_UPDATED"
                );

        assertTrue(
                details.contains(
                        "target=user:C4_TARGET_UPDATED"
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );
    }

    @Test
    void accountStatusChangeProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                IT_ACTOR,
                99002,
                "IT"
        );

        UserService service =
                userServiceWithFakeDAO();

        service.updateAccountStatus(
                "C4_TARGET_STATUS",
                "Deactivated"
        );

        String details =
                requireAuditEvent(
                        IT_ACTOR,
                        "USER_ACCOUNT_STATUS_CHANGED"
                );

        assertTrue(
                details.contains(
                        "target=user:C4_TARGET_STATUS"
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );

        assertTrue(
                details.contains(
                        "new_status=Deactivated"
                )
        );
    }

    @Test
    void userAccountDeletionProducesStructuredAuditRecord()
            throws Exception {

        SessionManager.setSession(
                IT_ACTOR,
                99002,
                "IT"
        );

        UserService service =
                userServiceWithFakeDAO();

        service.deleteUser(
                "C4_TARGET_DELETED"
        );

        String details =
                requireAuditEvent(
                        IT_ACTOR,
                        "USER_ACCOUNT_DELETED"
                );

        assertTrue(
                details.contains(
                        "target=user:C4_TARGET_DELETED"
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );
    }

    /*
     * ============================================================
     * PASSWORD-CHANGE TRACEABILITY + SECRET NON-DISCLOSURE
     * ============================================================
     */

    @Test
    void passwordChangeIsAuditedWithoutExposingPasswordOrHash()
            throws Exception {

        SessionManager.setSession(
                IT_ACTOR,
                99002,
                "IT"
        );

        UserService service =
                userServiceWithFakeDAO();

        /*
         * Valid 15+ character password satisfying the centralized
         * MotorPH password policy.
         *
         * This value MUST NOT appear in audit_log.
         */
        String plaintextPassword =
                "C4SecureAudit#2026Password";

        service.updateOwnPassword(
                plaintextPassword
        );

        String details =
                requireAuditEvent(
                        IT_ACTOR,
                        "PASSWORD_CHANGED"
                );

        assertTrue(
                details.contains(
                        "target=user:" + IT_ACTOR
                )
        );

        assertTrue(
                details.contains(
                        "result=SUCCESS"
                )
        );

        assertFalse(
                details.contains(
                        plaintextPassword
                ),
                "Audit details must never contain the plaintext password."
        );

        assertFalse(
                details.contains("$2a$")
                        || details.contains("$2b$")
                        || details.contains("$2y$"),
                "Audit details must never contain a BCrypt password hash."
        );
    }

    /*
     * ============================================================
     * TEST SUPPORT
     * ============================================================
     */

    /**
     * Loads the newest matching audit record and fails the test
     * immediately if the expected event was not persisted.
     */
    private static String requireAuditEvent(
            String actor,
            String action
    ) throws SQLException {

        String sql =
                "SELECT details " +
                "FROM audit_log " +
                "WHERE user_id = ? " +
                "AND action = ? " +
                "ORDER BY id DESC " +
                "LIMIT 1";

        try (Connection conn =
                     DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(
                    1,
                    actor
            );

            ps.setString(
                    2,
                    action
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                assertTrue(
                        rs.next(),
                        "Expected audit event was not persisted: "
                                + action
                );

                String details =
                        rs.getString(
                                "details"
                        );

                assertNotNull(
                        details,
                        "Audit details must not be null."
                );

                return details;
            }
        }
    }

    /**
     * Inject a controlled DAO implementation into a service.
     *
     * This allows testing the security/audit behavior of the service
     * without modifying real Leave or Overtime business records.
     */
    private static void injectField(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        Field field =
                target.getClass()
                        .getDeclaredField(
                                fieldName
                        );

        field.setAccessible(
                true
        );

        field.set(
                target,
                value
        );
    }

    /**
     * Dynamic no-op ManageableRequestDAO.
     *
     * updateApprovalStatus() succeeds in-memory, allowing the test
     * to verify that the service subsequently writes the audit event.
     */
    @SuppressWarnings("unchecked")
    private static <T> ManageableRequestDAO<T>
            noOpRequestDAO() {

        return (ManageableRequestDAO<T>)
                Proxy.newProxyInstance(
                        ManageableRequestDAO.class
                                .getClassLoader(),
                        new Class<?>[]{
                                ManageableRequestDAO.class
                        },
                        (proxy, method, args) ->
                                defaultValue(
                                        method.getReturnType()
                                )
                );
    }

    /**
     * Creates UserService backed by a controlled in-memory UserDAO.
     *
     * No actual authentication/user-account rows are created,
     * modified, or deleted by these Control 4 tests.
     */
    private static UserService userServiceWithFakeDAO()
            throws Exception {

        UserService service =
                new UserService();

        UserDAO fakeDAO =
                (UserDAO)
                        Proxy.newProxyInstance(
                                UserDAO.class
                                        .getClassLoader(),
                                new Class<?>[]{
                                        UserDAO.class
                                },
                                (proxy, method, args) -> {

                                    if ("getUserByUserID"
                                            .equals(
                                                    method.getName()
                                            )) {

                                        User user =
                                                new User();

                                        user.setUserID(
                                                (String) args[0]
                                        );

                                        user.setAccountStatus(
                                                "Active"
                                        );

                                        return user;
                                    }

                                    return defaultValue(
                                            method.getReturnType()
                                    );
                                }
                        );

        injectField(
                service,
                "userDAO",
                fakeDAO
        );

        return service;
    }

    /**
     * Creates synthetic user data that satisfies the centralized
     * password policy without using any real MotorPH credentials.
     */
    private static User newSecureTestUser(
            String userID
    ) {

        User user =
                new User();

        user.setUserID(
                userID
        );

        user.setPassword(
                "C4SecureAudit#2026Account"
        );

        user.setAccountStatus(
                "Active"
        );

        user.setUserRole(
                "Employee"
        );

        return user;
    }

    /**
     * Supplies safe default values for methods invoked through
     * dynamic DAO proxies.
     */
    private static Object defaultValue(
            Class<?> returnType
    ) {

        if (!returnType.isPrimitive()) {
            return null;
        }

        if (returnType == boolean.class) {
            return false;
        }

        if (returnType == byte.class) {
            return (byte) 0;
        }

        if (returnType == short.class) {
            return (short) 0;
        }

        if (returnType == int.class) {
            return 0;
        }

        if (returnType == long.class) {
            return 0L;
        }

        if (returnType == float.class) {
            return 0F;
        }

        if (returnType == double.class) {
            return 0D;
        }

        if (returnType == char.class) {
            return '\0';
        }

        return null;
    }
}