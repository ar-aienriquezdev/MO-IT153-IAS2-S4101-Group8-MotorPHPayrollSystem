package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import service.AttendanceService;
import service.EmployeeService;
import util.ErrorLogger;
import util.SessionManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Control 6 targeted coverage expansion for small security-relevant utility
 * and service branches that do not require Swing interaction.
 */
public class SecurityCoverageExpansionTest {

    @AfterEach
    void tearDown() {

        SessionManager.clearSession();
    }

    @Test
    void attendanceStatusDefaultsAndAccessorsBehavePredictably() {

        AttendanceService.AttendanceStatus status =
                new AttendanceService.AttendanceStatus();

        assertFalse(
                status.isClockedIn()
        );

        assertFalse(
                status.isClockedOut()
        );

        assertNull(
                status.getLogIn()
        );

        assertNull(
                status.getLogOut()
        );

        assertEquals(
                BigDecimal.ZERO,
                status.getWorkedHours()
        );

        status.setClockedIn(true);
        status.setClockedOut(true);

        status.setLogIn(
                "08:00:00"
        );

        status.setLogOut(
                "17:00:00"
        );

        status.setWorkedHours(
                new BigDecimal(
                        "8.50"
                )
        );

        assertTrue(
                status.isClockedIn()
        );

        assertTrue(
                status.isClockedOut()
        );

        assertEquals(
                "08:00:00",
                status.getLogIn()
        );

        assertEquals(
                "17:00:00",
                status.getLogOut()
        );

        assertEquals(
                new BigDecimal(
                        "8.50"
                ),
                status.getWorkedHours()
        );

        /*
         * AttendanceStatus deliberately normalizes a null DB value
         * to BigDecimal.ZERO.
         */
        status.setWorkedHours(null);

        assertEquals(
                BigDecimal.ZERO,
                status.getWorkedHours()
        );
    }

    @Test
    void employeeServiceRejectsNullUpdateBeforeAuthorizationOrDAOAccess() {

        EmployeeService service =
                new EmployeeService();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service
                                .updateEmployee(null)
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("employee")
        );
    }

    @Test
    void centralizedErrorLoggerAcceptsNullWithoutFailure() {

        assertDoesNotThrow(
                () -> ErrorLogger.log(null)
        );
    }

    @Test
    void centralizedErrorLoggerRecordsThrowableWithoutPropagating() {

        IllegalStateException testException =
                new IllegalStateException(
                        "Control 6 coverage-test exception"
                );

        assertDoesNotThrow(
                () -> ErrorLogger.log(
                        testException
                )
        );
    }
}