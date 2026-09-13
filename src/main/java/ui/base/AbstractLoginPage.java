package ui.base;

import service.LoginService;
import pojo.User;
import util.SessionManager;
import util.BlueButton;
import ui.PageEmployeeHome;
import ui.PageFinanceHome;
import ui.PageHRHome;
import ui.PageITHome;
import ui.PageManagerHome;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractLoginPage extends JFrame {
  protected JTextField     usernameField;
  protected JPasswordField passwordField;
  protected BlueButton     loginButton;

  /**
   * Call once, *after* your NetBeans initComponents():
   */
  protected void setupLoginPage(
    JTextField     unameField,
    JPasswordField pwdField,
    BlueButton     loginBtn
  ) {
    this.usernameField = unameField;
    this.passwordField = pwdField;
    this.loginButton   = loginBtn;

    // reset backgrounds
    usernameField.setBackground(Color.WHITE);
    passwordField.setBackground(Color.WHITE);

    // real-time: clear red highlight as soon as user types
    DocumentListener clearRed = new DocumentListener() {
      public void insertUpdate(DocumentEvent e) { clear(); }
      public void removeUpdate(DocumentEvent e) { clear(); }
      public void changedUpdate(DocumentEvent e) { clear(); }
      private void clear() {
        if (!usernameField.getText().trim().isEmpty())
          usernameField.setBackground(Color.WHITE);
        if (passwordField.getPassword().length > 0)
          passwordField.setBackground(Color.WHITE);
      }
    };
    usernameField.getDocument().addDocumentListener(clearRed);
    passwordField.getDocument().addDocumentListener(clearRed);

    // wire login button
    loginButton.addActionListener(ev -> performLogin());
  }

  /** Attempts login, with single-dialog enforcement and status-specific handling */
  protected void performLogin() {
    String userInput = usernameField.getText().trim();
    String pwd       = new String(passwordField.getPassword()).trim();

    // 1) check empties
    if (userInput.isEmpty() || pwd.isEmpty()) {
      if (userInput.isEmpty()) usernameField.setBackground(Color.PINK);
      if (pwd.isEmpty())       passwordField.setBackground(Color.PINK);
      JOptionPane.showMessageDialog(
        this,
        "Please fill in both UserID/Email and Password fields.",
        "Missing Fields",
        JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    try {
      LoginService svc = new LoginService();
      User user = svc.login(userInput, pwd);

      // 2) login failed — determine the reason
    if (user == null) {

        boolean exists =
                svc.doesUserExist(userInput);

        if (!exists) {

            usernameField.setBackground(
                    Color.PINK
            );

            JOptionPane.showMessageDialog(
                    this,
                    "The account you entered does not exist.",
                    "Login Failed",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        /*
         * The account exists, so check whether it has been
         * temporarily locked after repeated failed attempts.
         */
        Timestamp lockedUntil =
                svc.getLockedUntil(userInput);

        Timestamp now =
                new Timestamp(
                        System.currentTimeMillis()
                );

        if (lockedUntil != null
                && lockedUntil.after(now)) {

            passwordField.setBackground(
                    Color.PINK
            );

            LocalDateTime lockoutEnd =
                    lockedUntil.toLocalDateTime();

            LocalDateTime currentTime =
                    LocalDateTime.now();

            long secondsRemaining =
                    Duration.between(
                            currentTime,
                            lockoutEnd
                    ).getSeconds();

            long minutesRemaining =
                    Math.max(
                            1,
                            (secondsRemaining + 59) / 60
                    );

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "MMM d, yyyy 'at' h:mm a"
                    );

            String formattedTime =
                    lockoutEnd.format(
                            formatter
                    );

            JOptionPane.showMessageDialog(
                    this,
                    "Your account has been temporarily locked "
                            + "due to too many failed login attempts.\n\n"
                            + "Please try again after "
                            + formattedTime
                            + ".\n"
                            + "Approximately "
                            + minutesRemaining
                            + " minute"
                            + (minutesRemaining == 1 ? "" : "s")
                            + " remaining.",
                    "Account Temporarily Locked",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        /*
         * Account exists and is not currently locked,
         * so this is an ordinary incorrect-password result.
         */
        passwordField.setBackground(
                Color.PINK
        );

        JOptionPane.showMessageDialog(
                this,
                "The password you entered is incorrect.",
                "Login Failed",
                JOptionPane.WARNING_MESSAGE
        );

        return;
    }

      // 3) account-status check
      String status = user.getAccountStatus();
      switch (status.toLowerCase()) {
        case "active" -> {
          // success! set session, greet, route
          int empId = svc.getEmployeeIDByUserID(user.getUserID());
          SessionManager.setSession(
                  user.getUserID(),
                  empId,
                  user.getUserRole()
          );
          JOptionPane.showMessageDialog(
            this,
            "Welcome, " + user.getUsername() + "!",
            "Login Successful",
            JOptionPane.INFORMATION_MESSAGE
          );
          switch (user.getUserRole()) {
            case "Employee"             -> new PageEmployeeHome().setVisible(true);
            case "Finance"              -> new PageFinanceHome().setVisible(true);
            case "HR"                   -> new PageHRHome().setVisible(true);
            case "IT"                   -> new PageITHome().setVisible(true);
            case "Immediate Supervisor" -> new PageManagerHome().setVisible(true);
            default -> JOptionPane.showMessageDialog(
              this,
              "Unknown role: " + user.getUserRole(),
              "Error",
              JOptionPane.ERROR_MESSAGE
            );
          }
          dispose();
        }
        case "pending" -> JOptionPane.showMessageDialog(
          this,
          "Your account is pending confirmation. Please await activation.",
          "Account Pending",
          JOptionPane.INFORMATION_MESSAGE
        );
        case "rejected" -> JOptionPane.showMessageDialog(
          this,
          "The account you entered does not exist.",
          "Login Failed",
          JOptionPane.INFORMATION_MESSAGE
        );
        case "deactivated" -> JOptionPane.showMessageDialog(
          this,
          "Your account has been deactivated. Please contact support.",
          "Account Deactivated",
          JOptionPane.WARNING_MESSAGE
        );
        default -> JOptionPane.showMessageDialog(
          this,
          "Your account status \"" + status + "\" does not permit login.",
          "Login Denied",
          JOptionPane.WARNING_MESSAGE
        );
      }
    }
    catch (Exception ex) {
      JOptionPane.showMessageDialog(
        this,
        "Error during login: " + ex.getMessage(),
        "Error",
        JOptionPane.ERROR_MESSAGE
      );
      ex.printStackTrace();
    }
  }
}