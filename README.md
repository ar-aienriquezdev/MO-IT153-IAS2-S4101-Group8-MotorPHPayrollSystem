# 🚀 MotorPH Payroll System

*Information Assurance and Security 2 (IAS2) • S4101 • Group 8 • Java Swing • MySQL • JasperReports*

---

## 👥 Project Team

| Member | Role |
|---|---|
| **Abegail Imee Enriquez** | Project Coordinator and Lead Developer |
| **Alvin Tubtub** | Analysis and Documentation Lead |
| **Michael Galario** | Security Solution Designer |
| **Russell Jae Udaundo** | Risk and Threat Analyst |

---

## 🔍 Project Overview

The **MotorPH Payroll System** is a Java Swing desktop-based payroll management application continued from the team's earlier Advanced Object-Oriented Programming (AOOP) implementation and currently being strengthened for **Information Assurance and Security 2 (IAS2)**.

The system supports employee records, attendance tracking, leave and overtime processing, payroll computation, payslip generation, account administration, and role-aware access. It uses JDBC to communicate with a local/on-premise MySQL database containing employee, authentication, attendance, payroll, and other business-critical records.

The IAS2 implementation follows a defense-in-depth approach in which security is enforced across the authentication, authorization, service-validation, database-access, auditability, and security-assurance layers rather than relying only on the Java Swing user interface.

---

## 🛡️ Current IAS2 Security Hardening

Milestone 2 Controls **1 through 5** have been implemented and verified.

### ✅ Control 1 — Authentication Hardening

- BCrypt password hashing using application-side verification
- Centralized strong-password policy
- Minimum 15-character password requirement
- Uppercase, lowercase, numeric, and special-character requirements
- Service-layer password-policy enforcement
- Persistent failed-login tracking
- Temporary account lockout after five consecutive failed attempts
- Approximately 15-minute lockout duration
- Correct credentials remain blocked while the account is actively locked

### ✅ Control 2 — Centralized Backend RBAC and Resource Ownership

- Centralized `AuthorizationService`
- Authenticated session enforcement
- Explicit role-to-permission mapping
- Resource-ownership validation
- Backend authorization before protected DAO/database operations
- Fail-closed behavior for unauthenticated and unrecognized-role requests
- Authorization-denial audit logging

### ✅ Control 3 — Service-Layer Validation and SQL Safety

- Security-relevant business validation at the backend/service layer
- Protection against bypassing Swing UI validation
- Leave and overtime rule enforcement before persistence
- Parameterized JDBC `PreparedStatement` usage
- SQL-injection regression protection
- Automated service-layer validation tests

### ✅ Control 4 — Structured Audit Logging and Traceability

- Centralized `AuditLogger`
- Persistent MySQL `audit_log` records
- Protected Log4j2 audit-file output
- Authentication and authorization audit events
- User-account administration audit events
- Password-change audit events
- Leave approval/rejection audit events
- Overtime approval/rejection audit events
- Structured actor/action/target/result information
- Passwords and BCrypt hashes intentionally excluded from audit content

### ✅ Control 5 — Database Credential, Privilege, Network, and TLS Hardening

- Database URL, username, and password externalized from Java source
- No default/fallback operational database credentials in source code
- Dedicated MySQL application account: `motorph_app`
- Application database privileges restricted to:
  - `SELECT`
  - `INSERT`
  - `UPDATE`
  - `DELETE`
- No administrative or `GRANT OPTION` privileges for the application account
- Application account requires SSL/TLS
- MySQL `require_secure_transport = ON`
- MySQL listener restricted to loopback:
  - `127.0.0.1:3306`
- Connector/J centrally enforces:
  - `sslMode=REQUIRED`
- JDBC URL values attempting to override the centralized TLS policy are rejected
- Controlled plaintext database connection attempts are denied
- Secure runtime sessions verified using a negotiated TLS cipher
- Dedicated automated database-hardening security tests

### ✅ Control 6 — Secure Development, Security Testing, and Automated Quality Assurance

Control 6 is implemented and verified as the final Milestone 2 security-assurance layer.

Implemented assurance controls include:

- Maven-integrated **OWASP Dependency-Check 13.0.0**
- Dependency remediation with a documented JasperReports compatibility exception
- Dedicated Maven `security-regression` profile for security-critical JUnit tests
- Expanded security regression coverage for authentication, authorization, validation, audit logging, database hardening, attendance, and security-relevant UI/service branches
- **JaCoCo 0.8.13** HTML, XML, and CSV coverage generation
- **SonarQube Cloud** analysis from the `main` branch with JaCoCo XML import
- **Gitleaks** scans of both the current working tree and Git history
- Removal of production `printStackTrace()` calls in favor of centralized Log4j2 error logging
- Retained security-quality-gate and dependency-exception evidence under `docs/security/`

Final Control 6 verification achieved:

```text
Full regression suite
Tests run: 310
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

Dedicated security regression suite
Tests run: 158
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Final SonarQube Cloud verification on `main` reported:

- **Quality Gate: Passed**
- **New-code coverage: 80.03%** — required threshold: `>= 80.0%`
- **New-code duplication: 0.18%** — required threshold: `<= 3.0%`
- **Security rating: A**
- **Security issues: 0**
- **Security Hotspots: 0**
- **Security review rating: A**

The overall-code dashboard still reports legacy/shared-code technical debt, including an overall Reliability rating of **C** with **9 Medium reliability findings**. These remaining reliability findings are non-blocking under the configured Sonar quality gate and are retained for future cleanup rather than being represented as remediated.

SonarQube also reports **6 dependency risks** associated with JasperReports 6.21.5 and transitive JFreeChart 1.0.19 dependencies. These are tracked separately from code-security issues. The existing OWASP exception `SEC-DEP-001` applies specifically to the documented JasperReports Dependency-Check finding and does not silently classify all Sonar dependency risks as remediated.

---

## 🖥️ System Requirements

| Layer | Current / Recommended | Notes |
|---|---|---|
| **JDK** | JDK 23 | Project compile/runtime target |
| **IDE** | Apache NetBeans 24 | Current verified development environment |
| **Build Tool** | Apache Maven | Dependencies and security tooling are managed through `pom.xml` |
| **Database** | MySQL 8.4.x | Control 5 verified using MySQL Community Server 8.4.5 |
| **JDBC Driver** | MySQL Connector/J 9.3.0 | Managed through Maven |
| **Reporting** | JasperReports 6.21.5 | Managed through Maven; residual dependency risk is documented |
| **Testing** | JUnit 5.13.1 | Functional and security-assurance tests |
| **Logging** | Log4j2 2.25.5 | Structured audit-file and centralized error logging |
| **Coverage** | JaCoCo 0.8.13 | Maven-integrated HTML/XML/CSV coverage reporting |
| **Dependency Security** | OWASP Dependency-Check 13.0.0 | Maven-integrated dependency-security gate |
| **Secret Scanning** | Gitleaks | Working-tree and Git-history secret scanning |
| **Static Analysis / Quality Gate** | SonarQube Cloud | Main-branch SAST/SCA analysis with JaCoCo coverage import |
| **OS** | Windows, macOS, or Linux | Database/server configuration differs by operating system |

---

## 🔐 Default Test Logins

The following accounts are retained for **controlled local testing, mentor review, and academic project evaluation**.

| Role | User ID | Email | Password |
|---|---|---|---|
| IT | `U10005` | `ehernandez@motor.ph` | `Hernandez@10005` |
| HR | `U10006` | `avillanueva@motor.ph` | `Villanueva@10006` |
| Manager | `U10002` | `alim@motor.ph` | `Lim@10002` |
| Finance | `U10011` | `asalcedo@motor.ph` | `Salcedo@10011` |
| Employee | `U10008` | `aromualdez@motor.ph` | `Romualdez@10008` |

Users can log in using either their **User ID** or **email address**.

> ⚠️ These credentials are intentionally published fixtures for the controlled academic dataset and mentor/evaluation workflow. They are not operational secrets and must not be reused for personal, external, or production accounts. Any deployment outside the academic test environment must replace them with environment-specific credentials.

---

## 💳 Payslip & Payroll Access

To view or generate payslips and payroll summaries, choose a valid date range using the JDateChooser or built-in calendar picker.

1. **Start Date:** cannot be earlier than **June 3, 2024**
2. **End Date:** cannot be later than **December 31, 2024**

> ⚠️ If either date falls outside this range, the system will reject the request.

After selecting both dates, click **Generate** to compute and display the payslip or payroll report.

---

# 📦 Installation & First Run

## 1. Clone the Repository

Open Terminal, PowerShell, Command Prompt, or Git Bash:

```bash
git clone https://github.com/ar-aienriquezdev/MO-IT153-IAS2-S4101-Group8-MotorPHPayrollSystem
```

Open the downloaded repository in NetBeans as a Maven project.

---

## 2. Import the MySQL Database

1. Download the current SQL file from the **MotorPH Payroll System SQL** folder:

   <https://drive.google.com/drive/folders/1SoM4fx7P42JrqvOrqJZqKarLgRVgy571?usp=sharing>

2. Open **MySQL Workbench**.
3. Import the provided `MotorPHPayrollSystem.sql` file.
4. Confirm that the required schema is available as:

```text
payrollsystem_db
```

> If an older AOOP database is being reused, ensure that all IAS2 schema changes required for authentication lockout and structured audit logging have also been applied.

---

# 🔒 3. Configure the Hardened MySQL Application Account

The Java application should **not run using the MySQL `root` account**.

Create a dedicated local account for MotorPH using an administrative MySQL session.

Replace the placeholder below with a strong local database password:

```sql
CREATE USER IF NOT EXISTS 'motorph_app'@'localhost'
IDENTIFIED BY '<STRONG_LOCAL_DATABASE_PASSWORD>'
REQUIRE SSL;
```

If the account already exists, ensure that SSL is required:

```sql
ALTER USER 'motorph_app'@'localhost'
REQUIRE SSL;
```

Remove excessive privileges:

```sql
REVOKE ALL PRIVILEGES, GRANT OPTION
FROM 'motorph_app'@'localhost';
```

Grant only the operations required by the application:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE
ON payrollsystem_db.*
TO 'motorph_app'@'localhost';
```

Verify the effective privileges:

```sql
SHOW GRANTS
FOR 'motorph_app'@'localhost';
```

Expected application privileges are limited to:

```text
SELECT
INSERT
UPDATE
DELETE
```

The application account should **not** receive administrative privileges, `ALL PRIVILEGES`, or `GRANT OPTION`.

To verify the account's SSL requirement without displaying authentication-verifier material:

```sql
SELECT
    User,
    Host,
    ssl_type
FROM mysql.user
WHERE User = 'motorph_app';
```

The hardened local account should report an SSL requirement such as:

```text
motorph_app | localhost | ANY
```

---

# 🌐 4. Harden the Local MySQL Server

For the current controlled local deployment, MySQL should listen only on the loopback interface and require encrypted transport.

The MySQL server configuration should contain:

```ini
[mysqld]
bind-address=127.0.0.1
require_secure_transport=ON
```

The location of the MySQL option file depends on the operating system and installation method.

### Verified macOS Oracle MySQL Setup

The current development system uses:

```text
/etc/my.cnf
```

with:

```ini
[mysqld]
bind-address=127.0.0.1
require_secure_transport=ON
```

The Oracle MySQL macOS service is managed by:

```text
com.oracle.oss.mysql.mysqld
```

and may be restarted using:

```bash
sudo launchctl kickstart -k system/com.oracle.oss.mysql.mysqld
```

> Homebrew or other MySQL installations may use a different service manager and configuration-file location.

### Verify the MySQL Security Configuration

Run:

```sql
SHOW VARIABLES
WHERE Variable_name IN (
    'bind_address',
    'require_secure_transport'
);
```

Expected values:

```text
bind_address               127.0.0.1
require_secure_transport   ON
```

On macOS/Linux, the actual TCP listener can also be checked using:

```bash
sudo lsof -nP -iTCP:3306 -sTCP:LISTEN
```

The expected listener is:

```text
127.0.0.1:3306
```

rather than:

```text
*:3306
0.0.0.0:3306
```

---

# 🔑 5. Configure Database Environment Variables

The application **does not store operational database credentials in `DatabaseConnection.java`**.

The following runtime configuration is required:

```text
DB_URL
DB_USER
DB_PASS
```

For the hardened local configuration:

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/payrollsystem_db
DB_USER=motorph_app
DB_PASS=<your_local_database_password>
```

> 🔒 Never commit a real database password to this repository, README, `pom.xml`, Java source code, screenshots, test fixtures, or documentation.

## Important TLS Behavior

`DatabaseConnection` centrally configures MySQL Connector/J with:

```text
sslMode=REQUIRED
```

The application therefore requires an encrypted JDBC connection and does not silently fall back to plaintext transport.

TLS-related options must **not** be appended to `DB_URL`.

Examples that are intentionally rejected include:

```text
?sslMode=DISABLED
?sslMode=PREFERRED
?useSSL=false
?requireSSL=false
```

The TLS policy is controlled centrally by `DatabaseConnection`.

---

## macOS

For NetBeans launched from Finder or the Dock:

```bash
launchctl setenv DB_URL "jdbc:mysql://127.0.0.1:3306/payrollsystem_db"
launchctl setenv DB_USER "motorph_app"
launchctl setenv DB_PASS "<your_local_database_password>"
```

After changing these values, **fully quit and reopen NetBeans**.

Verify the non-sensitive values:

```bash
launchctl getenv DB_URL
launchctl getenv DB_USER
```

Verify that the password exists without printing it:

```bash
if [ -n "$(launchctl getenv DB_PASS)" ]; then
    echo "DB_PASS is set"
else
    echo "DB_PASS is NOT set"
fi
```

For Maven commands executed from an existing Terminal session:

```bash
export DB_URL="$(launchctl getenv DB_URL)"
export DB_USER="$(launchctl getenv DB_USER)"
export DB_PASS="$(launchctl getenv DB_PASS)"
```

Do not print the contents of `DB_PASS`.

> `launchctl setenv` is appropriate for the current local academic-development workflow. Values may need to be configured again after logout/restart depending on the local system.

---

## Windows

Configure the following **User Environment Variables**:

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/payrollsystem_db
DB_USER=motorph_app
DB_PASS=<your_local_database_password>
```

One approach is:

1. Search Windows for **Edit environment variables for your account**.
2. Under **User variables**, create `DB_URL`, `DB_USER`, and `DB_PASS`.
3. Save the changes.
4. Fully close and reopen NetBeans.

PowerShell can also be used:

```powershell
[System.Environment]::SetEnvironmentVariable(
    "DB_URL",
    "jdbc:mysql://127.0.0.1:3306/payrollsystem_db",
    "User"
)

[System.Environment]::SetEnvironmentVariable(
    "DB_USER",
    "motorph_app",
    "User"
)

[System.Environment]::SetEnvironmentVariable(
    "DB_PASS",
    "<your_local_database_password>",
    "User"
)
```

The corresponding MySQL server configuration must also restrict the listener appropriately and require secure transport.

---

## Linux / Other Unix-like Systems

For the active shell:

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/payrollsystem_db"
export DB_USER="motorph_app"
export DB_PASS="<your_local_database_password>"
```

Launch NetBeans from an environment that inherits these variables.

For persistent configuration, use the operating system's preferred secure environment or credential-management mechanism rather than storing real credentials in repository files.

---

## Optional JVM System Properties

`DatabaseConnection` also accepts equivalent JVM system properties:

```text
db.url
db.user
db.pass
```

These may be used when an IDE, test environment, or deployment environment supplies JVM properties directly.

The same centralized TLS policy remains enforced.

---

# 🔐 DatabaseConnection Security Behavior

The current `DatabaseConnection` implementation:

1. Requires an explicitly configured database URL.
2. Requires an explicitly configured database username.
3. Requires an explicitly configured database password.
4. Rejects missing or blank database configuration.
5. Validates that the configured URL is a MySQL JDBC URL.
6. Rejects JDBC URLs containing conflicting TLS overrides.
7. Centrally passes:

```text
sslMode=REQUIRED
```

to MySQL Connector/J.
8. Establishes connections using the configured runtime identity rather than a hardcoded administrative account.

There is no operational username/password embedded in Java source code.

---

# 🔐 Authentication Security Behavior

## BCrypt Password Verification

Passwords are stored as BCrypt hashes and verified within the Java application rather than through SQL-level plaintext password comparison.

## Centralized Strong Password Policy

Password creation and updates must satisfy the shared backend policy:

- Minimum **15 characters**
- At least one lowercase character
- At least one uppercase character
- At least one numeric character
- At least one special character

The policy is enforced at the service layer so direct backend invocation cannot bypass Swing validation.

## Timed Account Lockout

- Maximum consecutive failed attempts: **5**
- Lockout duration: approximately **15 minutes**
- Correct credentials remain blocked during an active lockout
- Successful authentication after expiry resets:
  - `failed_attempts`
  - `locked_until`

---

# 🛂 Backend Authorization Security

Role-based access control is enforced through the backend rather than relying only on role-specific Swing screens.

Security behavior includes:

- Authenticated-session validation
- Explicit backend permission checks
- Role-to-permission mappings
- Resource-ownership enforcement
- Self-service access restrictions
- Protected service operations checked before DAO access
- Unknown roles fail closed
- Authorization-denial events are audited

Relevant components include:

```text
AuthorizationService
Permission
SessionManager
UserService
EmployeeService
AttendanceService
LeaveService
OvertimeService
PayslipService
```

---

# ✅ Service-Layer Validation and SQL Safety

Security-relevant business validation is enforced before persistence.

Current protections include:

- Leave-request validation
- Overtime-request validation
- Backend checks independent of Swing UI validation
- Rejection of invalid direct service-layer requests
- JDBC parameterization using `PreparedStatement`
- SQL-injection regression testing

The DAO layer must continue to avoid dynamic SQL concatenation with untrusted values.

---

# 🧾 Structured Audit Logging

Security-relevant actions are recorded through the centralized `AuditLogger`.

Audit records are persisted to:

```text
payrollsystem_db.audit_log
```

and:

```text
logs/motorph-audit.log
```

The local audit directory is excluded from Git.

Current security-relevant event coverage includes authentication, authorization denials, account administration, password changes, Leave decisions, and Overtime decisions.

Representative actions include:

```text
LOGIN_SUCCESS
LOGIN_FAILED
ACCOUNT_LOCKED
LOGIN_BLOCKED_LOCKED
AUTHORIZATION_DENIED

USER_ACCOUNT_CREATED
USER_ACCOUNT_UPDATED
USER_ACCOUNT_STATUS_CHANGED
USER_ACCOUNT_DELETED
PASSWORD_CHANGED

LEAVE_APPROVED
LEAVE_REJECTED
OVERTIME_APPROVED
OVERTIME_REJECTED
```

Audit entries are designed to retain sufficient actor/action/target/result information while avoiding disclosure of:

```text
plaintext passwords
BCrypt hashes
database passwords
other credential material
```

---

# 🔒 Database Security Behavior

Control 5 adds defense-in-depth protection around database access.

## Dedicated Application Identity

Normal application execution uses:

```text
motorph_app
```

rather than the administrative MySQL `root` account.

## Least Privilege

The application account is limited to:

```text
SELECT
INSERT
UPDATE
DELETE
```

on:

```text
payrollsystem_db.*
```

It does not require administrative database privileges for normal MotorPH operation.

## Restricted Network Exposure

The verified local MySQL listener is restricted to:

```text
127.0.0.1:3306
```

## Mandatory Secure Transport

Server configuration requires encrypted transport:

```text
require_secure_transport=ON
```

The application account independently requires SSL/TLS, and Connector/J also enforces:

```text
sslMode=REQUIRED
```

This provides multiple enforcement layers and prevents silent fallback to a plaintext JDBC connection.

---

# 🧪 Testing

The project uses **JUnit 5** for functional and security-assurance testing. Control 6 adds a repeatable dedicated security-regression profile and expands test coverage for the final SonarQube quality gate.

## Current Security-Focused Test Classes

```text
SecurityPasswordUtilTest
SecurityPasswordPolicyServiceTest
SecurityLoginServiceLockoutTest
SecurityBackendRBACTest
SecurityServiceLayerValidationTest
SecurityAuditLoggerTest
SecurityAuditTraceabilityTest
SecurityDatabaseHardeningTest
SecurityCoverageExpansionTest
SecurityUserServiceCoverageTest
SecurityLoginServiceReadCoverageTest
SecurityLeaveOvertimeCoverageTest
SecurityHomePageCoverageTest
SecurityHomePageActionCoverageTest
SecurityLoginPageCoverageTest
SecurityAuthorizationBranchCoverageTest
SecurityAttendanceServiceCoverageTest
SecurityFinalCoverageThresholdTest
```

## Dedicated Security Regression Suite

Run:

```bash
mvn -Psecurity-regression test
```

The dedicated profile executes the `Security*Test` classes independently of the broader GUI/application regression suite.

Final Control 6 result:

```text
Tests run: 158
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The security regression suite covers:

- password hashing and centralized password-policy enforcement;
- authentication failure counting and timed lockout;
- backend RBAC, permissions, ownership, and fail-closed role handling;
- service-layer validation and SQL-safety regression protection;
- structured audit logging and traceability;
- hardened database identity, privilege, listener, and TLS verification;
- attendance/service authorization and exception-handling paths; and
- targeted security-relevant branch coverage used by the final SonarQube quality gate.

## Full Regression Suite

For the full application test suite:

```bash
mvn clean test
```

For full verification with JaCoCo report generation while intentionally skipping a repeated Dependency-Check run:

```bash
mvn clean verify -Ddependency-check.skip=true
```

Final Control 6 regression result:

```text
Tests run: 310
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

This confirms that the completed Milestone 2 security controls and final coverage expansion did not introduce detected regressions into the existing MotorPH application test suite.

## JaCoCo Coverage

Maven `verify` generates:

```text
target/site/jacoco/index.html
target/site/jacoco/jacoco.xml
target/site/jacoco/jacoco.csv
```

SonarQube imports:

```text
target/site/jacoco/jacoco.xml
```

The final SonarQube **New Code** coverage result is:

```text
80.03%
Required: >= 80.0%
Quality Gate: PASSED
```

The SonarQube **overall-code** coverage value is lower because the project contains substantial legacy and NetBeans-generated Swing UI code. The release gate is evaluated on the configured SonarQube new-code conditions.

## OWASP Dependency-Check

OWASP Dependency-Check 13.0.0 is integrated into the Maven workflow.

Control 6 dependency remediation reduced the initial result from:

```text
5 vulnerable dependencies
16 vulnerabilities
```

to:

```text
1 vulnerable dependency
1 vulnerability
```

before application of the narrowly scoped documented JasperReports exception.

The final automated gate report records:

```text
Vulnerable Dependencies: 0
Vulnerabilities Found: 0
Vulnerabilities Suppressed: 1
BUILD SUCCESS
```

The suppression is traceable to the temporary exception in:

```text
docs/security/DEPENDENCY-EXCEPTIONS.md
```

The exception is not a claim that the underlying JasperReports issue is fixed.

## SonarQube Cloud Verification

After generating the JaCoCo XML report, run the SonarQube analysis from the supported `main` branch:

```bash
mvn sonar:sonar
```

Final Control 6 SonarQube results:

```text
Quality Gate: PASSED
New-code coverage: 80.03%
New-code duplication: 0.18%
Security rating: A
Security issues: 0
Security Hotspots: 0
Security review rating: A
```

SonarQube separately reports six remaining dependency risks associated with JasperReports and transitive JFreeChart components. They are retained as documented dependency technical debt and are not represented as remediated.

## Gitleaks Secret Scanning

The final Control 6 secret scans cover both the current repository contents and Git history.

Working tree:

```bash
gitleaks dir . \
  --redact \
  --report-format json \
  --report-path docs/security/evidence/gitleaks-working-tree-report.json
```

Git history:

```bash
gitleaks git . \
  --redact \
  --report-format json \
  --report-path docs/security/evidence/gitleaks-history-report.json
```

Final result:

```text
Working tree: no leaks found
Git history: no leaks found
```

---

## Manual Database Security Verification

### Verify Secure Connection

```bash
mysql \
  --protocol=TCP \
  -h 127.0.0.1 \
  -P 3306 \
  -u motorph_app \
  -p \
  --ssl-mode=REQUIRED \
  payrollsystem_db
```

Then:

```sql
SELECT
    USER() AS login_identity,
    CURRENT_USER() AS privilege_identity;

SHOW SESSION STATUS LIKE 'Ssl_cipher';

SHOW GRANTS;
```

The secure session should show:

- `motorph_app` as the application identity
- A non-empty TLS cipher
- Only expected application grants

### Verify Plaintext Connection Rejection

A controlled negative test may be performed using:

```bash
mysql \
  --protocol=TCP \
  -h 127.0.0.1 \
  -P 3306 \
  -u motorph_app \
  -p \
  --ssl-mode=DISABLED \
  payrollsystem_db
```

The connection should be rejected.

---

## Manual Test Considerations

Some application tests include GUI interactions or PDF generation. During full test execution:

- Modal dialogs may appear.
- Generated PDFs may open automatically.
- A local MySQL database connection may be required.
- Database environment variables must be configured before DB-dependent tests run.
- MySQL must be running using the hardened configuration.

Close blocking dialogs or generated PDF viewers as necessary while monitoring test execution.

---

# 🏗️ Project Layout

```text
src/main/java/
├── db/             Database connection management and TLS enforcement
├── pojo/           Plain Java objects
├── dao/            DAO interfaces
├── daoimpl/        JDBC / PreparedStatement implementations
├── service/        Authentication, authorization, validation, payroll, and business logic
├── ui/             Java Swing application screens
├── ui/base/        Shared Swing templates and form helpers
└── util/           Password, session, authorization, validation, and audit utilities

src/main/resources/
├── log4j2.xml      Log4j2 audit-file configuration
├── background/     Application UI resources
└── reports/        JasperReports templates/resources

src/test/java/
└── test/           JUnit 5 application and security-assurance tests
```

---

# 🛠️ Development & Security Notes

- **Database credentials:** never hardcode operational database credentials in Java source.
- **Database identity:** normal application execution should use `motorph_app`, not `root`.
- **Database privileges:** do not expand the application account beyond required DML privileges without documented justification.
- **Database TLS:** do not weaken or bypass the centralized `sslMode=REQUIRED` policy.
- **Database URL:** do not append TLS-overriding parameters to `DB_URL`.
- **Network exposure:** local MySQL should remain restricted to loopback unless a formally reviewed deployment requirement changes the architecture.
- **Password storage:** passwords must pass through the BCrypt utility.
- **Password policy:** password changes must remain protected by centralized backend validation.
- **Authorization:** protected business actions must remain authorized at the service/backend layer.
- **Validation:** security-relevant rules must not rely exclusively on Swing UI validation.
- **Audit events:** never log plaintext passwords, BCrypt hashes, or database credentials.
- **SQL access:** continue using `PreparedStatement` and parameter binding.
- **Generated logs:** `/logs/` is intentionally ignored by Git.
- **Environment files:** `.env` and `.env.*` local files are intentionally ignored by Git.
- **Before committing:** inspect the Git diff and verify that no real credential or local secret is staged.

---

# 📊 Security Assurance Tooling

The completed Control 6 assurance workflow uses:

```text
JUnit 5
Maven
JaCoCo 0.8.13
OWASP Dependency-Check 13.0.0
Gitleaks
SonarQube Cloud
Git / GitHub
```

The final security-quality-gate record is maintained in:

```text
docs/security/SECURITY-QUALITY-GATE.md
```

The temporary JasperReports dependency exception is maintained in:

```text
docs/security/DEPENDENCY-EXCEPTIONS.md
```

Final retained assurance evidence demonstrates:

- repeatable functional and security regression testing;
- JaCoCo XML coverage imported into SonarQube;
- Maven-integrated dependency scanning and documented exception handling;
- clean Gitleaks working-tree and Git-history scans;
- removal of production stack-trace output;
- successful SonarQube main-branch analysis;
- **SonarQube Quality Gate: Passed**;
- **Security rating: A** with **0 security issues** and **0 Security Hotspots**; and
- **80.03% new-code coverage**, satisfying the configured `>= 80%` threshold.

Remaining overall reliability findings and Sonar dependency risks are retained as documented technical debt rather than being represented as remediated.

---

# 📚 Documentation & Assets

- 🔐 **Final Control 6 Security Quality Gate:**
  `docs/security/SECURITY-QUALITY-GATE.md`

- ⚠️ **Dependency Security Exception Register:**
  `docs/security/DEPENDENCY-EXCEPTIONS.md`

- 📄 **Expanded System Design Documentation:**
  <https://docs.google.com/spreadsheets/d/1O2_Qsl-e7WOu_GajDM0SVfFg9hynrvFhv8UL3yjqZXo>

- ✅ **Testing Documentation:**
  <https://docs.google.com/spreadsheets/d/1CtGctWMrtfvwRpn_sDlLcfvddDmkd6QHmhEyFHldKI8>

- 💾 **Current MotorPH Payroll System SQL Folder:**
  <https://drive.google.com/drive/folders/1SoM4fx7P42JrqvOrqJZqKarLgRVgy571?usp=sharing>

---

# 🤝 Contributing

1. Work from the appropriate development or feature branch.
2. Keep security-critical business logic in the service/backend layer rather than relying only on the UI.
3. Do not commit database passwords, local credentials, generated logs, `.env` files, or other secrets.
4. Preserve centralized authentication, authorization, validation, audit, and TLS enforcement.
5. Run the relevant dedicated security tests after security-sensitive changes.
6. Run the complete regression suite before committing or opening a pull request.
7. Review `git diff` and staged files before every security-control commit.
8. Use pull-request review before merging completed IAS2 changes into the protected baseline branch.

---

# 📝 License

© 2026 MotorPH IAS2 S4101 Group 8. All rights reserved.

This repository is maintained for academic use, controlled testing, and project evaluation.

---

Made with ☕, deadlines, and secure-by-design improvements.
