package service;

import dao.ManageableRequestDAO;
import daoimpl.LeaveDAOImpl;
import pojo.Leave;
import util.AuthorizationService;
import util.Permission;
import util.SessionManager;
import util.RequestValidationUtil;

import java.sql.SQLException;
import java.util.List;

public class LeaveService {

    private static final int PENDING_APPROVAL_STATUS_ID = 3;
    private ManageableRequestDAO<Leave> leaveDAO;

    public LeaveService() {
        try {
            leaveDAO = new LeaveDAOImpl();
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing LeaveDAO", e);
        }
    }

    public Leave getLeaveByID(int leaveID) {
        try {
            Leave leave = leaveDAO.getRequestByID(leaveID);

            if (leave != null) {
                AuthorizationService.requireSelfEmployeeOr(
                        leave.getEmployeeID(),
                        Permission.APPROVE_LEAVE
                );
            } else {
                AuthorizationService.requireAuthenticated();
            }

            return leave;

        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving leave by ID", e);
        }
    }

    public List<Leave> getLeavesByEmployeeID(int employeeID) {

        AuthorizationService.requireSelfEmployeeOr(
                employeeID,
                Permission.APPROVE_LEAVE
        );

        try {
            return leaveDAO.getRequestsByEmployeeID(employeeID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving leaves by employee ID",
                    e
            );
        }
    }

    public List<Leave> getAllLeaves() {

        AuthorizationService.requirePermission(
                Permission.APPROVE_LEAVE
        );

        try {
            return leaveDAO.getAllRequests();
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving all leaves", e);
        }
    }

    public void addLeave(Leave leave) {

        if (leave == null) {
            throw new IllegalArgumentException(
                    "Leave must not be null."
            );
        }

        AuthorizationService.requireSelfEmployee(
                leave.getEmployeeID()
        );

        /*
         * IAS2 Control 3:
         * New employee-submitted leave requests must always begin in the
         * backend-controlled Pending state. A crafted request cannot
         * self-approve or self-reject by supplying another status ID.
         */
        leave.setApprovalStatusID(
                PENDING_APPROVAL_STATUS_ID
        );

        RequestValidationUtil.validateLeaveForCreate(leave);

        try {
            leaveDAO.addRequest(leave);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error adding leave request",
                    e
            );
        }
    }

    /**
     * Approval status changes are supervisor-only backend operations.
     */
    public void updateApprovalStatus(
            int leaveID,
            int approvalStatusID
    ) {

        AuthorizationService.requirePermission(
                Permission.APPROVE_LEAVE
        );

        try {
            leaveDAO.updateApprovalStatus(
                    leaveID,
                    approvalStatusID
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error updating approval status of leave",
                    e
            );
        }
    }

    public void deleteLeave(int leaveID) {
        try {
            Leave existing =
                    leaveDAO.getRequestByID(leaveID);

            if (existing == null) {
                AuthorizationService.requireAuthenticated();
                return;
            }

            AuthorizationService.requireSelfEmployeeOr(
                    existing.getEmployeeID(),
                    Permission.APPROVE_LEAVE
            );

            leaveDAO.deleteRequest(leaveID);

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting leave", e);
        }
    }

    public String getApprovalStatusName(int approvalStatusID) {
        try {
            return ((LeaveDAOImpl) leaveDAO)
                    .getApprovalStatusName(approvalStatusID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving approval status name",
                    e
            );
        }
    }

    public String getLeaveTypeName(int leaveTypeID) {
        try {
            return ((LeaveDAOImpl) leaveDAO)
                    .getLeaveTypeName(leaveTypeID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving leave type name",
                    e
            );
        }
    }

    /**
     * Employees may update only their own leave request. The backend preserves
     * the existing approval status so a user cannot approve their own request
     * by constructing a modified Leave object.
     */
    public void updateLeave(Leave leave) {

        if (leave == null) {
            throw new IllegalArgumentException(
                    "Leave must not be null."
            );
        }

        AuthorizationService.requireSelfEmployee(
                leave.getEmployeeID()
        );

        /*
         * IAS2 Control 3:
         * Validate employee-controlled business data before persistence.
         */
        RequestValidationUtil.validateLeaveForUpdate(leave);

        try {
            Leave existing =
                    leaveDAO.getRequestByID(
                            leave.getLeaveID()
                    );

            // Preserve backend-controlled ownership and approval state.
            leave.setEmployeeID(
                    existing.getEmployeeID()
            );

            leave.setApprovalStatusID(
                    existing.getApprovalStatusID()
            );

            leaveDAO.updateRequest(leave);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error updating leave request",
                    e
            );
        }
    }
}
