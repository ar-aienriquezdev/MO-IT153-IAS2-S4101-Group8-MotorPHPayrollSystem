package util;

import pojo.Leave;
import pojo.Overtime;

/**
 * Centralized backend validation for security-relevant business requests.
 *
 * These checks are intentionally enforced outside the presentation layer so
 * direct service calls cannot bypass validation performed by Swing forms.
 *
 * IAS2 Control 3:
 * Service-Layer Business Validation and SQL-Safety Regression Protection
 */
public final class RequestValidationUtil {

    private RequestValidationUtil() {
        // Utility class
    }

    // =========================================================
    // LEAVE VALIDATION
    // =========================================================

    /**
     * Validate a new leave request before it reaches the DAO.
     */
    public static void validateLeaveForCreate(Leave leave) {

        validateLeaveCommon(leave);

        if (leave.getApprovalStatusID() <= 0) {
            throw new IllegalArgumentException(
                    "Leave approval status must be valid."
            );
        }
    }

    /**
     * Validate an existing leave request before an update reaches the DAO.
     *
     * Approval status is deliberately not trusted from the incoming object.
     * LeaveService preserves the backend-controlled existing status.
     */
    public static void validateLeaveForUpdate(Leave leave) {

        validateLeaveCommon(leave);

        if (leave.getLeaveID() <= 0) {
            throw new IllegalArgumentException(
                    "Leave ID must be valid for an update."
            );
        }
    }

    private static void validateLeaveCommon(Leave leave) {

        if (leave == null) {
            throw new IllegalArgumentException(
                    "Leave must not be null."
            );
        }

        if (leave.getEmployeeID() <= 0) {
            throw new IllegalArgumentException(
                    "Leave employee ID must be valid."
            );
        }

        if (leave.getLeaveStart() == null) {
            throw new IllegalArgumentException(
                    "Leave start date is required."
            );
        }

        if (leave.getLeaveEnd() == null) {
            throw new IllegalArgumentException(
                    "Leave end date is required."
            );
        }

        if (leave.getLeaveEnd().before(leave.getLeaveStart())) {
            throw new IllegalArgumentException(
                    "Leave end date cannot be earlier than the start date."
            );
        }

        if (leave.getLeaveReason() == null
                || leave.getLeaveReason().isBlank()) {

            throw new IllegalArgumentException(
                    "Leave reason is required."
            );
        }

        if (!Double.isFinite(leave.getLeaveAllowance())
                || leave.getLeaveAllowance() < 0) {

            throw new IllegalArgumentException(
                    "Leave allowance cannot be negative or non-finite."
            );
        }

        if (leave.getLeaveTypeID() <= 0) {
            throw new IllegalArgumentException(
                    "Leave type must be valid."
            );
        }
    }

    // =========================================================
    // OVERTIME VALIDATION
    // =========================================================

    /**
     * Validate a new overtime request before it reaches the DAO.
     */
    public static void validateOvertimeForCreate(Overtime overtime) {

        validateOvertimeCommon(overtime);

        if (overtime.getApprovalStatusID() <= 0) {
            throw new IllegalArgumentException(
                    "Overtime approval status must be valid."
            );
        }
    }

    /**
     * Validate an existing overtime request before an update reaches the DAO.
     *
     * Approval status is preserved by OvertimeService and is therefore not
     * trusted from the incoming update object.
     */
    public static void validateOvertimeForUpdate(Overtime overtime) {

        validateOvertimeCommon(overtime);

        if (overtime.getOvertimeID() <= 0) {
            throw new IllegalArgumentException(
                    "Overtime ID must be valid for an update."
            );
        }
    }

    private static void validateOvertimeCommon(Overtime overtime) {

        if (overtime == null) {
            throw new IllegalArgumentException(
                    "Overtime must not be null."
            );
        }

        if (overtime.getEmployeeID() <= 0) {
            throw new IllegalArgumentException(
                    "Overtime employee ID must be valid."
            );
        }

        if (overtime.getOvertimeStart() == null) {
            throw new IllegalArgumentException(
                    "Overtime start time is required."
            );
        }

        if (overtime.getOvertimeEnd() == null) {
            throw new IllegalArgumentException(
                    "Overtime end time is required."
            );
        }

        if (!overtime.getOvertimeEnd().after(
                overtime.getOvertimeStart())) {

            throw new IllegalArgumentException(
                    "Overtime end time must be later than the start time."
            );
        }

        if (overtime.getOvertimeReason() == null
                || overtime.getOvertimeReason().isBlank()) {

            throw new IllegalArgumentException(
                    "Overtime reason is required."
            );
        }
    }
}