package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pojo.Leave;
import pojo.Overtime;
import util.RequestValidationUtil;
import util.SessionManager;

import java.sql.Date;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityFinalCoverageThresholdTest {

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    void blankUserIDDoesNotCountAsActiveSession() {

        SessionManager.setSession(
                "   ",
                97001,
                "Employee"
        );

        assertFalse(
                SessionManager.hasActiveSession()
        );
    }

    @Test
    void nonPositiveEmployeeIDDoesNotCountAsActiveSession() {

        SessionManager.setSession(
                "C6_FINAL_SESSION",
                0,
                "Employee"
        );

        assertFalse(
                SessionManager.hasActiveSession()
        );
    }

    @Test
    void leaveValidationRejectsNullReasonAndNonFiniteAllowance() {

        Leave nullReason =
                validLeave();

        nullReason.setLeaveReason(
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(
                                nullReason
                        )
        );

        Leave nonFiniteAllowance =
                validLeave();

        nonFiniteAllowance.setLeaveAllowance(
                Double.NaN
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateLeaveForCreate(
                                nonFiniteAllowance
                        )
        );
    }

    @Test
    void overtimeValidationRejectsNullReason() {

        Overtime overtime =
                validOvertime();

        overtime.setOvertimeReason(
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RequestValidationUtil
                        .validateOvertimeForCreate(
                                overtime
                        )
        );
    }

    private static Leave validLeave() {

        Leave leave =
                new Leave();

        leave.setLeaveAllowance(
                1.0
        );

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
                "Control 6 valid leave"
        );

        leave.setDateCreated(
                Date.valueOf(
                        "2026-10-18"
                )
        );

        leave.setEmployeeID(
                97001
        );

        leave.setApprovalStatusID(
                3
        );

        leave.setLeaveTypeID(
                1
        );

        return leave;
    }

    private static Overtime validOvertime() {

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
                "Control 6 valid overtime"
        );

        overtime.setApprovalStatusID(
                3
        );

        overtime.setEmployeeID(
                97001
        );

        return overtime;
    }
}