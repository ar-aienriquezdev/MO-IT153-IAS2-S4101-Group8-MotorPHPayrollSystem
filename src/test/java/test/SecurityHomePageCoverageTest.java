package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pojo.Leave;
import service.AttendanceService;
import service.LeaveService;
import ui.base.AbstractHomePage;
import util.SessionManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityHomePageCoverageTest {

    private static final int EMPLOYEE_ID =
            92001;

    private HomeHarness page;
    private FakeAttendanceService attendance;
    private FakeLeaveService leaves;

    @BeforeEach
    void setUp() {

        SessionManager.clearSession();

        SessionManager.setSession(
                "C6_HOME_USER",
                EMPLOYEE_ID,
                "Employee"
        );

        attendance =
                new FakeAttendanceService();

        leaves =
                new FakeLeaveService();

        page =
                new HomeHarness();

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

        if (page != null) {
            page.dispose();
        }

        SessionManager.clearSession();
    }

    @Test
    void parseTimeOnlyUsesTodayDate() throws Exception {

        LocalDateTime parsed =
                (LocalDateTime) invokePrivate(
                        page,
                        "parsePossiblyTimeOnly",
                        new Class<?>[]{
                                String.class
                        },
                        "08:15:30"
                );

        assertEquals(
                LocalDate.now(),
                parsed.toLocalDate()
        );

        assertEquals(
                LocalTime.of(
                        8,
                        15,
                        30
                ),
                parsed.toLocalTime()
        );
    }

    @Test
    void parseFullTimestampPreservesDateAndTime() throws Exception {

        LocalDateTime parsed =
                (LocalDateTime) invokePrivate(
                        page,
                        "parsePossiblyTimeOnly",
                        new Class<?>[]{
                                String.class
                        },
                        "2026-10-18 09:45:30"
                );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        9,
                        45,
                        30
                ),
                parsed
        );
    }

    @Test
    void malformedTimestampFallsBackToCurrentTime() throws Exception {

        LocalDateTime before =
                LocalDateTime.now()
                        .minusSeconds(
                                1
                        );

        LocalDateTime parsed =
                (LocalDateTime) invokePrivate(
                        page,
                        "parsePossiblyTimeOnly",
                        new Class<?>[]{
                                String.class
                        },
                        "not-a-time"
                );

        LocalDateTime after =
                LocalDateTime.now()
                        .plusSeconds(
                                1
                        );

        assertFalse(
                parsed.isBefore(
                        before
                )
        );

        assertFalse(
                parsed.isAfter(
                        after
                )
        );
    }

    @Test
    void monthlyHoursDisplayWorksWithoutDailyClockIn() throws Exception {

        attendance.monthlyHours =
                new BigDecimal(
                        "10.50"
                );

        setPrivateField(
                page,
                "clockInDateTime",
                null
        );

        setPrivateField(
                page,
                "clockOutDateTime",
                null
        );

        invokePrivate(
                page,
                "updateWorkedHoursDisplay",
                new Class<?>[]{
                        LocalDateTime.class
                },
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        12,
                        0
                )
        );

        assertEquals(
                "10 hrs, 30 min",
                page.totalWorkedHoursText
                        .getText()
        );
    }

    @Test
    void workedHoursDisplayShowsOngoingDailyDuration() throws Exception {

        attendance.monthlyHours =
                new BigDecimal(
                        "20.00"
                );

        LocalDateTime now =
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        12,
                        0,
                        0
                );

        setPrivateField(
                page,
                "clockInDateTime",
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        10,
                        15,
                        0
                )
        );

        setPrivateField(
                page,
                "clockOutDateTime",
                null
        );

        invokePrivate(
                page,
                "updateWorkedHoursDisplay",
                new Class<?>[]{
                        LocalDateTime.class
                },
                now
        );

        String text =
                page.totalWorkedHoursText
                        .getText();

        assertTrue(
                text.contains(
                        "20 hrs, 0 min"
                )
        );

        assertTrue(
                text.contains(
                        "today: 1 hrs, 45 min, 0 sec"
                )
        );
    }

    @Test
    void workedHoursDisplayUsesClockOutWhenPresent() throws Exception {

        attendance.monthlyHours =
                new BigDecimal(
                        "40.00"
                );

        setPrivateField(
                page,
                "clockInDateTime",
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        8,
                        0,
                        0
                )
        );

        setPrivateField(
                page,
                "clockOutDateTime",
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        17,
                        30,
                        0
                )
        );

        invokePrivate(
                page,
                "updateWorkedHoursDisplay",
                new Class<?>[]{
                        LocalDateTime.class
                },
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        18,
                        0,
                        0
                )
        );

        String text =
                page.totalWorkedHoursText
                        .getText();

        assertTrue(
                text.contains(
                        "today: 9 hrs, 30 min, 0 sec"
                )
        );
    }

    @Test
    void workedHoursDisplayDoesNotShowPreviousDayAsToday() throws Exception {

        attendance.monthlyHours =
                new BigDecimal(
                        "5.00"
                );

        setPrivateField(
                page,
                "clockInDateTime",
                LocalDateTime.of(
                        2026,
                        10,
                        17,
                        8,
                        0,
                        0
                )
        );

        setPrivateField(
                page,
                "clockOutDateTime",
                LocalDateTime.of(
                        2026,
                        10,
                        17,
                        17,
                        0,
                        0
                )
        );

        invokePrivate(
                page,
                "updateWorkedHoursDisplay",
                new Class<?>[]{
                        LocalDateTime.class
                },
                LocalDateTime.of(
                        2026,
                        10,
                        18,
                        12,
                        0,
                        0
                )
        );

        assertEquals(
                "5 hrs, 0 min",
                page.totalWorkedHoursText
                        .getText()
        );
    }

    @Test
    void clockInButtonIsDisabledWhenAlreadyClockedIn() throws Exception {

        page.isClockedInToday =
                true;

        page.clockInButton
                .setEnabled(
                        true
                );

        invokePrivate(
                page,
                "updateClockInButtonAvailability",
                new Class<?>[]{}
        );

        assertFalse(
                page.clockInButton
                        .isEnabled()
        );
    }

    @Test
    void availabilityCalculationExecutesNormalTimeBranch() throws Exception {

        page.isClockedInToday =
                false;

        invokePrivate(
                page,
                "updateClockInButtonAvailability",
                new Class<?>[]{}
        );

        /*
         * Exact enabled state depends on the real day/time,
         * but the normal availability branch must execute
         * without throwing.
         */
        assertNotNull(
                page.clockInButton
        );
    }

    @Test
    void refreshStatusCoversClockedInAndClockedOutBranches() {

        AttendanceService.AttendanceStatus status =
                new AttendanceService.AttendanceStatus();

        status.setClockedIn(
                true
        );

        status.setClockedOut(
                true
        );

        status.setLogIn(
                "08:00:00"
        );

        status.setLogOut(
                "17:30:00"
        );

        status.setWorkedHours(
                new BigDecimal(
                        "9.50"
                )
        );

        attendance.status =
                status;

        attendance.monthlyHours =
                new BigDecimal(
                        "45.00"
                );

        leaves.leaves =
                List.of(
                        leave(
                                "2026-10-10",
                                3.0
                        ),
                        leave(
                                "2026-10-18",
                                7.0
                        )
                );

        assertDoesNotThrow(
                page::refreshClockInOutStatus
        );

        assertTrue(
                page.isClockedInToday
        );

        assertTrue(
                page.isClockedOutToday
        );

        assertEquals(
                "08:00:00",
                page.clockInText
                        .getText()
        );

        assertEquals(
                "17:30:00",
                page.clockOutText
                        .getText()
        );

        assertEquals(
                "7",
                page.leavesAvailableText
                        .getText()
        );
    }

    @Test
    void refreshStatusCoversNotClockedInAndEmptyLeaveBranches() {

        AttendanceService.AttendanceStatus status =
                new AttendanceService.AttendanceStatus();

        status.setClockedIn(
                false
        );

        status.setClockedOut(
                false
        );

        status.setLogIn(
                "Not Clocked-In"
        );

        status.setLogOut(
                "Not Clocked-Out"
        );

        attendance.status =
                status;

        attendance.monthlyHours =
                BigDecimal.ZERO;

        leaves.leaves =
                List.of();

        assertDoesNotThrow(
                page::refreshClockInOutStatus
        );

        assertFalse(
                page.isClockedInToday
        );

        assertFalse(
                page.isClockedOutToday
        );

        assertEquals(
                "Not Clocked-In",
                page.clockInText
                        .getText()
        );

        assertEquals(
                "Not Clocked-Out",
                page.clockOutText
                        .getText()
        );

        assertEquals(
                "0",
                page.leavesAvailableText
                        .getText()
        );
    }

    @Test
    void disposeStopsAndClearsClockTimer() throws Exception {

        Timer timer =
                new Timer(
                        1000,
                        e -> {
                        }
                );

        timer.start();

        setPrivateField(
                page,
                "clockTimer",
                timer
        );

        assertTrue(
                timer.isRunning()
        );

        page.dispose();

        assertFalse(
                timer.isRunning()
        );

        assertNull(
                getPrivateField(
                        page,
                        "clockTimer"
                )
        );
    }

    @Test
    void currentTimeHelperProducesExpectedFormat() throws Exception {

        String text =
                (String) invokePrivate(
                        page,
                        "getCurrentTime",
                        new Class<?>[]{}
                );

        assertDoesNotThrow(
                () -> LocalDateTime.parse(
                        text,
                        java.time.format.DateTimeFormatter
                                .ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"
                                )
                )
        );
    }

    private static Leave leave(
            String dateCreated,
            double allowance
    ) {

        Leave leave =
                new Leave();

        leave.setDateCreated(
                Date.valueOf(
                        dateCreated
                )
        );

        leave.setLeaveAllowance(
                allowance
        );

        leave.setEmployeeID(
                EMPLOYEE_ID
        );

        return leave;
    }

    private static Object invokePrivate(
            Object target,
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {

        Method method =
                AbstractHomePage.class
                        .getDeclaredMethod(
                                methodName,
                                parameterTypes
                        );

        method.setAccessible(
                true
        );

        try {

            return method.invoke(
                    target,
                    arguments
            );

        } catch (
                java.lang.reflect.InvocationTargetException e
        ) {

            Throwable cause =
                    e.getCause();

            if (cause
                    instanceof Exception exception) {

                throw exception;
            }

            throw new RuntimeException(
                    cause
            );
        }
    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        Field field =
                AbstractHomePage.class
                        .getDeclaredField(
                                fieldName
                        );

        field.setAccessible(
                true
        );

        field.set(
                target,
                value
        );
    }

    private static Object getPrivateField(
            Object target,
            String fieldName
    ) throws Exception {

        Field field =
                AbstractHomePage.class
                        .getDeclaredField(
                                fieldName
                        );

        field.setAccessible(
                true
        );

        return field.get(
                target
        );
    }

    private static class FakeAttendanceService
            extends AttendanceService {

        private AttendanceStatus status =
                new AttendanceStatus();

        private BigDecimal monthlyHours =
                BigDecimal.ZERO;

        @Override
        public AttendanceStatus getTodayAttendanceStatus(
                int employeeID
        ) {

            return status;
        }

        @Override
        public BigDecimal getMonthlyWorkedHours(
                int employeeID,
                int year,
                int month
        ) {

            return monthlyHours;
        }
    }

    private static class FakeLeaveService
            extends LeaveService {

        private List<Leave> leaves =
                List.of();

        @Override
        public List<Leave> getLeavesByEmployeeID(
                int employeeID
        ) {

            /*
             * Return a mutable copy because AbstractHomePage sorts
             * the retrieved leave records by date before displaying
             * the latest available allowance.
             */
            return new ArrayList<>(
                    leaves
            );
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
            // No navigation in coverage test.
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