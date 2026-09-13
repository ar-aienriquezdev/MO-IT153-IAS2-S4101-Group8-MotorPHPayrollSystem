# 🚀 MotorPH Payroll System
*Information Assurance and Security 2 (IAS2) • S4101 • Group 8 • Java Swing • MySQL • JasperReports*

---

## 👥 Project Team

| Member | Role |
|---|---|
| **Abegail Imee Enriquez** | Project Coordinator |
| **Alvin Tubtub** | Analysis and Documentation Lead |
| **Michael Galario** | Security Solution Designer |
| **Russell Jae Udaundo** | Risk and Threat Analyst |

---

## 🔍 Project Overview

The **MotorPH Payroll System** is a Java Swing desktop-based payroll management application continued from the team's earlier Advanced Object-Oriented Programming (AOOP) implementation and currently being strengthened for **Information Assurance and Security 2 (IAS2)**.

The system supports employee records, attendance tracking, leave and overtime processing, payroll computation, payslip generation, and role-aware access. It uses JDBC to communicate with a local/on-premise MySQL database containing employee, authentication, attendance, payroll, and other business-critical records.

### Current IAS2 Security Hardening

The current pre-audit implementation includes:

- ✅ **BCrypt password hashing** with application-side password verification
- ✅ **Timed account lockout** after repeated failed login attempts
- ✅ **Structured authentication audit logging** to MySQL and local Log4j2 audit files
- ✅ **Environment-based database credential configuration** so database usernames and passwords are not stored in Java source code
- ✅ **PreparedStatement-based SQL access** throughout the DAO layer
- ✅ **Backend role-based access control (RBAC)** — centralized permissions and service-layer authorization now protect sensitive operations based on user role and resource ownership.
- ✅ **JaCoCo test coverage reporting** — Maven-integrated coverage reporting is configured to measure automated test coverage across the application.
- 🔄 **SonarQube Cloud static analysis** — planned as the next pre-audit analysis tool.

> The current IAS2 branch is intentionally being developed and tested in stages before the formal security audit.

---

## 🖥️ System Requirements

| Layer | Recommended | Notes |
|---|---|---|
| **JDK** | JDK 23 | Project compile/runtime target |
| **IDE** | Apache NetBeans 23+ | NetBeans 24 is also supported for the current project setup |
| **Build Tool** | Maven | Dependencies are managed through `pom.xml` |
| **Database** | MySQL 8.x | Local/on-premise development database |
| **Reporting** | JasperReports 6.20+ | Dependency is managed through Maven |
| **Testing** | JUnit 5 | Security and application tests |
| **Logging** | Log4j2 | Authentication audit logging |
| **OS** | Windows, macOS, or Linux | Local environment setup differs slightly by OS |

---

## 🔐 Default Test Logins

The following accounts are retained for **local testing, mentor review, and project evaluation**.

| Role | User ID | Email | Password |
|---|---|---|---|
| IT | `U10005` | `ehernandez@motor.ph` | `Hernandez@10005` |
| HR | `U10006` | `avillanueva@motor.ph` | `Villanueva@10006` |
| Manager | `U10002` | `alim@motor.ph` | `Lim@10002` |
| Finance | `U10011` | `asalcedo@motor.ph` | `Salcedo@10011` |
| Employee | `U10008` | `aromualdez@motor.ph` | `Romualdez@10008` |

Users can log in using either their **User ID** or **email address**.

> ⚠️ These are application test accounts only. Do not reuse these passwords for real or production accounts.

---

## 💳 Payslip & Payroll Access

To view or generate payslips and payroll summaries, choose a valid date range using the JDateChooser or built-in calendar picker.

1. **Start Date:** cannot be earlier than **June 3, 2024**
2. **End Date:** cannot be later than **December 31, 2024**

> ⚠️ If either date falls outside this range, the system will reject the request.

After selecting both dates, click **Generate** to compute and display the payslip or payroll report.

---

## 📦 Installation & First Run

### 1. Clone the Repository

Open Terminal, PowerShell, Command Prompt, or Git Bash and clone the current project repository:

```bash
git clone https://github.com/ar-aienriquezdev/MO-IT153-IAS2-S4101-Group8-MotorPHPayrollSystem
```

Then open the project in NetBeans as a Maven project.

---

### 2. Import the MySQL Database

1. Download the current SQL file from the **MotorPH Payroll System SQL** folder:
   <https://drive.google.com/drive/folders/1SoM4fx7P42JrqvOrqJZqKarLgRVgy571?usp=sharing>
2. Open **MySQL Workbench**.
3. Import the provided `MotorPHPayrollSystem.sql` file.
4. Confirm that the local schema is available as:

```text
payrollsystem_db
```

> If an older AOOP database is being reused, make sure the IAS2 schema updates required for account lockout and audit logging have also been applied.

---

### 3. Configure Database Credentials

The application **does not store the MySQL username or password in `DatabaseConnection.java`**.

Database credentials must be provided locally through environment variables:

```text
DB_URL
DB_USER
DB_PASS
```

The default local database URL used by the application is:

```text
jdbc:mysql://localhost:3306/payrollsystem_db
```

`DB_USER` and `DB_PASS` must be supplied by each developer or tester based on their own local MySQL configuration.

> 🔒 Never commit a real MySQL password to the repository, README, `pom.xml`, or Java source files.

#### macOS

For NetBeans launched from Finder or the Dock, the current development setup can be configured using:

```bash
launchctl setenv DB_URL "jdbc:mysql://localhost:3306/payrollsystem_db"
launchctl setenv DB_USER "<your_mysql_username>"
launchctl setenv DB_PASS "<your_mysql_password>"
```

After setting the values, **quit and reopen NetBeans** before running the project.

To verify the non-sensitive values:

```bash
launchctl getenv DB_URL
launchctl getenv DB_USER
```

To verify that the password variable exists without printing it:

```bash
if [ -n "$(launchctl getenv DB_PASS)" ]; then
    echo "DB_PASS is set"
else
    echo "DB_PASS is NOT set"
fi
```

> Note: `launchctl setenv` is suitable for the current local development workflow, but the values may need to be set again after logout or restart. A secure credential manager such as macOS Keychain may be used later for more permanent local secret handling.

#### Windows

Set the following **User Environment Variables** in Windows:

```text
DB_URL=jdbc:mysql://localhost:3306/payrollsystem_db
DB_USER=<your_mysql_username>
DB_PASS=<your_mysql_password>
```

One way to configure them is:

1. Search Windows for **Edit environment variables for your account**.
2. Under **User variables**, create `DB_URL`, `DB_USER`, and `DB_PASS`.
3. Save the changes.
4. Fully close and reopen NetBeans before running the project.

PowerShell users may also configure User-level variables using:

```powershell
[System.Environment]::SetEnvironmentVariable("DB_URL", "jdbc:mysql://localhost:3306/payrollsystem_db", "User")
[System.Environment]::SetEnvironmentVariable("DB_USER", "<your_mysql_username>", "User")
[System.Environment]::SetEnvironmentVariable("DB_PASS", "<your_mysql_password>", "User")
```

#### Linux / Other Unix-like Systems

For a shell session:

```bash
export DB_URL="jdbc:mysql://localhost:3306/payrollsystem_db"
export DB_USER="<your_mysql_username>"
export DB_PASS="<your_mysql_password>"
```

Launch NetBeans from the same environment so the application inherits the variables.

For a persistent setup, configure the variables using the operating system's preferred user-session or credential-management method rather than storing real credentials inside the repository.

#### Optional JVM System Properties

`DatabaseConnection` also supports equivalent JVM system properties:

```text
db.url
db.user
db.pass
```

These can be used when an IDE or deployment environment is configured to pass JVM properties directly.

---

### 4. Clean and Build

In NetBeans:

```text
Right-click project → Clean and Build
```

Or from a terminal inside the repository:

```bash
mvn clean test
```

Make sure all required database environment variables are available to the process running the tests.

---

### 5. Run the Application

In NetBeans:

```text
Right-click project → Run
```

or use the **Run Project** button.

Log in using one of the test accounts listed above.

---

## 🛡️ Current Authentication Security Behavior

### BCrypt Password Verification

Passwords are stored as BCrypt hashes and verified inside the Java application rather than being compared directly in SQL.

### Timed Account Lockout

- Maximum failed attempts: **5**
- Lockout duration: **15 minutes**
- A successful login resets previous failed-attempt state
- Attempts made while the lock is active remain blocked even if the correct password is entered

### Authentication Audit Logging

Authentication-related events are written to the MySQL `audit_log` table and to the local Log4j2 audit file.

Current authentication events include:

```text
LOGIN_SUCCESS
LOGIN_FAILED
ACCOUNT_LOCKED
LOGIN_BLOCKED_LOCKED
```

The local audit file is generated under:

```text
logs/motorph-audit.log
```

The `/logs/` directory is excluded from Git.

---

## 🧪 Testing Notes

The project uses **JUnit 5** for automated tests.

Current IAS2 security-focused tests include:

```text
SecurityPasswordUtilTest
SecurityLoginServiceLockoutTest
SecurityAuditLoggerTest
```

Run tests through NetBeans or:

```bash
mvn test
```

### Manual Test Considerations

Some existing application tests may include GUI interactions or PDF generation. During full test execution:

- Modal dialogs may appear.
- Generated PDFs may open automatically.
- A local MySQL database connection may be required.
- Database environment variables must be configured before DB-dependent tests run.

Monitor the test execution and close any blocking GUI dialogs or generated PDF viewers as needed.

---

## 🏗️ Project Layout

```text
src/main/java/
├── db/             Database connection management
├── pojo/           Plain Java objects
├── dao/            DAO interfaces
├── daoimpl/        JDBC / PreparedStatement implementations
├── service/        Authentication, validation, payroll, and business logic
├── ui/             Java Swing application screens
├── ui/base/        Shared Swing templates and form helpers
└── util/           Password, session, migration, and audit utilities

src/main/resources/
├── log4j2.xml      Log4j2 audit-file configuration
├── background/     Application UI resources
└── reports/        JasperReports templates/resources

src/test/java/
└── test/           JUnit 5 application and security tests
```

---

## 🛠️ Development Notes

- **Database password:** never hardcode it in `DatabaseConnection.java`; use runtime environment variables or JVM properties.
- **Password storage:** new or updated passwords must pass through the BCrypt password utility.
- **Audit events:** never log plaintext passwords or BCrypt hashes.
- **SQL access:** continue using `PreparedStatement` for parameterized database queries.
- **Business logic:** keep authorization and validation in the service/backend layer rather than relying only on Swing UI visibility.
- **Generated logs:** `/logs/` is intentionally ignored by Git.
- **Before committing:** verify that no real database credential or local secret appears in the Git diff.

---

## 📚 Documentation & Assets

- 📄 **Expanded System Design Documentation:** <https://docs.google.com/spreadsheets/d/1O2_Qsl-e7WOu_GajDM0SVfFg9hynrvFhv8UL3yjqZXo>
- ✅ **Testing Documentation:** <https://docs.google.com/spreadsheets/d/1CtGctWMrtfvwRpn_sDlLcfvddDmkd6QHmhEyFHldKI8>
- 💾 **Current MotorPH Payroll System SQL Folder:** <https://drive.google.com/drive/folders/1SoM4fx7P42JrqvOrqJZqKarLgRVgy571?usp=sharing>

---

## 🤝 Contributing

1. Work from the appropriate development or feature branch.
2. Keep business logic in the service/backend layer rather than the UI.
3. Do not commit database passwords, local credentials, generated logs, or other secrets.
4. Run the relevant JUnit tests before opening or merging a pull request.
5. Use pull-request review before merging completed IAS2 changes into the protected baseline branch.

---

## 📝 License

© 2026 MotorPH IAS2 Group 8. All rights reserved.

This repository is maintained for academic use, testing, and project evaluation.

---

Made with ☕, deadlines, and secure-by-design improvements.
