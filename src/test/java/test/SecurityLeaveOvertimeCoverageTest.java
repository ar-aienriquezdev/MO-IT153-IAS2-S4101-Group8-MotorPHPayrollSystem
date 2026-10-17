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
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Control 6 targeted coverage expansion.
 *
 * Extends the existing Control 3 security tests by exercising additional
 * validation, ownership, retrieval, update, and delete branches in
 * LeaveService and OvertimeService.
 *
 * Recording DAOs are injected so these tests remain deterministic and do not
 * modify application database records.
 */
public class SecurityLeaveOvertimeCoverageTest {

    private static final String TEST_USER_ID =
            "C6_COVERAGE_EMPLOYEE";

    private static final int TEST_EMPLOYEE_ID =
            10008;

    private static final int PENDING_STATUS_ID =
            3;

    private LeaveService leaveService;
    private OvertimeService overtimeService;

    private RecordingLeaveDAO leaveDAO;
    private RecordingOvertimeDAO overtimeDAO;

    @BeforeEach
    void setUp() {

        SessionManager.clearSession();

        SessionManager.setSession(
                TEST_USER_ID,
                TEST_EMPLOYEE_ID,
                "Employee"
        );

        leaveService =
                new LeaveService();

        overtimeService =
                new OvertimeService();

        leaveDAO =
                new RecordingLeaveDAO();

        overtimeDAO =
                new RecordingOvertimeDAO();

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
    // LEAVE VALIDATION COVERAGE
    // =========================================================

    @Test
    void leaveCreateRejectsInvalidEmployeeID() {

        Leave leave =
                validLeave();

        leave.setEmployeeID(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveCreateRejectsMissingStartDate() {

        Leave leave =
                validLeave();

        leave.setLeaveStart(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveCreateRejectsMissingEndDate() {

        Leave leave =
                validLeave();

        leave.setLeaveEnd(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveCreateRejectsNonFiniteAllowance() {

        Leave leave =
                validLeave();

        leave.setLeaveAllowance(
                Double.NaN
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveCreateRejectsInvalidApprovalStatus() {

        Leave leave =
                validLeave();

        leave.setApprovalStatusID(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(leave)
        );
    }

    @Test
    void leaveUpdateRejectsInvalidLeaveID() {

        Leave leave =
                validLeave();

        leave.setLeaveID(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForUpdate(leave)
        );
    }

    // =========================================================
    // LEAVE SERVICE COVERAGE
    // =========================================================

    @Test
    void validLeaveUpdatePreservesBackendOwnerAndApprovalStatus() {

        Leave existing =
                validLeave();

        existing.setLeaveID(501);
        existing.setEmployeeID(
                TEST_EMPLOYEE_ID
        );
        existing.setApprovalStatusID(2);

        leaveDAO.existingRequest =
                existing;

        Leave incoming =
                validLeave();

        incoming.setLeaveID(501);

        /*
         * Crafted client-side status. The service must restore the
         * backend-controlled persisted status before the DAO update.
         */
        incoming.setApprovalStatusID(1);

        assertDoesNotThrow(
                () -> leaveService
                        .updateLeave(incoming)
        );

        assertEquals(
                1,
                leaveDAO.updateCalls
        );

        assertSame(
                incoming,
                leaveDAO.lastUpdated
        );

        assertEquals(
                TEST_EMPLOYEE_ID,
                incoming.getEmployeeID()
        );

        assertEquals(
                2,
                incoming.getApprovalStatusID(),
                "Existing backend approval state must be preserved."
        );
    }

    @Test
    void leaveLookupByIDReturnsOwnedRequest() {

        Leave existing =
                validLeave();

        existing.setLeaveID(502);

        leaveDAO.existingRequest =
                existing;

        Leave returned =
                leaveService.getLeaveByID(502);

        assertSame(
                existing,
                returned
        );

        assertEquals(
                1,
                leaveDAO.getByIdCalls
        );
    }

    @Test
    void leaveLookupByEmployeeReturnsDAOResult() {

        Leave existing =
                validLeave();

        existing.setLeaveID(503);

        leaveDAO.existingRequest =
                existing;

        List<Leave> results =
                leaveService.getLeavesByEmployeeID(
                        TEST_EMPLOYEE_ID
                );

        assertEquals(
                1,
                results.size()
        );

        assertSame(
                existing,
                results.get(0)
        );
    }

    @Test
    void deleteOwnedLeaveReachesDAO() {

        Leave existing =
                validLeave();

        existing.setLeaveID(504);

        leaveDAO.existingRequest =
                existing;

        assertDoesNotThrow(
                () -> leaveService
                        .deleteLeave(504)
        );

        assertEquals(
                1,
                leaveDAO.deleteCalls
        );
    }

    @Test
    void missingLeaveLookupReturnsNullForAuthenticatedUser() {

        leaveDAO.existingRequest =
                null;

        assertNull(
                leaveService.getLeaveByID(
                        999999
                )
        );
    }

    @Test
    void deletingMissingLeaveDoesNotCallDeleteDAO() {

        leaveDAO.existingRequest =
                null;

        assertDoesNotThrow(
                () -> leaveService
                        .deleteLeave(999999)
        );

        assertEquals(
                0,
                leaveDAO.deleteCalls
        );
    }

    // =========================================================
    // OVERTIME VALIDATION COVERAGE
    // =========================================================

    @Test
    void overtimeCreateRejectsInvalidEmployeeID() {

        Overtime overtime =
                validOvertime();

        overtime.setEmployeeID(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateOvertimeForCreate(
                                overtime
                        )
        );
    }

    @Test
    void overtimeCreateRejectsInvalidApprovalStatus() {

        Overtime overtime =
                validOvertime();

        overtime.setApprovalStatusID(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateOvertimeForCreate(
                                overtime
                        )
        );
    }

    @Test
    void overtimeUpdateRejectsInvalidOvertimeID() {

        Overtime overtime =
                validOvertime();

        overtime.setOvertimeID(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateOvertimeForUpdate(
                                overtime
                        )
        );
    }

    @Test
    void overtimeCreateRejectsEqualStartAndEndTimes() {

        Overtime overtime =
                validOvertime();

        Timestamp sameTime =
                Timestamp.valueOf(
                        "2026-10-20 18:00:00"
                );

        overtime.setOvertimeStart(
                sameTime
        );

        overtime.setOvertimeEnd(
                sameTime
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateOvertimeForCreate(
                                overtime
                        )
        );
    }

    // =========================================================
    // OVERTIME SERVICE COVERAGE
    // =========================================================

    @Test
    void validOvertimeUpdatePreservesBackendOwnerAndApprovalStatus() {

        Overtime existing =
                validOvertime();

        existing.setOvertimeID(601);
        existing.setEmployeeID(
                TEST_EMPLOYEE_ID
        );
        existing.setApprovalStatusID(2);

        overtimeDAO.existingRequest =
                existing;

        Overtime incoming =
                validOvertime();

        incoming.setOvertimeID(601);

        /*
         * Crafted Approved value. The service must restore the
         * backend-controlled persisted approval state.
         */
        incoming.setApprovalStatusID(1);

        assertDoesNotThrow(
                () -> overtimeService
                        .updateOvertime(incoming)
        );

        assertEquals(
                1,
                overtimeDAO.updateCalls
        );

        assertSame(
                incoming,
                overtimeDAO.lastUpdated
        );

        assertEquals(
                TEST_EMPLOYEE_ID,
                incoming.getEmployeeID()
        );

        assertEquals(
                2,
                incoming.getApprovalStatusID(),
                "Existing backend approval state must be preserved."
        );
    }

    @Test
    void overtimeLookupByIDReturnsOwnedRequest() {

        Overtime existing =
                validOvertime();

        existing.setOvertimeID(602);

        overtimeDAO.existingRequest =
                existing;

        Overtime returned =
                overtimeService.getOvertimeByID(
                        602
                );

        assertSame(
                existing,
                returned
        );

        assertEquals(
                1,
                overtimeDAO.getByIdCalls
        );
    }

    @Test
    void overtimeLookupByEmployeeReturnsDAOResult() {

        Overtime existing =
                validOvertime();

        existing.setOvertimeID(603);

        overtimeDAO.existingRequest =
                existing;

        List<Overtime> results =
                overtimeService
                        .getOvertimesByEmployeeID(
                                TEST_EMPLOYEE_ID
                        );

        assertEquals(
                1,
                results.size()
        );

        assertSame(
                existing,
                results.get(0)
        );
    }

    @Test
    void deleteOwnedOvertimeReachesDAO() {

        Overtime existing =
                validOvertime();

        existing.setOvertimeID(604);

        overtimeDAO.existingRequest =
                existing;

        assertDoesNotThrow(
                () -> overtimeService
                        .deleteOvertime(604)
        );

        assertEquals(
                1,
                overtimeDAO.deleteCalls
        );
    }

    @Test
    void missingOvertimeLookupReturnsNullForAuthenticatedUser() {

        overtimeDAO.existingRequest =
                null;

        assertNull(
                overtimeService.getOvertimeByID(
                        999999
                )
        );
    }

    @Test
    void deletingMissingOvertimeDoesNotCallDeleteDAO() {

        overtimeDAO.existingRequest =
                null;

        assertDoesNotThrow(
                () -> overtimeService
                        .deleteOvertime(999999)
        );

        assertEquals(
                0,
                overtimeDAO.deleteCalls
        );
    }

    // =========================================================
    // TEST DATA HELPERS
    // =========================================================

    private Leave validLeave() {

        Leave leave =
                new Leave();

        leave.setLeaveAllowance(1.0);

        leave.setLeaveStart(
                Date.valueOf(
                        "2026-10-20"
                )
        );

        leave.setLeaveEnd(
                Date.valueOf(
                        "2026-10-21"
                )
        );

        leave.setLeaveReason(
                "Control 6 coverage leave"
        );

        leave.setDateCreated(
                Date.valueOf(
                        "2026-10-18"
                )
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

        Overtime overtime =
                new Overtime();

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
                "Control 6 coverage overtime"
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
    // REFLECTION-BASED TEST DAO INJECTION
    // =========================================================

    private static void injectDAO(
            Object service,
            String fieldName,
            Object replacementDAO
    ) {

        try {

            Field field =
                    service.getClass()
                            .getDeclaredField(
                                    fieldName
                            );

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
        private int deleteCalls;
        private int getByIdCalls;

        private Leave lastAdded;
        private Leave lastUpdated;
        private Leave existingRequest;

        @Override
        public Leave getRequestByID(
                int requestID
        ) {

            getByIdCalls++;

            return existingRequest;
        }

        @Override
        public List<Leave> getRequestsByEmployeeID(
                int employeeID
        ) {

            if (existingRequest == null) {
                return List.of();
            }

            return List.of(
                    existingRequest
            );
        }

        @Override
        public List<Leave> getAllRequests() {

            if (existingRequest == null) {
                return List.of();
            }

            return List.of(
                    existingRequest
            );
        }

        @Override
        public void addRequest(
                Leave request
        ) {

            addCalls++;

            lastAdded =
                    request;
        }

        @Override
        public void updateApprovalStatus(
                int requestID,
                int approvalStatusID
        ) {

            /*
             * Not needed by these coverage tests.
             */
        }

        @Override
        public void deleteRequest(
                int requestID
        ) {

            deleteCalls++;
        }

        @Override
        public void updateRequest(
                Leave request
        ) {

            updateCalls++;

            lastUpdated =
                    request;
        }
    }

    // =========================================================
    // RECORDING OVERTIME DAO
    // =========================================================

    private static class RecordingOvertimeDAO
            implements ManageableRequestDAO<Overtime> {

        private int addCalls;
        private int updateCalls;
        private int deleteCalls;
        private int getByIdCalls;

        private Overtime lastAdded;
        private Overtime lastUpdated;
        private Overtime existingRequest;

        @Override
        public Overtime getRequestByID(
                int requestID
        ) {

            getByIdCalls++;

            return existingRequest;
        }

        @Override
        public List<Overtime> getRequestsByEmployeeID(
                int employeeID
        ) {

            if (existingRequest == null) {
                return List.of();
            }

            return List.of(
                    existingRequest
            );
        }

        @Override
        public List<Overtime> getAllRequests() {

            if (existingRequest == null) {
                return List.of();
            }

            return List.of(
                    existingRequest
            );
        }

        @Override
        public void addRequest(
                Overtime request
        ) {

            addCalls++;

            lastAdded =
                    request;
        }

        @Override
        public void updateApprovalStatus(
                int requestID,
                int approvalStatusID
        ) {

            /*
             * Not needed by these coverage tests.
             */
        }

        @Override
        public void deleteRequest(
                int requestID
        ) {

            deleteCalls++;
        }

        @Override
        public void updateRequest(
                Overtime request
        ) {

            updateCalls++;

            lastUpdated =
                    request;
        }
    }
}