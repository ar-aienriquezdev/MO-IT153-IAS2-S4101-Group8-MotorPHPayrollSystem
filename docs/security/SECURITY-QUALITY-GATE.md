# Security Quality Gate

**Project:** MotorPH Payroll System
**Course / Milestone:** MO-IT153 Information Assurance and Security 2 — Milestone 2
**Control:** Control 6 — Secure Development, Security Testing, and Automated Quality Assurance
**Gate Record:** C6-QG-001
**Date:** 2026-10-17
**Current Status:** **PASS WITH DOCUMENTED RESIDUAL DEPENDENCY RISKS**

---

## 1. Purpose

This record consolidates the final objective security-assurance evidence for Control 6 of the MotorPH Payroll System.

The quality gate evaluates functional regression, dedicated security regression, dependency security, repository secret exposure, release/debug behavior, code-coverage generation, and SonarQube Cloud static/security analysis. Findings that cannot be safely remediated within the milestone are retained explicitly as exceptions or technical debt rather than being hidden or represented as fixed.

---

## 2. Final Quality-Gate Results

| Security Assurance Check | Acceptance Criterion | Final Result | Status |
|---|---|---|---|
| Full regression suite | All project tests complete with no failures or errors | **310 tests; 0 failures; 0 errors; 0 skipped** | **PASS** |
| Dedicated security regression suite | Security-critical controls execute independently and pass | **158 tests; 0 failures; 0 errors; 0 skipped** | **PASS** |
| JaCoCo coverage generation | Maven produces coverage artifacts for SonarQube import | `index.html`, `jacoco.xml`, and `jacoco.csv` generated under `target/site/jacoco/` | **PASS** |
| OWASP Dependency-Check integration | Dependency scan runs repeatably through Maven | Dependency-Check **13.0.0** integrated with a CVSS `>= 7.0` release threshold | **PASS** |
| Dependency remediation | High-impact findings are remediated or explicitly dispositioned | Initial **5 vulnerable dependencies / 16 vulnerabilities** reduced to **1 / 1** before the documented JasperReports exception | **PASS WITH EXCEPTION** |
| Final OWASP dependency gate | No unsuppressed Dependency-Check vulnerability remains in the automated gate | **0 vulnerable dependencies; 0 vulnerabilities found; 1 vulnerability suppressed** under `SEC-DEP-001`; build successful | **PASS WITH DOCUMENTED EXCEPTION** |
| Repository secret scan — working tree | No detected repository secrets | Gitleaks final working-tree scan: **no leaks found** | **PASS** |
| Repository secret scan — Git history | No detected repository secrets | Gitleaks final history scan: **no leaks found** | **PASS** |
| Release/debug behavior | Production stack-trace/debug behavior is removed or gated | Production `printStackTrace()` calls removed; centralized `ErrorLogger` used; intentional console output remains only in the controlled migration utility | **PASS** |
| SonarQube main-branch analysis | Analysis completes successfully from `main` | SonarScanner analysis completed successfully and uploaded results | **PASS** |
| SonarQube Quality Gate | Configured quality conditions pass | **Quality Gate: Passed — all conditions passed** | **PASS** |
| SonarQube new-code coverage | New-code coverage is at least 80% | **80.03%** on 837 new lines to cover | **PASS** |
| SonarQube new-code duplication | New-code duplication is at most 3% | **0.18%** | **PASS** |
| SonarQube code security | No release-blocking code-security findings | **Security rating A; 0 security issues; 0 Security Hotspots; Security review rating A** | **PASS** |
| Remaining overall reliability debt | Residual non-blocking findings are documented | **9 Medium reliability findings; overall Reliability C** in shared/legacy Swing code | **DOCUMENTED TECHNICAL DEBT** |
| SonarQube dependency risks | Residual third-party dependency risks are reviewed and retained | **6 risks**: 5 Medium and 1 Low by Sonar risk severity, associated with JasperReports 6.21.5 and transitive JFreeChart 1.0.19 | **DOCUMENTED RESIDUAL RISK** |

---

## 3. Dependency Security Result

### 3.1 Initial Control 6 scan

The initial OWASP Dependency-Check run reported:

- Vulnerable dependencies: **5**
- Vulnerabilities found: **16**
- Release behavior: **build failed at CVSS >= 7.0 as designed**

### 3.2 Dependency remediation

The project remediated the initial dependency set by upgrading:

- JasperReports: `6.21.3` -> `6.21.5`
- Commons BeanUtils: `1.9.4` -> `1.11.0` transitively
- Jackson: `2.15.3` -> `2.18.11`
- Log4j: `2.23.1` -> `2.25.5`

The post-remediation unsuppressed scan was reduced to:

- Vulnerable dependencies: **1**
- Vulnerabilities found: **1**
- Remaining OWASP finding: `CVE-2025-10492`
- Affected component: `net.sf.jasperreports:jasperreports:6.21.5`

The remaining OWASP finding is not represented as fixed. It is retained under the temporary exception `SEC-DEP-001` in `DEPENDENCY-EXCEPTIONS.md`.

### 3.3 Final automated OWASP gate

The narrowly scoped suppression tied to `SEC-DEP-001` allows the repeatable automated gate to proceed without hiding unrelated findings.

Final automated report:

```text
Vulnerable Dependencies: 0
Vulnerabilities Found: 0
Vulnerabilities Suppressed: 1
BUILD SUCCESS
```

The original pre-suppression evidence remains retained to show the underlying JasperReports finding.

### 3.4 SonarQube dependency-risk review

SonarQube SCA separately reports **6 remaining dependency risks**. Sonar risk severity is distinct from the underlying CVSS severity.

| Vulnerability | Component | Dependency Type | Sonar Risk Severity | CVSS shown by Sonar |
|---|---|---|---|---|
| `CVE-2025-10492` | JasperReports 6.21.5 | Direct | Medium | 9.8 Critical |
| `CVE-2024-22949` | JFreeChart 1.0.19 | Transitive | Medium | 9.1 Critical |
| `CVE-2026-8009` | JasperReports 6.21.5 | Direct | Medium | 8.7 High |
| `CVE-2024-23076` | JFreeChart 1.0.19 | Transitive | Medium | 7.5 High |
| `CVE-2024-23077` | JFreeChart 1.0.19 | Transitive | Medium | 7.5 High |
| `CVE-2023-52070` | JFreeChart 1.0.19 | Transitive | Low | 8.4 High |

These Sonar SCA risks are **not** code-security issues and are not configured as blocking conditions in the current Sonar quality gate. They are therefore retained as visible third-party dependency technical debt for future JasperReports/JFreeChart migration or vendor-supported remediation.

`SEC-DEP-001` applies specifically to the existing OWASP JasperReports exception and must not be interpreted as silently suppressing or accepting every Sonar dependency-risk item.

---

## 4. Repository Secret-Scanning Result

Gitleaks was executed against both the current repository contents and Git history.

Final results:

- Working-tree scan: **no leaks found**
- Git-history scan: **17 commits scanned; no leaks found**

The redacted JSON reports are retained under:

```text
docs/security/evidence/
```

This result supports the Control 6 requirement that operational secrets are not committed to the repository.

---

## 5. Release / Debug Behavior Review

Production source was reviewed for:

- `printStackTrace()`
- `System.out`
- `System.err`
- DEBUG/debug flags
- password, token, API-key, secret, and credential-related patterns

All production `printStackTrace()` calls identified during Control 6 were removed and replaced with centralized Log4j2 error logging through `ErrorLogger`.

The only retained direct console output is within `PasswordBulkMigrate`, a dedicated command-line migration utility where console status/error feedback is intentional.

No repository secret was detected by the final Gitleaks working-tree or Git-history scans.

---

## 6. Regression Testing and Coverage

### 6.1 Dedicated security regression

The Maven `security-regression` profile executes `Security*Test.java` classes covering:

- BCrypt/password-policy behavior;
- authentication lockout and read paths;
- backend RBAC, ownership, and fail-closed authorization;
- service-layer validation and SQL-safety regression protection;
- structured audit logging and traceability;
- database least privilege, listener restriction, and mandatory TLS;
- Leave and Overtime security-relevant workflows;
- UserService, LoginService, AttendanceService, and authorization branch coverage;
- security-relevant Home/Login Swing execution paths; and
- final targeted coverage-threshold branches.

Final dedicated result:

> **158 tests run, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**

### 6.2 Full application regression

Final full regression result:

> **310 tests run, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**

### 6.3 JaCoCo and SonarQube coverage

Maven verification generates:

```text
target/site/jacoco/index.html
target/site/jacoco/jacoco.xml
target/site/jacoco/jacoco.csv
```

SonarQube imports the XML report from:

```text
target/site/jacoco/jacoco.xml
```

Final SonarQube new-code coverage:

> **80.03% — PASS against the configured >= 80.0% threshold**

The SonarQube overall-code coverage value is **50.1%**. This is not the quality-gate coverage criterion; the configured Sonar Way gate evaluates the new-code condition shown above.

---

## 7. Final SonarQube Main-Branch Verification

The final analysis was executed against the supported `main` branch and completed successfully.

Final reviewed results:

- **Quality Gate:** Passed
- **New-code coverage:** 80.03%
- **New-code duplication:** 0.18%
- **Security rating:** A
- **Security issues:** 0
- **Security Hotspots:** 0
- **Security review rating:** A
- **Overall-code coverage:** 50.1%
- **Overall reliability:** C with 9 Medium findings
- **Maintainability rating:** A

The remaining reliability findings are concentrated in shared/legacy Swing classes and are retained as non-blocking technical debt. They do not change the final Control 6 security decision because the configured main-branch quality gate passed and the new-code security/coverage conditions are satisfied.

---

## 8. Evidence Register

Final Control 6 evidence includes:

- `C6-E01a_SonarQube-Final-Quality-Gate-Passed.png`
- `C6-E01b_SonarQube-Final-Quality-Gate-Passed-Detailed.png`
- `C6-E01c_SonarQube-Final-Security-Rating-A.png`
- `C6-E02_Final-OWASP-Gate-With-Documented-Exception.png`
- `C6-E03a_Final-Gitleaks-Working-Tree-Scan.png`
- `C6-E03b_Final-Gitleaks-History-Scan.png`
- `C6-E04_SonarQube-JaCoCo-Main-Branch-Analysis.png`
- `C6-E05_SonarQube-Remaining-Dependency-Risks.png`
- `DEPENDENCY-EXCEPTIONS.md`
- Gitleaks JSON reports under `docs/security/evidence/`
- retained earlier OWASP, JaCoCo, regression, and pre-remediation evidence showing the Control 6 before/after trail

---

## 9. Final Gate Decision

**Decision:** **PASS WITH DOCUMENTED RESIDUAL DEPENDENCY RISKS**

Control 6 is complete for Milestone 2. The final evidence demonstrates repeatable functional and security regression testing, JaCoCo coverage generation, dependency remediation and exception handling, repository secret scanning, release/debug cleanup, successful SonarQube analysis on `main`, Security rating A with zero code-security issues and zero Security Hotspots, and a passed SonarQube Quality Gate with 80.03% new-code coverage.

The remaining Sonar dependency risks and nine Medium overall-reliability findings are retained transparently as follow-up technical debt. They are not represented as remediated and do not block the configured final quality gate.

**Gate Record:** `C6-QG-001`
**Final Disposition:** **Passed — Control 6 complete with documented third-party dependency residual risks.**
