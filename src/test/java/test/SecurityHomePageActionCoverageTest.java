package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.AttendanceService;
import service.LeaveService;
import ui.base.AbstractHomePage;
import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityHomePageActionCoverageTest {

    private static final int EMPLOYEE_ID = 93001;

    private HomeHarness page;
    private FakeAttendanceService attendance;
    private FakeLeaveService leaves;

    @BeforeEach
    void setUp() {

        SessionManager.clearSession();

        SessionManager.setSession(
                "C6_HOME_ACTION_USER",
                EMPLOYEE_ID,
                "Employee"
        );

        attendance = new FakeAttendanceService();
        leaves = new FakeLeaveService();

        page = new HomeHarness();

        page.setEmployeeIDForTest(
                EMPLOYEE_ID
        );

        page.setAttendanceServiceForTest(
                attendance
        );

        page.setLeaveServiceForTest(
                leaves
        );
    }

    @AfterEach
    void tearDown() {

        closeOpenDialogs();

        if (page != null) {
            page.dispose();
        }

        SessionManager.clearSession();
    }

    @Test
    void clockInStopsImmediatelyWhenAlreadyClockedIn() {

        page.isClockedInToday = true;

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockIn
        );

        assertEquals(
                0,
                attendance.clockInCalls
        );
    }

    @Test
    void successfulClockInExecutesSuccessAndRefreshPath() {

        page.isClockedInToday = false;

        attendance.clockInResult = true;

        attendance.status.setClockedIn(
                true
        );

        attendance.status.setClockedOut(
                false
        );

        attendance.status.setLogIn(
                "08:00:00"
        );

        attendance.status.setLogOut(
                "Not Clocked-Out"
        );

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockIn
        );

        assertEquals(
                1,
                attendance.clockInCalls
        );

        assertTrue(
                page.isClockedInToday
        );

        assertTrue(
                page.clockOutButton.isEnabled()
        );
    }

    @Test
    void failedClockInExecutesFailurePath() {

        page.isClockedInToday = false;

        attendance.clockInResult = false;

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockIn
        );

        assertEquals(
                1,
                attendance.clockInCalls
        );
    }

    @Test
    void clockInExceptionIsHandledWithoutPropagation() {

        page.isClockedInToday = false;

        attendance.clockInFailure =
                new SQLException(
                        "Synthetic clock-in failure"
                );

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockIn
        );

        assertEquals(
                1,
                attendance.clockInCalls
        );
    }

    @Test
    void clockOutStopsWhenEmployeeHasNotClockedIn() {

        page.isClockedInToday = false;
        page.isClockedOutToday = false;

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockOut
        );

        assertEquals(
                0,
                attendance.clockOutCalls
        );
    }

    @Test
    void clockOutStopsWhenAlreadyClockedOut() {

        page.isClockedInToday = true;
        page.isClockedOutToday = true;

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockOut
        );

        assertEquals(
                0,
                attendance.clockOutCalls
        );
    }

    @Test
    void successfulClockOutExecutesSuccessAndRefreshPath() {

        page.isClockedInToday = true;
        page.isClockedOutToday = false;

        attendance.clockOutResult = true;

        attendance.status.setClockedIn(
                true
        );

        attendance.status.setClockedOut(
                true
        );

        attendance.status.setLogIn(
                "08:00:00"
        );

        attendance.status.setLogOut(
                "17:00:00"
        );

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockOut
        );

        assertEquals(
                1,
                attendance.clockOutCalls
        );

        assertTrue(
                page.isClockedOutToday
        );

        assertFalse(
                page.clockOutButton.isEnabled()
        );
    }

    @Test
    void failedClockOutExecutesFailurePath() {

        page.isClockedInToday = true;
        page.isClockedOutToday = false;

        attendance.clockOutResult = false;

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockOut
        );

        assertEquals(
                1,
                attendance.clockOutCalls
        );
    }

    @Test
    void clockOutExceptionIsHandledWithoutPropagation() {

        page.isClockedInToday = true;
        page.isClockedOutToday = false;

        attendance.clockOutFailure =
                new SQLException(
                        "Synthetic clock-out failure"
                );

        closeNextDialogSoon();

        assertDoesNotThrow(
                page::performClockOut
        );

        assertEquals(
                1,
                attendance.clockOutCalls
        );
    }

    @Test
    void refreshHandlesLeaveServiceFailureByDisplayingZero() {

        attendance.status.setClockedIn(
                false
        );

        attendance.status.setClockedOut(
                false
        );

        attendance.status.setLogIn(
                "Not Clocked-In"
        );

        attendance.status.setLogOut(
                "Not Clocked-Out"
        );

        leaves.throwFailure = true;

        assertDoesNotThrow(
                page::refreshClockInOutStatus
        );

        assertEquals(
                "0",
                page.leavesAvailableText.getText()
        );
    }

    private static void closeNextDialogSoon() {

        Thread closer =
                new Thread(
                        () -> {

                            try {
                                Thread.sleep(
                                        200
                                );
                            } catch (
                                    InterruptedException e
                            ) {
                                Thread.currentThread()
                                        .interrupt();
                            }

                            SwingUtilities.invokeLater(
                                    SecurityHomePageActionCoverageTest
                                            ::closeOpenDialogs
                            );
                        }
                );

        closer.setDaemon(
                true
        );

        closer.start();
    }

    private static void closeOpenDialogs() {

        for (Window window : Window.getWindows()) {

            if (window instanceof JDialog dialog
                    && dialog.isShowing()) {

                dialog.dispose();
            }
        }
    }

    private static class FakeAttendanceService
            extends AttendanceService {

        private boolean clockInResult = true;
        private boolean clockOutResult = true;

        private SQLException clockInFailure;
        private SQLException clockOutFailure;

        private int clockInCalls;
        private int clockOutCalls;

        private AttendanceStatus status =
                new AttendanceStatus();

        private BigDecimal monthlyHours =
                BigDecimal.ZERO;

        @Override
        public boolean clockIn(
                int employeeID
        ) throws SQLException {

            clockInCalls++;

            if (clockInFailure != null) {
                throw clockInFailure;
            }

            return clockInResult;
        }

        @Override
        public boolean clockOut(
                int employeeID
        ) throws SQLException {

            clockOutCalls++;

            if (clockOutFailure != null) {
                throw clockOutFailure;
            }

            return clockOutResult;
        }

        @Override
        public AttendanceStatus
                getTodayAttendanceStatus(
                        int employeeID
                ) {

            return status;
        }

        @Override
        public BigDecimal
                getMonthlyWorkedHours(
                        int employeeID,
                        int year,
                        int month
                ) {

            return monthlyHours;
        }
    }

    private static class FakeLeaveService
            extends LeaveService {

        private boolean throwFailure;

        @Override
        public List<pojo.Leave>
                getLeavesByEmployeeID(
                        int employeeID
                ) {

            if (throwFailure) {

                throw new RuntimeException(
                        "Synthetic leave lookup failure"
                );
            }

            return new ArrayList<>();
        }
    }

    private static class HomeHarness
            extends AbstractHomePage {

        private final JLabel fullNameText =
                new JLabel();

        private final JLabel positionText =
                new JLabel();

        private final JLabel dateTimeText =
                new JLabel();

        private final JLabel clockInText =
                new JLabel();

        private final JLabel clockOutText =
                new JLabel();

        private final JButton clockInButton =
                new JButton();

        private final JButton clockOutButton =
                new JButton();

        private final JLabel totalWorkedHoursText =
                new JLabel();

        private final JLabel leavesAvailableText =
                new JLabel();

        void setEmployeeIDForTest(
                int employeeID
        ) {

            this.employeeID =
                    employeeID;
        }

        void setAttendanceServiceForTest(
                AttendanceService service
        ) {

            this.attendanceService =
                    service;
        }

        void setLeaveServiceForTest(
                LeaveService service
        ) {

            this.leaveService =
                    service;
        }

        @Override
        protected void onProfileLabelClick() {
        }

        @Override
        protected JLabel getFullNameText() {
            return fullNameText;
        }

        @Override
        protected JLabel getPositionText() {
            return positionText;
        }

        @Override
        protected JLabel getDateTimeText() {
            return dateTimeText;
        }

        @Override
        protected JLabel getClockInText() {
            return clockInText;
        }

        @Override
        protected JLabel getClockOutText() {
            return clockOutText;
        }

        @Override
        protected JButton getClockInButton() {
            return clockInButton;
        }

        @Override
        protected JButton getClockOutButton() {
            return clockOutButton;
        }

        @Override
        protected JLabel getTotalWorkedHoursText() {
            return totalWorkedHoursText;
        }

        @Override
        protected JLabel getLeavesAvailableText() {
            return leavesAvailableText;
        }
    }
}