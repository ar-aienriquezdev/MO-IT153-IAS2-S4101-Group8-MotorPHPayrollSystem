package test;

import dao.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pojo.User;
import service.UserService;
import util.PasswordUtil;
import util.SessionManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityUserServiceCoverageTest {

    private UserService userService;
    private RecordingUserDAO handler;

    @BeforeEach
    void setUp() throws Exception {

        SessionManager.clearSession();

        userService =
                new UserService();

        handler =
                new RecordingUserDAO();

        UserDAO proxy =
                (UserDAO) Proxy.newProxyInstance(
                        UserDAO.class.getClassLoader(),
                        new Class<?>[]{
                                UserDAO.class
                        },
                        handler
                );

        Field field =
                UserService.class
                        .getDeclaredField(
                                "userDAO"
                        );

        field.setAccessible(true);

        field.set(
                userService,
                proxy
        );
    }

    @AfterEach
    void tearDown() {

        SessionManager.clearSession();
    }

    @Test
    void itCanRetrieveUsersThroughAllAuthorizedLookupPaths() {

        SessionManager.setSession(
                "C6_IT",
                91001,
                "IT"
        );

        User user =
                user(
                        "C6_TARGET",
                        "Target User",
                        "target@example.test",
                        PasswordUtil.hash(
                                "Existing-Secure!2026"
                        )
                );

        handler.users.put(
                user.getUserID(),
                user
        );

        assertSame(
                user,
                userService.getUserByUserID(
                        "C6_TARGET"
                )
        );

        assertSame(
                user,
                userService.getUserByEmail(
                        "target@example.test"
                )
        );

        assertSame(
                user,
                userService.getUserByUsername(
                        "Target User"
                )
        );

        List<User> all =
                userService.getAllUsers();

        assertEquals(
                1,
                all.size()
        );

        assertSame(
                user,
                all.get(0)
        );
    }

    @Test
    void employeeCanRetrieveOwnAccount() {

        SessionManager.setSession(
                "C6_SELF",
                91002,
                "Employee"
        );

        User user =
                user(
                        "C6_SELF",
                        "Self User",
                        "self@example.test",
                        PasswordUtil.hash(
                                "Existing-Secure!2026"
                        )
                );

        handler.users.put(
                user.getUserID(),
                user
        );

        assertSame(
                user,
                userService.getUserByUserID(
                        "C6_SELF"
                )
        );
    }

    @Test
    void itAddUserHashesStrongPlaintextBeforeDAO() {

        SessionManager.setSession(
                "C6_IT",
                91003,
                "IT"
        );

        User user =
                user(
                        "C6_NEW",
                        "New User",
                        "new@example.test",
                        "N3w-Secure-Pass!2026"
                );

        userService.addUser(
                user
        );

        assertEquals(
                1,
                handler.addCalls
        );

        assertTrue(
                PasswordUtil.isHash(
                        user.getPassword()
                )
        );

        assertSame(
                user,
                handler.users.get(
                        "C6_NEW"
                )
        );
    }

    @Test
    void itUpdateUserPreservesExistingBCryptHash() {

        SessionManager.setSession(
                "C6_IT",
                91004,
                "IT"
        );

        String existingHash =
                PasswordUtil.hash(
                        "Existing-Secure!2026"
                );

        User user =
                user(
                        "C6_UPDATE",
                        "Update User",
                        "update@example.test",
                        existingHash
                );

        userService.updateUser(
                user
        );

        assertEquals(
                1,
                handler.updateCalls
        );

        assertEquals(
                existingHash,
                user.getPassword()
        );
    }

    @Test
    void itUpdateUserHashesNewStrongPlaintext() {

        SessionManager.setSession(
                "C6_IT",
                91005,
                "IT"
        );

        User user =
                user(
                        "C6_UPDATE_PW",
                        "Password Update",
                        "password@example.test",
                        "An0ther-Secure!2026"
                );

        userService.updateUser(
                user
        );

        assertEquals(
                1,
                handler.updateCalls
        );

        assertTrue(
                PasswordUtil.isHash(
                        user.getPassword()
                )
        );
    }

    @Test
    void fullAdministrationRejectsNullUser() {

        SessionManager.setSession(
                "C6_IT",
                91006,
                "IT"
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService
                                .addUser(null)
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("user")
        );
    }

    @Test
    void fullAdministrationRejectsEmptyPassword() {

        SessionManager.setSession(
                "C6_IT",
                91007,
                "IT"
        );

        User user =
                user(
                        "C6_EMPTY_PASSWORD",
                        "Empty Password",
                        "empty@example.test",
                        ""
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.addUser(
                        user
                )
        );

        assertEquals(
                0,
                handler.addCalls
        );
    }

    @Test
    void ownPasswordChangeHashesDifferentStrongPassword() {

        SessionManager.setSession(
                "C6_SELF_PASSWORD",
                91008,
                "Employee"
        );

        User current =
                user(
                        "C6_SELF_PASSWORD",
                        "Password Owner",
                        "owner@example.test",
                        PasswordUtil.hash(
                                "Current-Secure!2026"
                        )
                );

        handler.users.put(
                current.getUserID(),
                current
        );

        userService.updateOwnPassword(
                "Replacement-Secure!2027"
        );

        assertEquals(
                1,
                handler.updateCalls
        );

        assertTrue(
                PasswordUtil.verify(
                        "Replacement-Secure!2027",
                        current.getPassword()
                )
        );

        assertFalse(
                PasswordUtil.verify(
                        "Current-Secure!2026",
                        current.getPassword()
                )
        );
    }

    @Test
    void ownPasswordChangeRejectsReuseOfCurrentPassword() {

        SessionManager.setSession(
                "C6_REUSE_PASSWORD",
                91009,
                "Employee"
        );

        String password =
                "Current-Secure!2026";

        User current =
                user(
                        "C6_REUSE_PASSWORD",
                        "Reuse Test",
                        "reuse@example.test",
                        PasswordUtil.hash(
                                password
                        )
                );

        handler.users.put(
                current.getUserID(),
                current
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService
                                .updateOwnPassword(
                                        password
                                )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("differ")
        );

        assertEquals(
                0,
                handler.updateCalls
        );
    }

    @Test
    void ownPasswordChangeRejectsMissingCurrentUser() {

        SessionManager.setSession(
                "C6_MISSING_SELF",
                91010,
                "Employee"
        );

        assertThrows(
                IllegalStateException.class,
                () -> userService
                        .updateOwnPassword(
                                "Replacement-Secure!2027"
                        )
        );

        assertEquals(
                0,
                handler.updateCalls
        );
    }

    @Test
    void hrStatusUpdateCanonicalizesCaseAndPersists() {

        SessionManager.setSession(
                "C6_HR",
                91011,
                "HR"
        );

        User target =
                user(
                        "C6_STATUS",
                        "Status User",
                        "status@example.test",
                        PasswordUtil.hash(
                                "Existing-Secure!2026"
                        )
                );

        target.setAccountStatus(
                "Active"
        );

        handler.users.put(
                target.getUserID(),
                target
        );

        userService.updateAccountStatus(
                "C6_STATUS",
                "deactivated"
        );

        assertEquals(
                "Deactivated",
                target.getAccountStatus()
        );

        assertEquals(
                1,
                handler.updateCalls
        );
    }

    @Test
    void statusUpdateRejectsUnsupportedAndNullValues() {

        SessionManager.setSession(
                "C6_HR",
                91012,
                "HR"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService
                        .updateAccountStatus(
                                "C6_TARGET",
                                "Disabled"
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService
                        .updateAccountStatus(
                                "C6_TARGET",
                                null
                        )
        );
    }

    @Test
    void statusUpdateRejectsMissingTargetUser() {

        SessionManager.setSession(
                "C6_HR",
                91013,
                "HR"
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService
                                .updateAccountStatus(
                                        "C6_DOES_NOT_EXIST",
                                        "Active"
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "C6_DOES_NOT_EXIST"
                        )
        );
    }

    @Test
    void hrCanDeleteUserThroughNarrowStatusPermission() {

        SessionManager.setSession(
                "C6_HR",
                91014,
                "HR"
        );

        User user =
                user(
                        "C6_DELETE",
                        "Delete User",
                        "delete@example.test",
                        PasswordUtil.hash(
                                "Existing-Secure!2026"
                        )
                );

        handler.users.put(
                user.getUserID(),
                user
        );

        userService.deleteUser(
                "C6_DELETE"
        );

        assertEquals(
                1,
                handler.deleteCalls
        );

        assertFalse(
                handler.users.containsKey(
                        "C6_DELETE"
                )
        );
    }

    @Test
    void daoLookupFailureIsWrappedAsRuntimeException() {

        SessionManager.setSession(
                "C6_IT",
                91015,
                "IT"
        );

        handler.failMethod =
                "getUserByEmail";

        handler.failure =
                new SQLException(
                        "Synthetic lookup failure"
                );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService
                                .getUserByEmail(
                                        "failure@example.test"
                                )
                );

        assertNotNull(
                exception.getCause()
        );

        assertInstanceOf(
                SQLException.class,
                exception.getCause()
        );
    }

    @Test
    void genericUpdateFailureIsWrapped() {

        SessionManager.setSession(
                "C6_IT",
                91016,
                "IT"
        );

        User user =
                user(
                        "C6_UPDATE_FAILURE",
                        "Update Failure",
                        "update-failure@example.test",
                        PasswordUtil.hash(
                                "Existing-Secure!2026"
                        )
                );

        handler.failMethod =
                "updateUser";

        handler.failure =
                new SQLException(
                        "Synthetic non-connection failure"
                );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService
                                .updateUser(
                                        user
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Error updating user"
                        )
        );
    }

    private static User user(
            String userID,
            String username,
            String email,
            String password
    ) {

        User user =
                new User();

        user.setUserID(
                userID
        );

        user.setUsername(
                username
        );

        user.setEmail(
                email
        );

        user.setPassword(
                password
        );

        user.setUserRole(
                "Employee"
        );

        user.setAccountStatus(
                "Active"
        );

        user.setPositionID(
                1
        );

        return user;
    }

    private static class RecordingUserDAO
            implements InvocationHandler {

        private final Map<String, User> users =
                new LinkedHashMap<>();

        private int addCalls;
        private int updateCalls;
        private int deleteCalls;

        private String failMethod;
        private SQLException failure;

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
                            "RecordingUserDAO";

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

                throw failure;
            }

            return switch (
                    method.getName()
            ) {

                case "getUserByUserID" ->

                        users.get(
                                (String) args[0]
                        );

                case "getUserByEmail" ->

                        users.values()
                                .stream()
                                .filter(
                                        user ->
                                                args[0]
                                                        .equals(
                                                                user.getEmail()
                                                        )
                                )
                                .findFirst()
                                .orElse(
                                        null
                                );

                case "getUserByUsername" ->

                        users.values()
                                .stream()
                                .filter(
                                        user ->
                                                args[0]
                                                        .equals(
                                                                user.getUsername()
                                                        )
                                )
                                .findFirst()
                                .orElse(
                                        null
                                );

                case "getAllUsers" ->

                        new ArrayList<>(
                                users.values()
                        );

                case "addUser" -> {

                    addCalls++;

                    User user =
                            (User) args[0];

                    users.put(
                            user.getUserID(),
                            user
                    );

                    yield null;
                }

                case "updateUser" -> {

                    updateCalls++;

                    User user =
                            (User) args[0];

                    users.put(
                            user.getUserID(),
                            user
                    );

                    yield null;
                }

                case "deleteUser" -> {

                    deleteCalls++;

                    users.remove(
                            (String) args[0]
                    );

                    yield null;
                }

                default ->

                        throw new UnsupportedOperationException(
                                "Unexpected UserDAO method: "
                                        + method.getName()
                        );
            };
        }
    }
}