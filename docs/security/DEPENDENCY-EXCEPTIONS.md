# Dependency Security Exception Register

**Project:** MotorPH Payroll System  
**Course / Milestone:** MO-IT153 Information Assurance and Security 2 — Milestone 2  
**Control:** Control 6 — Secure Development, Security Testing, and Automated Quality Assurance  
**Exception ID:** SEC-DEP-001  
**Status:** Temporary Accepted Residual Risk  
**Date Recorded:** 2026-10-18  
**Owner:** Group 8  

---

## 1. Affected Dependency

| Field | Value |
|---|---|
| Component | `net.sf.jasperreports:jasperreports` |
| Current Version | `6.21.5` |
| Vulnerability | `CVE-2025-10492` |
| Severity | High |
| Reported CVSS | 8.7 |
| Detection Tool | OWASP Dependency-Check |
| Evidence | `C6-E04b_OWASP-Dependency-Check-Post-Remediation` |

---

## 2. Background

Control 6 introduced OWASP Dependency-Check as a repeatable dependency-security control within the Maven workflow. The initial scan identified **5 vulnerable dependencies and 16 vulnerabilities**.

Dependency remediation was then performed by upgrading:

- JasperReports from `6.21.3` to `6.21.5`;
- Commons BeanUtils transitively from `1.9.4` to `1.11.0`;
- Jackson components from `2.15.3` to `2.18.11`; and
- Log4j from `2.23.1` to `2.25.5`.

The post-remediation scan reduced the result to **1 vulnerable dependency and 1 remaining vulnerability**. The only remaining finding is `CVE-2025-10492` affecting JasperReports `6.21.5`.

This exception therefore applies only to the remaining JasperReports finding and does not cover any other dependency vulnerability.

---

## 3. Reason Immediate Remediation Is Deferred

The remaining finding cannot be addressed through another low-risk patch update within the current JasperReports 6.x line.

Moving to JasperReports 7.x is a major-version migration that may require conversion or revalidation of existing JRXML report templates and may introduce compatibility changes to the report-generation workflow.

The MotorPH Payroll System currently depends on existing JasperReports 6.x report templates for payroll-related document generation. Performing a major report-engine migration immediately, without adequate compatibility and regression testing, could create functional defects in report generation and destabilize an otherwise working system.

For Milestone 2, the risk is therefore documented as a **temporary exception** rather than being hidden or treated as remediated.

---

## 4. Current Exposure Assessment

Based on the current implementation review:

- Report templates are controlled application resources rather than arbitrary templates supplied by end users.
- No user-facing feature for uploading or importing untrusted JRXML report definitions has been identified.
- Report-generation functionality remains inside the authenticated application workflow.
- Backend authorization, service-layer validation, audit logging, and hardened database access remain enforced independently of JasperReports.
- The remaining vulnerability continues to be visible in OWASP Dependency-Check evidence and is not being represented as fixed.

These conditions reduce the practical exposure of the remaining dependency issue in the current controlled project environment, but they do **not** eliminate the vulnerability.

---

## 5. Compensating Controls

Until JasperReports can be migrated and retested, the following controls must remain in place:

1. Only application-controlled report templates may be used.
2. Arbitrary or user-supplied JRXML / Jasper report definitions must not be accepted or executed.
3. Existing authentication and backend authorization controls must remain enforced before protected report-generation functions are accessed.
4. Database access must continue to use the dedicated least-privilege account and mandatory TLS configuration.
5. Security-relevant report or application actions must continue to be covered by the existing audit and logging controls where applicable.
6. OWASP Dependency-Check must continue to scan the dependency in future assurance runs.
7. The finding must remain documented in the Control 6 quality-gate evidence until the migration is completed.

---

## 6. Exception Decision

**Decision:** Temporarily accepted for the current Milestone 2 release evidence, subject to the compensating controls and review conditions in this document.

**Rationale:**  
The project successfully remediated the other dependency findings and reduced the OWASP result from **5 vulnerable dependencies / 16 vulnerabilities** to **1 vulnerable dependency / 1 vulnerability**. The remaining JasperReports issue requires a major-version migration with report-template compatibility implications. Under the current project schedule, immediate migration introduces a material regression risk that should be addressed through a dedicated migration and validation activity rather than an untested emergency upgrade.

This exception is **not a permanent acceptance of the vulnerability**.

---

## 7. Required Follow-Up

The exception must be reassessed when any of the following occurs:

- before production deployment outside the current controlled project environment;
- before the next significant application release;
- when JasperReports is next upgraded;
- when report templates are redesigned or converted;
- if a user-facing report-template upload/import feature is introduced; or
- if new vulnerability information materially changes the risk assessment.

The preferred permanent remediation is to migrate to a non-affected JasperReports release, convert or update the existing report templates as required, run the complete functional and security regression suites, and repeat OWASP Dependency-Check.

---

## 8. Quality-Gate Treatment

This exception is part of the Control 6 security-quality-gate evidence.

The remaining CVE must not be described as remediated. It must be reported as:

> **Open — Temporary documented exception pending JasperReports major-version migration and report-template compatibility validation.**

If a future OWASP Dependency-Check suppression is introduced solely to allow the automated release gate to proceed, it must be narrowly scoped to this specific dependency/CVE and traceable to **SEC-DEP-001**. The suppression must not conceal unrelated findings.

---

## 9. Evidence References

- `C6-E04a_OWASP-Dependency-Check-Initial-Results`
- `C6-E04b_OWASP-Dependency-Check-Post-Remediation`
- Maven dependency-tree evidence showing the remediated dependency versions
- Full regression-test evidence after dependency remediation
- Final Control 6 security-quality-gate evidence

---

**Exception ID:** `SEC-DEP-001`  
**Current Disposition:** Temporary accepted residual risk — remediation deferred to JasperReports major-version migration and compatibility validation.
