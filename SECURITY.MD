# Security Policy for Simault®

Simault takes security seriously. As a Vault and Secret Management service designed for AI Agents, we implement multiple layers of defense-in-depth, including database encryption, log redaction, and strict access controls.

## 🛡️ Security Architecture & Features

### 1. Zero-Knowledge Architecture
* **Data at Rest:** All secrets are encrypted using **MongoDB Client-Side Field Level Encryption (CSFLE)** before they leave the application memory. The database only ever sees binary ciphertext (`Type 6` Binary).
* **Key Management:** The Master Key is stored locally (`master-key.txt`) and is never committed to version control.
* **Ephemeral Secrets:** Secrets have a strict **TTL (Time-To-Live)** of 1 hour. Expired secrets are automatically purged and rotated.

### 2. Log Redaction (Log4j 2)
We utilize a custom Log4j 2 configuration with **Regex Replacement** to prevent sensitive data leakage in logs.
* **What is Redacted:**
    * `Bearer` tokens in HTTP headers.
    * JSON fields: `key":"..."`, `token":"..."`, `password":"..."`.
    * URL parameters: `token=...`.
* **Scope:** Redaction applies to both Console (stdout) and File logs (`logs/app.log`), including stack traces.

### 3. Agent-to-Agent (A2A) Security
The A2A Discovery Protocol enforces **Role-Based Access Control (RBAC)** at the discovery layer to prevent unauthorized tool usage by autonomous agents.
* **Admin Agents** (`/api/admin/.well-known/agent-card`) can only discover management tools (Register/Remove Apps).
* **Client Agents** (`/api/client/.well-known/agent-card`) can only discover operational tools (Get/Rotate Secrets).
* **Strict Segregation:** Administrative tools are physically hidden from Client agents to prevent privilege escalation.

### 4. Access Control
* **Admin API:** Protected by a configurable API Key (`X-ADMIN-KEY`).
* **Client API:** Protected by a Strict Allowlist (Registry). Only pre-registered `appId`s can request secrets.
* **Minimum Privilege:** Client tools (`getSecret`) never return the raw secret to the logs, only to the calling context.

---

## 🐛 Reporting a Vulnerability

We strongly encourage responsible disclosure of security vulnerabilities.

**1. Private Disclosure**
If you discover a vulnerability, please **DO NOT** open a public issue on GitHub.
* **Email:** security@ayedata.com
* **Subject:** `[VULN] Simault - <Short Description>`

**2. What to Include**
* Step-by-step instructions to reproduce the issue.
* Proof-of-concept (PoC) scripts or `curl` commands.
* Impact assessment (e.g., "Allows unauthorized secret retrieval" or "Bypasses log redaction").

**3. Response Timeline**
* **Acknowledgment:** We aim to acknowledge reports within 48 hours.
* **Assessment:** We will assess the severity and provide an estimated timeline for a fix within 5 business days.
* **Fix & Disclosure:** Once a patch is released, we will credit you (with your permission) in the release notes.

---

## 🚫 Out of Scope

The following are generally considered out of scope for security reports:
* Attacks requiring physical access to the server (e.g., stealing `master-key.txt` from the disk).
* Social engineering attacks against employees.
* Denial of Service (DoS) attacks that do not exploit a code vulnerability.

---

## 📦 Supported Versions

Only the latest major version receives active security updates.

| Version | Supported | Notes |
| :--- | :--- | :--- |
| **1.x** | ✅ | Current Stable Release |
| **0.x** | ❌ | Development / Beta |

---

## ⚠️ Critical Configuration Checks

Before deploying Simault to production, ensure:
1.  **Log Level:** Set `logging.level.root=INFO` (Avoid `DEBUG` in production unless diagnosing a specific issue).
2.  **Master Key Permissions:** Ensure `master-key.txt` has `600` (read-only, owner-only) file permissions.
3.  **API Key Strength:** Change the default `vault.admin.api-key` to a strong, generated string (min 32 chars).
4.  **Transport Security:** Always run Simault behind a reverse proxy (Nginx/Traefik) with **TLS 1.2+ enabled**.