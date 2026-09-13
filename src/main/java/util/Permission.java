package util;

/**
 * Backend permissions used by the MotorPH role-based access control layer.
 *
 * UI visibility is not treated as a security boundary. Sensitive service
 * operations must require one of these permissions or verify resource
 * ownership before accessing or modifying data.
 */
public enum Permission {
    VIEW_ALL_EMPLOYEES,
    MANAGE_EMPLOYEES,
    VIEW_USER_ACCOUNTS,
    MANAGE_USER_ACCOUNTS,
    MANAGE_ACCOUNT_STATUS,
    VIEW_ALL_ATTENDANCE,
    MANAGE_ATTENDANCE,
    VIEW_PAYROLL,
    APPROVE_LEAVE,
    APPROVE_OVERTIME
}
