package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import service.UserService;
import service.EmployeeService;
import service.AttendanceService;
import service.LeaveService;
import service.OvertimeService;
import service.PayslipService;

import util.AuthorizationException;
import util.AuthorizationService;
import util.Permission;
import util.SessionManager;


import static org.junit.jupiter.api.Assertions.*;

class SecurityBackendRBACTest {

    @AfterEach
    void clearSession() {
        SessionManager.clearSession();
    }

    @Test
    void employeeCannotAccessITUserAccountListing() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90001,
                "Employee"
        );

        UserService userService =
                new UserService();

        assertThrows(
                AuthorizationException.class,
                userService::getAllUsers
        );
    }

    @Test
    void itRoleHasUserAccountAdministrationPermissions() {

        SessionManager.setSession(
                "RBAC_IT_TEST",
                90002,
                "IT"
        );

        assertTrue(
                AuthorizationService.hasPermission(
                        Permission.VIEW_USER_ACCOUNTS
                )
        );

        assertTrue(
                AuthorizationService.hasPermission(
                        Permission.MANAGE_USER_ACCOUNTS
                )
        );
    }

    @Test
    void hrCanManageEmployeesButCannotAdministerITAccounts() {

        SessionManager.setSession(
                "RBAC_HR_TEST",
                90003,
                "HR"
        );

        assertTrue(
                AuthorizationService.hasPermission(
                        Permission.MANAGE_EMPLOYEES
                )
        );

        assertTrue(
                AuthorizationService.hasPermission(
                        Permission.MANAGE_ACCOUNT_STATUS
                )
        );

        assertFalse(
                AuthorizationService.hasPermission(
                        Permission.MANAGE_USER_ACCOUNTS
                )
        );
    }

    @Test
    void financeCanViewPayrollButCannotManageEmployees() {

        SessionManager.setSession(
                "RBAC_FINANCE_TEST",
                90004,
                "Finance"
        );

        assertTrue(
                AuthorizationService.hasPermission(
                        Permission.VIEW_PAYROLL
                )
        );

        assertFalse(
                AuthorizationService.hasPermission(
                        Permission.MANAGE_EMPLOYEES
                )
        );
    }

    @Test
    void supervisorCanApproveRequestsButCannotManageUsers() {

        SessionManager.setSession(
                "RBAC_MANAGER_TEST",
                90005,
                "Immediate Supervisor"
        );

        assertTrue(
                AuthorizationService.hasPermission(
                        Permission.APPROVE_LEAVE
                )
        );

        assertTrue(
                AuthorizationService.hasPermission(
                        Permission.APPROVE_OVERTIME
                )
        );

        assertFalse(
                AuthorizationService.hasPermission(
                        Permission.MANAGE_USER_ACCOUNTS
                )
        );
    }

    @Test
    void employeeCanAccessOwnEmployeeResourceButNotAnotherEmployeesResource() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90006,
                "Employee"
        );

        assertDoesNotThrow(
                () -> AuthorizationService.requireSelfEmployee(
                        90006
                )
        );

        assertThrows(
                AuthorizationException.class,
                () -> AuthorizationService.requireSelfEmployee(
                        99999
                )
        );
    }

    @Test
    void noSessionIsDenied() {

        SessionManager.clearSession();

        assertThrows(
                AuthorizationException.class,
                AuthorizationService::requireAuthenticated
        );
    }
    
    @Test
    void noSessionCannotBypassProtectedUserService() {

        SessionManager.clearSession();

        UserService userService =
                new UserService();

        assertThrows(
                AuthorizationException.class,
                userService::getAllUsers
        );
    }
    
    @Test
    void employeeCannotRetrieveAllEmployeeRecords() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90001,
                "Employee"
        );

        EmployeeService employeeService =
                new EmployeeService();

        assertThrows(
                AuthorizationException.class,
                employeeService::getAllEmployees
        );
    }
    
    @Test
    void employeeCannotRetrieveAllAttendanceRecords() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90001,
                "Employee"
        );

        AttendanceService attendanceService =
                new AttendanceService();

        assertThrows(
                AuthorizationException.class,
                attendanceService::getAllAttendance
        );
    }
    
    @Test
    void employeeCannotApproveLeaveThroughDirectServiceCall() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90001,
                "Employee"
        );

        LeaveService leaveService =
                new LeaveService();

        assertThrows(
                AuthorizationException.class,
                () -> leaveService.updateApprovalStatus(
                        99999,
                        1
                )
        );
    }
    
    @Test
    void employeeCannotApproveOvertimeThroughDirectServiceCall() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90001,
                "Employee"
        );

        OvertimeService overtimeService =
                new OvertimeService();

        assertThrows(
                AuthorizationException.class,
                () -> overtimeService.updateApprovalStatus(
                        99999,
                        1
                )
        );
    }
    
    @Test
    void employeeCannotViewAnotherEmployeesPayslips() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90001,
                "Employee"
        );

        PayslipService payslipService =
                new PayslipService();

        assertThrows(
                AuthorizationException.class,
                () -> payslipService.getPayslipsByEmployeeID(
                        99999
                )
        );
    }
    
    @Test
    void employeeCanAccessOwnUserResourceButNotAnotherUsersResource() {

        SessionManager.setSession(
                "RBAC_EMPLOYEE_TEST",
                90001,
                "Employee"
        );

        assertDoesNotThrow(
                () -> AuthorizationService.requireSelfUser(
                        "RBAC_EMPLOYEE_TEST"
                )
        );

        assertThrows(
                AuthorizationException.class,
                () -> AuthorizationService.requireSelfUser(
                        "ANOTHER_USER"
                )
        );
    }
    
    @Test
    void unrecognizedRoleCannotAccessProtectedOperation() {

        SessionManager.setSession(
                "RBAC_UNKNOWN_TEST",
                90099,
                "Unknown Role"
        );

        assertFalse(
                AuthorizationService.hasPermission(
                        Permission.VIEW_USER_ACCOUNTS
                )
        );

        assertThrows(
                AuthorizationException.class,
                () -> AuthorizationService.requirePermission(
                        Permission.VIEW_USER_ACCOUNTS
                )
        );
    }
}
