package test;

import dao.AttendanceDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pojo.Attendance;
import service.AttendanceService;
import util.SessionManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAttendanceServiceCoverageTest {

    private AttendanceService attendanceService;
    private RecordingAttendanceDAO handler;

    @BeforeEach
    void setUp() throws Exception {

        SessionManager.clearSession();

        /*
         * HR legitimately has VIEW_ALL_ATTENDANCE and
         * MANAGE_ATTENDANCE permissions.
         */
        SessionManager.setSession(
                "C6_ATTENDANCE_HR",
                96001,
                "HR"
        );

        attendanceService =
                new AttendanceService();

        handler =
                new RecordingAttendanceDAO();

        AttendanceDAO proxy =
                (AttendanceDAO) Proxy.newProxyInstance(
                        AttendanceDAO.class.getClassLoader(),
                        new Class<?>[]{
                                AttendanceDAO.class
                        },
                        handler
                );

        Field field =
                AttendanceService.class
                        .getDeclaredField(
                                "attendanceDAO"
                        );

        field.setAccessible(
                true
        );

        field.set(
                attendanceService,
                proxy
        );
    }

    @AfterEach
    void tearDown() {

        SessionManager.clearSession();
    }

    @Test
    void authorizedAttendanceReadAndWritePathsReachDAO() {

        /*
         * Missing ID still requires authentication but should
         * safely return null.
         */
        assertNull(
                attendanceService
                        .getAttendanceByID(
                                999999
                        )
        );

        List<Attendance> employeeRecords =
                attendanceService
                        .getAttendanceByEmployeeID(
                                96002
                        );

        assertNotNull(
                employeeRecords
        );

        assertTrue(
                employeeRecords.isEmpty()
        );

        Date date =
                Date.valueOf(
                        "2026-10-18"
                );

        List<Attendance> dateRecords =
                attendanceService
                        .getAttendanceByDate(
                                date
                        );

        assertNotNull(
                dateRecords
        );

        assertTrue(
                dateRecords.isEmpty()
        );

        List<Attendance> allRecords =
                attendanceService
                        .getAllAttendance();

        assertNotNull(
                allRecords
        );

        assertTrue(
                allRecords.isEmpty()
        );

        Attendance attendance =
                new Attendance();

        assertDoesNotThrow(
                () -> attendanceService
                        .addAttendance(
                                attendance
                        )
        );

        assertDoesNotThrow(
                () -> attendanceService
                        .updateAttendance(
                                attendance
                        )
        );

        assertDoesNotThrow(
                () -> attendanceService
                        .deleteAttendance(
                                12345
                        )
        );

        assertEquals(
                1,
                handler.getByIDCalls
        );

        assertEquals(
                1,
                handler.getByEmployeeCalls
        );

        assertEquals(
                1,
                handler.getByDateCalls
        );

        assertEquals(
                1,
                handler.getAllCalls
        );

        assertEquals(
                1,
                handler.addCalls
        );

        assertEquals(
                1,
                handler.updateCalls
        );

        assertEquals(
                1,
                handler.deleteCalls
        );
    }

    @Test
    void attendanceDAOFailuresAreWrappedByServiceLayer() {

        handler.failMethod =
                "getAllAttendance";

        RuntimeException readException =
                assertThrows(
                        RuntimeException.class,
                        attendanceService
                                ::getAllAttendance
                );

        assertInstanceOf(
                SQLException.class,
                readException.getCause()
        );

        assertTrue(
                readException.getMessage()
                        .contains(
                                "Error retrieving all attendance records"
                        )
        );

        handler.failMethod =
                "addAttendance";

        RuntimeException writeException =
                assertThrows(
                        RuntimeException.class,
                        () -> attendanceService
                                .addAttendance(
                                        new Attendance()
                                )
                );

        assertInstanceOf(
                SQLException.class,
                writeException.getCause()
        );

        assertTrue(
                writeException.getMessage()
                        .contains(
                                "Error adding attendance record"
                        )
        );
    }

    private static class RecordingAttendanceDAO
            implements InvocationHandler {

        private int getByIDCalls;
        private int getByEmployeeCalls;
        private int getByDateCalls;
        private int getAllCalls;
        private int addCalls;
        private int updateCalls;
        private int deleteCalls;

        private String failMethod;

        @Override
        public Object invoke(
                Object proxy,
                Method method,
                Object[] args
        ) throws Throwable {

            if (method.getDeclaringClass()
                    == Object.class) {

                return switch (
                        method.getName()
                ) {

                    case "toString" ->
                            "RecordingAttendanceDAO";

                    case "hashCode" ->
                            System.identityHashCode(
                                    proxy
                            );

                    case "equals" ->
                            proxy == args[0];

                    default ->
                            null;
                };
            }

            if (method.getName()
                    .equals(
                            failMethod
                    )) {

                throw new SQLException(
                        "Synthetic attendance DAO failure"
                );
            }

            return switch (
                    method.getName()
            ) {

                case "getAttendanceByID" -> {

                    getByIDCalls++;

                    yield null;
                }

                case "getAttendanceByEmployeeID" -> {

                    getByEmployeeCalls++;

                    yield List.of();
                }

                case "getAttendanceByDate" -> {

                    getByDateCalls++;

                    yield List.of();
                }

                case "getAllAttendance" -> {

                    getAllCalls++;

                    yield List.of();
                }

                case "addAttendance" -> {

                    addCalls++;

                    yield null;
                }

                case "updateAttendance" -> {

                    updateCalls++;

                    yield null;
                }

                case "deleteAttendance" -> {

                    deleteCalls++;

                    yield null;
                }

                default ->

                        throw new UnsupportedOperationException(
                                "Unexpected AttendanceDAO method: "
                                        + method.getName()
                        );
            };
        }
    }
}