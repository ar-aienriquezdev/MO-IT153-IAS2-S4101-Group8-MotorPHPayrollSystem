package service;

import dao.PayslipDAO;
import daoimpl.PayslipDAOImpl;
import pojo.Payslip;
import util.AuthorizationService;
import util.Permission;

import java.sql.SQLException;
import java.util.List;

public class PayslipService {

    private final PayslipDAO payslipDAO;

    public PayslipService() {
        this.payslipDAO = new PayslipDAOImpl();
    }

    public Payslip getPayslipByPayslipNo(String payslipNo) {
        try {
            Payslip payslip =
                    payslipDAO.getPayslipByPayslipNo(payslipNo);

            if (payslip != null) {
                AuthorizationService.requireSelfEmployeeOr(
                        payslip.getEmployeeID(),
                        Permission.VIEW_PAYROLL
                );
            } else {
                AuthorizationService.requireAuthenticated();
            }

            return payslip;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving payslip by payslipNo",
                    e
            );
        }
    }

    public List<Payslip> getPayslipsByEmployeeID(
            int employeeID
    ) {

        AuthorizationService.requireSelfEmployeeOr(
                employeeID,
                Permission.VIEW_PAYROLL
        );

        try {
            return payslipDAO.getPayslipsByEmployeeID(employeeID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving payslips by employeeID",
                    e
            );
        }
    }

    public List<Payslip> getAllPayslips() {

        AuthorizationService.requirePermission(
                Permission.VIEW_PAYROLL
        );

        try {
            return payslipDAO.getAllPayslips();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error retrieving all payslips",
                    e
            );
        }
    }
}
