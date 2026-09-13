package service;

import dao.ManageableRequestDAO;
import daoimpl.OvertimeDAOImpl;
import pojo.Overtime;
import util.AuthorizationService;
import util.Permission;
import util.SessionManager;

import java.sql.SQLException;
import java.util.List;

public class OvertimeService {

    private ManageableRequestDAO<Overtime> overtimeDAO;

    public OvertimeService() {
        try {
            overtimeDAO = new OvertimeDAOImpl();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error initializing OvertimeDAO",
                    e
            );
        }
    }

    public Overtime getOvertimeByID(int overtimeID) {
        try {
            Overtime overtime =
                    overtimeDAO.getRequestByID(overtimeID);

            if (overtime != null) {
                AuthorizationService.requireSelfEmployeeOr(
                        overtime.getEmployeeID(),
                        Permission.APPROVE_OVERTIME
                );
            } else {
                AuthorizationService.requireAuthenticated();
            }

            return overtime;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving overtime by ID",
                    e
            );
        }
    }

    public List<Overtime> getOvertimesByEmployeeID(
            int employeeID
    ) {

        AuthorizationService.requireSelfEmployeeOr(
                employeeID,
                Permission.APPROVE_OVERTIME
        );

        try {
            return overtimeDAO.getRequestsByEmployeeID(employeeID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving overtimes by employee ID",
                    e
            );
        }
    }

    public List<Overtime> getAllOvertimes() {

        AuthorizationService.requirePermission(
                Permission.APPROVE_OVERTIME
        );

        try {
            return overtimeDAO.getAllRequests();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving all overtimes",
                    e
            );
        }
    }

    public void addOvertime(Overtime overtime) {

        if (overtime == null) {
            throw new IllegalArgumentException(
                    "Overtime must not be null."
            );
        }

        AuthorizationService.requireSelfEmployee(
                overtime.getEmployeeID()
        );

        try {
            overtimeDAO.addRequest(overtime);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error adding overtime request",
                    e
            );
        }
    }

    /** Supervisor-only approval/rejection operation. */
    public void updateApprovalStatus(
            int overtimeID,
            int approvalStatusID
    ) {

        AuthorizationService.requirePermission(
                Permission.APPROVE_OVERTIME
        );

        try {
            overtimeDAO.updateApprovalStatus(
                    overtimeID,
                    approvalStatusID
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error updating approval status of overtime",
                    e
            );
        }
    }

    public void deleteOvertime(int overtimeID) {
        try {
            Overtime existing =
                    overtimeDAO.getRequestByID(overtimeID);

            if (existing == null) {
                AuthorizationService.requireAuthenticated();
                return;
            }

            AuthorizationService.requireSelfEmployeeOr(
                    existing.getEmployeeID(),
                    Permission.APPROVE_OVERTIME
            );

            overtimeDAO.deleteRequest(overtimeID);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error deleting overtime",
                    e
            );
        }
    }

    /**
     * Employees may update only their own request. The backend preserves the
     * original approval status and owner so a crafted object cannot be used to
     * self-approve or transfer an overtime request.
     */
    public void updateOvertime(Overtime overtime) {

        if (overtime == null) {
            throw new IllegalArgumentException(
                    "Overtime must not be null."
            );
        }

        AuthorizationService.requireSelfEmployee(
                overtime.getEmployeeID()
        );

        try {
            Overtime existing =
                    overtimeDAO.getRequestByID(
                            overtime.getOvertimeID()
                    );

            if (existing == null) {
                throw new IllegalArgumentException(
                        "Overtime request not found: "
                                + overtime.getOvertimeID()
                );
            }

            if (existing.getEmployeeID()
                    != SessionManager.getEmployeeID()) {

                throw new SecurityException(
                        "Cannot modify another employee's overtime request."
                );
            }

            overtime.setEmployeeID(
                    existing.getEmployeeID()
            );

            overtime.setApprovalStatusID(
                    existing.getApprovalStatusID()
            );

            overtimeDAO.updateRequest(overtime);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error updating overtime request",
                    e
            );
        }
    }
}
