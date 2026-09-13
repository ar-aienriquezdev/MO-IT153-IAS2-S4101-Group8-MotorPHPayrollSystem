package test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ui.PageManagerLeaveAdmin;
import util.SessionManager;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import java.text.SimpleDateFormat;
import java.util.Date;

class ManagerLeaveAdminPageTest {

    private PageManagerLeaveAdmin page;

    /**
     * Establish an authenticated Immediate Supervisor session
     * before constructing the protected Manager Leave Admin page.
     */
    @BeforeEach
    void setUp() throws Exception {

        SessionManager.setSession(
                "U10001",
                10001,
                "Immediate Supervisor"
        );

        SwingUtilities.invokeAndWait(
                () -> page = new PageManagerLeaveAdmin()
        );
    }

    /**
     * Dispose the page and clear authentication state after
     * every test so sessions do not leak between tests.
     */
    @AfterEach
    void tearDown() throws Exception {

        if (page != null) {

            SwingUtilities.invokeAndWait(
                    () -> page.dispose()
            );
        }

        SessionManager.clearSession();
    }

    @Test
    void testAllUIComponentsExistAndInitialState() {

        assertNotNull(
                page.employeeIDComboBox,
                "EmployeeID ComboBox exists"
        );

        assertNotNull(
                page.JDateChooser,
                "DateChooser exists"
        );

        assertNotNull(
                page.leaveTable,
                "Leave JTable exists"
        );

        assertNotNull(
                page.jScrollPane1,
                "JScrollPane exists"
        );

        assertNotNull(
                page.backButton,
                "Back Button exists"
        );

        assertNotNull(
                page.viewOwnRecordButton,
                "View Own Record Button exists"
        );

        assertNotNull(
                page.refreshButton,
                "Refresh Table Button exists"
        );

        TableModel model =
                page.leaveTable.getModel();

        String[] expectedCols = {
            "Leave ID",
            "Employee ID",
            "Approval Status",
            "Leave Type",
            "Leave Start",
            "Leave End",
            "Leave Reason"
        };

        for (int i = 0;
             i < expectedCols.length;
             i++) {

            assertEquals(
                    expectedCols[i],
                    model.getColumnName(i)
            );
        }

        assertTrue(
                model.getRowCount() > 0,
                "Leave table should not be empty"
        );
    }

    @Test
    void testEmployeeIDComboBox_Filter10002()
            throws Exception {

        JComboBox<String> combo =
                page.employeeIDComboBox;

        boolean found =
                false;

        for (int i = 0;
             i < combo.getItemCount();
             i++) {

            if ("10002".equals(
                    combo.getItemAt(i))) {

                found = true;
            }
        }

        assertTrue(
                found,
                "Combo box contains '10002'"
        );

        SwingUtilities.invokeAndWait(
                () -> combo.setSelectedItem("10002")
        );

        TableModel model =
                page.leaveTable.getModel();

        assertTrue(
                model.getRowCount() > 0,
                "Table shows leave(s) for employee 10002"
        );

        for (int i = 0;
             i < model.getRowCount();
             i++) {

            String empID =
                    model.getValueAt(i, 1)
                            .toString();

            assertEquals(
                    "10002",
                    empID,
                    "Each row is for employee 10002"
            );
        }
    }

    @Test
    void testDateChooser_FilterJuly2024()
            throws Exception {

        SimpleDateFormat fmt =
                new SimpleDateFormat(
                        "yyyy-MM-dd"
                );

        Date july15 =
                fmt.parse(
                        "2024-07-15"
                );

        SwingUtilities.invokeAndWait(
                () -> {
                    page.JDateChooser
                            .setDate(july15);

                    page.JDateChooser
                            .getDateEditor()
                            .setDate(july15);
                }
        );

        TableModel model =
                page.leaveTable.getModel();

        assertTrue(
                model.getRowCount() > 0,
                "Table filtered by July 2024 should have rows"
        );

        for (int i = 0;
             i < model.getRowCount();
             i++) {

            String leaveStart =
                    model.getValueAt(i, 4)
                            .toString();

            String leaveEnd =
                    model.getValueAt(i, 5)
                            .toString();

            boolean isJuly =
                    leaveStart.startsWith(
                            "2024-07-"
                    )
                    || leaveEnd.startsWith(
                            "2024-07-"
                    );

            assertTrue(
                    isJuly,
                    String.format(
                            "Leave Start or End is in July 2024: %s - %s",
                            leaveStart,
                            leaveEnd
                    )
            );
        }
    }

    @Test
    void testEmployeeIDComboBoxAndDateChooserTogether()
            throws Exception {

        SwingUtilities.invokeAndWait(
                () -> page.employeeIDComboBox
                        .setSelectedItem("10002")
        );

        SimpleDateFormat fmt =
                new SimpleDateFormat(
                        "yyyy-MM-dd"
                );

        Date july15 =
                fmt.parse(
                        "2024-07-15"
                );

        SwingUtilities.invokeAndWait(
                () -> {
                    page.JDateChooser
                            .setDate(july15);

                    page.JDateChooser
                            .getDateEditor()
                            .setDate(july15);
                }
        );

        TableModel model =
                page.leaveTable.getModel();

        assertTrue(
                model.getRowCount() > 0,
                "Filtered table should not be empty"
        );

        for (int i = 0;
             i < model.getRowCount();
             i++) {

            String empID =
                    model.getValueAt(i, 1)
                            .toString();

            String leaveStart =
                    model.getValueAt(i, 4)
                            .toString();

            String leaveEnd =
                    model.getValueAt(i, 5)
                            .toString();

            assertEquals(
                    "10002",
                    empID,
                    "Row is for employee 10002"
            );

            boolean isJuly =
                    leaveStart.startsWith(
                            "2024-07-"
                    )
                    || leaveEnd.startsWith(
                            "2024-07-"
                    );

            assertTrue(
                    isJuly,
                    String.format(
                            "Leave Start or End is in July 2024: %s - %s",
                            leaveStart,
                            leaveEnd
                    )
            );
        }
    }

    @Test
    void testRefreshButtonResetsFilters()
            throws Exception {

        SwingUtilities.invokeAndWait(
                () -> page.employeeIDComboBox
                        .setSelectedItem("10003")
        );

        SimpleDateFormat fmt =
                new SimpleDateFormat(
                        "yyyy-MM-dd"
                );

        Date july15 =
                fmt.parse(
                        "2024-07-15"
                );

        SwingUtilities.invokeAndWait(
                () -> page.JDateChooser
                        .setDate(july15)
        );

        SwingUtilities.invokeAndWait(
                () -> page.refreshButton
                        .doClick()
        );

        assertEquals(
                "All",
                page.employeeIDComboBox
                        .getSelectedItem()
        );

        assertNull(
                page.JDateChooser.getDate()
        );

        TableModel model =
                page.leaveTable.getModel();

        assertTrue(
                model.getRowCount() > 0,
                "All leaves should show after refresh"
        );

        boolean foundNot10003 =
                false;

        for (int i = 0;
             i < model.getRowCount();
             i++) {

            String empID =
                    model.getValueAt(i, 1)
                            .toString();

            if (!"10003".equals(empID)) {
                foundNot10003 = true;
            }
        }

        assertTrue(
                foundNot10003,
                "At least one row not for 10003 after refresh"
        );
    }

    @Test
    void testBackAndViewOwnRecordButtonsExist() {

        JButton backBtn =
                page.backButton;

        JButton viewOwnBtn =
                page.viewOwnRecordButton;

        assertNotNull(
                backBtn
        );

        assertNotNull(
                viewOwnBtn
        );

        assertEquals(
                "Back",
                backBtn.getText()
        );

        assertEquals(
                "View Own Record",
                viewOwnBtn.getText()
        );
    }
}