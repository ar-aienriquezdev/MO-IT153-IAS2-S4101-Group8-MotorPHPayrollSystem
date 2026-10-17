package test;

import dao.ManageableRequestDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pojo.Leave;
import pojo.Overtime;
import service.LeaveService;
import service.OvertimeService;
import util.RequestValidationUtil;
import util.SessionManager;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IAS2 Control 3 security assurance tests.
 *
 * Verifies:
 * 1. Leave and Overtime business validation is enforced outside the UI.
 * 2. Invalid requests are rejected before DAO execution.
 * 3. New requests cannot self-assign an Approved/Rejected state.
 * 4. Valid requests continue to reach the DAO.
 * 5. Injection-like input remains data rather than being treated as SQL.
 * 6. Leave/Overtime DAO writes retain PreparedStatement parameterization.
 */
public class SecurityServiceLayerValidationTest {

    private static final String TEST_USER_ID = "C3_EMPLOYEE_TEST";
    private static final int TEST_EMPLOYEE_ID = 10008;
    private static final int PENDING_STATUS_ID = 3;

    private LeaveService leaveService;
    private OvertimeService overtimeService;

    private RecordingLeaveDAO leaveDAO;
    private RecordingOvertimeDAO overtimeDAO;

    @BeforeEach
    void setUp() {

        SessionManager.clearSession();

        /*
         * Provide an authenticated Employee context so authorization succeeds
         * and the tests can specifically evaluate Control 3 validation.
         */
        SessionManager.setSession(
                TEST_USER_ID,
                TEST_EMPLOYEE_ID,
                "Employee"
        );

        leaveService = new LeaveService();
        overtimeService = new OvertimeService();

        leaveDAO = new RecordingLeaveDAO();
        overtimeDAO = new RecordingOvertimeDAO();

        injectDAO(
                leaveService,
                "leaveDAO",
                leaveDAO
        );

        injectDAO(
                overtimeService,
                "overtimeDAO",
                overtimeDAO
        );
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    // =========================================================
    // LEAVE VALIDATION
    // =========================================================

    @Test
    void leaveValidationRejectsNullRequest() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> RequestValidationUtil
                                .validateLeaveForCreate(null)
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("leave")
        );
    }

    @Test
    void leaveValidationRejectsReversedDateRange() {

        Leave leave = validLeave();

        leave.setLeaveStart(
                Date.valueOf("2026-10-22")
        );

        leave.setLeaveEnd(
                Date.valueOf("2026-10-20")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveValidationRejectsBlankReason() {

        Leave leave = validLeave();

        leave.setLeaveReason("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveValidationRejectsNegativeAllowance() {

        Leave leave = validLeave();

        leave.setLeaveAllowance(-5.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveValidationRejectsInvalidLeaveType() {

        Leave leave = validLeave();

        leave.setLeaveTypeID(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void invalidLeaveIsRejectedBeforeDAOExecution() {

        Leave leave = validLeave();

        /*
         * This reproduces the important audit weakness:
         * a malformed object reaches LeaveService directly,
         * bypassing Swing validation.
         */
        leave.setLeaveReason("");

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveService.addLeave(leave)
        );

        assertEquals(
                0,
                leaveDAO.addCalls,
                "Invalid Leave must not reach DAO.addRequest()."
        );
    }

    @Test
    void invalidLeaveUpdateIsRejectedBeforeDAOExecution() {

        Leave leave = validLeave();

        leave.setLeaveID(501);
        leave.setLeaveReason("");

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveService.updateLeave(leave)
        );

        assertEquals(
                0,
                leaveDAO.getByIdCalls,
                "Invalid Leave update must fail before DAO lookup."
        );

        assertEquals(
                0,
                leaveDAO.updateCalls,
                "Invalid Leave update must not reach persistence."
        );
    }

    @Test
    void newLeaveCannotSelfApproveAndIsForcedToPending() {

        Leave leave = validLeave();

        /*
         * Simulate a crafted client attempting to create an
         * already-Approved request.
         */
        leave.setApprovalStatusID(1);

        leaveService.addLeave(leave);

        assertEquals(
                1,
                leaveDAO.addCalls
        );

        assertNotNull(
                leaveDAO.lastAdded
        );

        assertEquals(
                PENDING_STATUS_ID,
                leaveDAO.lastAdded.getApprovalStatusID(),
                "New Leave requests must be forced to Pending."
        );

        assertEquals(
                PENDING_STATUS_ID,
                leave.getApprovalStatusID()
        );
    }

    @Test
    void validLeavePassesValidationAndReachesDAO() {

        Leave leave = validLeave();

        assertDoesNotThrow(
                () -> leaveService.addLeave(leave)
        );

        assertEquals(
                1,
                leaveDAO.addCalls
        );

        assertSame(
                leave,
                leaveDAO.lastAdded
        );
    }

    // =========================================================
    // OVERTIME VALIDATION
    // =========================================================

    @Test
    void overtimeValidationRejectsNullRequest() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> RequestValidationUtil
                                .validateOvertimeForCreate(null)
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("overtime")
        );
    }

    @Test
    void overtimeValidationRejectsMissingStartOrEndTime() {

        Overtime missingStart = validOvertime();
        missingStart.setOvertimeStart(null);

        Overtime missingEnd = validOvertime();
        missingEnd.setOvertimeEnd(null);

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> RequestValidationUtil
                                .validateOvertimeForCreate(
                                        missingStart
                                )
                ),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> RequestValidationUtil
                                .validateOvertimeForCreate(
                                        missingEnd
                                )
                )
        );
    }

    @Test
    void overtimeValidationRejectsInvalidTimeOrdering() {

        Overtime overtime = validOvertime();

        overtime.setOvertimeStart(
                Timestamp.valueOf(
                        "2026-10-20 20:00:00"
                )
        );

        overtime.setOvertimeEnd(
                Timestamp.valueOf(
                        "2026-10-20 18:00:00"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateOvertimeForCreate(overtime)
        );
    }

    @Test
    void overtimeValidationRejectsBlankReason() {

        Overtime overtime = validOvertime();

        overtime.setOvertimeReason("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateOvertimeForCreate(overtime)
        );
    }

    @Test
    void invalidOvertimeIsRejectedBeforeDAOExecution() {

        Overtime overtime = validOvertime();

        overtime.setOvertimeReason("");

        assertThrows(
                IllegalArgumentException.class,
                () -> overtimeService
                        .addOvertime(overtime)
        );

        assertEquals(
                0,
                overtimeDAO.addCalls,
                "Invalid Overtime must not reach DAO.addRequest()."
        );
    }

    @Test
    void invalidOvertimeUpdateIsRejectedBeforeDAOExecution() {

        Overtime overtime = validOvertime();

        overtime.setOvertimeID(601);
        overtime.setOvertimeReason("");

        assertThrows(
                IllegalArgumentException.class,
                () -> overtimeService
                        .updateOvertime(overtime)
        );

        assertEquals(
                0,
                overtimeDAO.getByIdCalls,
                "Invalid Overtime update must fail before DAO lookup."
        );

        assertEquals(
                0,
                overtimeDAO.updateCalls,
                "Invalid Overtime update must not reach persistence."
        );
    }

    @Test
    void newOvertimeCannotSelfApproveAndIsForcedToPending() {

        Overtime overtime = validOvertime();

        /*
         * Simulate a crafted client attempting to create an
         * already-Approved overtime request.
         */
        overtime.setApprovalStatusID(1);

        overtimeService.addOvertime(overtime);

        assertEquals(
                1,
                overtimeDAO.addCalls
        );

        assertNotNull(
                overtimeDAO.lastAdded
        );

        assertEquals(
                PENDING_STATUS_ID,
                overtimeDAO.lastAdded.getApprovalStatusID(),
                "New Overtime requests must be forced to Pending."
        );

        assertEquals(
                PENDING_STATUS_ID,
                overtime.getApprovalStatusID()
        );
    }

    @Test
    void validOvertimePassesValidationAndReachesDAO() {

        Overtime overtime = validOvertime();

        assertDoesNotThrow(
                () -> overtimeService
                        .addOvertime(overtime)
        );

        assertEquals(
                1,
                overtimeDAO.addCalls
        );

        assertSame(
                overtime,
                overtimeDAO.lastAdded
        );
    }

    // =========================================================
    // SQL-SAFETY REGRESSION
    // =========================================================

    @Test
    void injectionLikeInputRemainsLiteralBusinessData() {

        String injectionLikeInput =
                "Robert'); DROP TABLE authentication; --";

        Leave leave = validLeave();
        leave.setLeaveReason(injectionLikeInput);

        Overtime overtime = validOvertime();
        overtime.setOvertimeReason(injectionLikeInput);

        assertDoesNotThrow(
                () -> leaveService.addLeave(leave)
        );

        assertDoesNotThrow(
                () -> overtimeService.addOvertime(overtime)
        );

        /*
         * Validation must not attempt to implement homemade SQL escaping.
         * The payload remains ordinary business data and is delegated to
         * the DAO's parameterized PreparedStatement.
         */
        assertEquals(
                injectionLikeInput,
                leaveDAO.lastAdded.getLeaveReason()
        );

        assertEquals(
                injectionLikeInput,
                overtimeDAO.lastAdded.getOvertimeReason()
        );
    }

    @Test
    void leaveDAOStillUsesPreparedStatementParameterization()
            throws Exception {

        String source = Files.readString(
                Path.of(
                        "src/main/java/daoimpl/LeaveDAOImpl.java"
                )
        );

        assertTrue(
                source.contains(
                        "connection.prepareStatement(query)"
                ),
                "Leave DAO must prepare SQL rather than concatenate request data."
        );

        assertTrue(
                source.contains(
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                ),
                "Leave INSERT must retain parameter placeholders."
        );

        assertTrue(
                source.contains(
                        "stmt.setString(4, leave.getLeaveReason())"
                ),
                "Leave reason must be bound as a PreparedStatement parameter."
        );

        assertFalse(
                source.contains("createStatement("),
                "Leave DAO should not regress to raw Statement execution."
        );
    }

    @Test
    void overtimeDAOStillUsesPreparedStatementParameterization()
            throws Exception {

        String source = Files.readString(
                Path.of(
                        "src/main/java/daoimpl/OvertimeDAOImpl.java"
                )
        );

        assertTrue(
                source.contains(
                        "connection.prepareStatement(query)"
                ),
                "Overtime DAO must prepare SQL rather than concatenate request data."
        );

        assertTrue(
                source.contains(
                        "VALUES (?, ?, ?, ?, ?)"
                ),
                "Overtime INSERT must retain parameter placeholders."
        );

        assertTrue(
                source.contains(
                        "stmt.setString(3, overtime.getOvertimeReason())"
                ),
                "Overtime reason must be bound as a PreparedStatement parameter."
        );

        assertFalse(
                source.contains("createStatement("),
                "Overtime DAO should not regress to raw Statement execution."
        );
    }

    // =========================================================
    // TEST DATA HELPERS
    // =========================================================

    private Leave validLeave() {

        Leave leave = new Leave();

        leave.setLeaveAllowance(1.0);

        leave.setLeaveStart(
                Date.valueOf("2026-10-20")
        );

        leave.setLeaveEnd(
                Date.valueOf("2026-10-21")
        );

        leave.setLeaveReason(
                "IAS2 Control 3 valid leave request"
        );

        leave.setDateCreated(
                Date.valueOf("2026-10-17")
        );

        leave.setEmployeeID(
                TEST_EMPLOYEE_ID
        );

        leave.setApprovalStatusID(
                PENDING_STATUS_ID
        );

        leave.setLeaveTypeID(1);

        return leave;
    }

    private Overtime validOvertime() {

        Overtime overtime = new Overtime();

        overtime.setOvertimeStart(
                Timestamp.valueOf(
                        "2026-10-20 18:00:00"
                )
        );

        overtime.setOvertimeEnd(
                Timestamp.valueOf(
                        "2026-10-20 20:00:00"
                )
        );

        overtime.setOvertimeReason(
                "IAS2 Control 3 valid overtime request"
        );

        overtime.setApprovalStatusID(
                PENDING_STATUS_ID
        );

        overtime.setEmployeeID(
                TEST_EMPLOYEE_ID
        );

        return overtime;
    }

    // =========================================================
    // DAO INJECTION
    // =========================================================

    private static void injectDAO(
            Object service,
            String fieldName,
            Object replacementDAO
    ) {

        try {

            Field field =
                    service.getClass()
                            .getDeclaredField(fieldName);

            field.setAccessible(true);

            field.set(
                    service,
                    replacementDAO
            );

        } catch (ReflectiveOperationException e) {

            throw new AssertionError(
                    "Unable to inject test DAO into "
                            + service.getClass()
                                    .getSimpleName(),
                    e
            );
        }
    }

    // =========================================================
    // RECORDING LEAVE DAO
    // =========================================================

    private static class RecordingLeaveDAO
            implements ManageableRequestDAO<Leave> {

        private int addCalls;
        private int updateCalls;
        private int getByIdCalls;

        private Leave lastAdded;
        private Leave existingRequest;

        @Override
        public Leave getRequestByID(int requestID) {

            getByIdCalls++;

            return existingRequest;
        }

        @Override
        public List<Leave> getRequestsByEmployeeID(
                int employeeID
        ) {
            return List.of();
        }

        @Override
        public List<Leave> getAllRequests() {
            return List.of();
        }

        @Override
        public void addRequest(Leave request) {

            addCalls++;

            lastAdded = request;
        }

        @Override
        public void updateApprovalStatus(
                int requestID,
                int approvalStatusID
        ) {
            // Not required by Control 3 tests.
        }

        @Override
        public void deleteRequest(int requestID) {
            // Not required by Control 3 tests.
        }

        @Override
        public void updateRequest(Leave request) {

            updateCalls++;
        }
    }

    // =========================================================
    // RECORDING OVERTIME DAO
    // =========================================================

    private static class RecordingOvertimeDAO
            implements ManageableRequestDAO<Overtime> {

        private int addCalls;
        private int updateCalls;
        private int getByIdCalls;

        private Overtime lastAdded;
        private Overtime existingRequest;

        @Override
        public Overtime getRequestByID(int requestID) {

            getByIdCalls++;

            return existingRequest;
        }

        @Override
        public List<Overtime> getRequestsByEmployeeID(
                int employeeID
        ) {
            return List.of();
        }

        @Override
        public List<Overtime> getAllRequests() {
            return List.of();
        }

        @Override
        public void addRequest(Overtime request) {

            addCalls++;

            lastAdded = request;
        }

        @Override
        public void updateApprovalStatus(
                int requestID,
                int approvalStatusID
        ) {
            // Not required by Control 3 tests.
        }

        @Override
        public void deleteRequest(int requestID) {
            // Not required by Control 3 tests.
        }

        @Override
        public void updateRequest(Overtime request) {

            updateCalls++;
        }
    }
}