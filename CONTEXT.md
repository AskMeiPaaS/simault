# 📘 Project Context: Simault

**Simault** (Simple Vault) is a lightweight, AI-native secret management service built with **Java 21** and **Spring Boot**. It is designed to act as a **Model Context Protocol (MCP) Server**, bridging the gap between autonomous AI agents and secure database operations.

## 🛠️ Technology Stack

* **Language:** Java 21
* **Framework:** Spring Boot 3.5.9
* **Build Tool:** Maven
* **AI Framework:** Spring AI (Version `1.0.0-M6`)
* **Database:** MongoDB (Driver 5.5.1)

## 📦 Key Dependencies & Architecture

### 1. AI & Agent Protocols
* **Dependency:** `spring-ai-mcp-server-webmvc-spring-boot-starter`
* **Purpose:** This dependency establishes Simault as a **Model Context Protocol (MCP) Server** over WebMVC. It allows the application to expose its tools (Admin Registry, Secret Vault) to AI clients (like Claude Desktop or IDEs) using standard MCP transport.

### 2. Database & Encryption
* **Dependency:** `mongodb-driver-sync`, `mongodb-crypt`
* **Purpose:**
    * Provides synchronous interaction with MongoDB.
    * **`mongodb-crypt`** indicates the use of **Client-Side Field Level Encryption (CSFLE)**. Secrets are encrypted in the application layer before being sent to the database, ensuring a "Zero-Knowledge" storage architecture.

### 3. Security & Logging
* **Dependency:** `spring-boot-starter-log4j2`
* **Configuration:** The default `spring-boot-starter-logging` (Logback) is explicitly **excluded** from all major starters (`web`, `test`, `spring-ai`).
* **Purpose:** This enforces the use of **Log4j 2** for all logging. This is critical for the project's **Security Redaction** strategy, which uses Log4j 2's Regex Replacement to scrub sensitive tokens (e.g., `Bearer eyJ...`, `key":"...`) from logs before they are written.

### 4. Web & API
* **Dependency:** `spring-boot-starter-web`
* **Purpose:** Exposes the REST API endpoints for:
    * **Admin Actions:** `/api/admin/*` (App Registry, Key Management)
    * **Client Actions:** `/api/secrets/*` (Retrieval, Rotation)
    * **A2A Discovery:** `/.well-known/agent-card` (Agent-to-Agent Protocol)

## 🧪 Testing
* **Dependencies:** `spring-boot-starter-test`, `junit-jupiter`
* **Scope:** Includes integration tests (`@SpringBootTest`) and web layer tests (`@WebMvcTest`) to verify security controls and API contracts.

## 🚀 Runtime Environment
* **Main Class:** `com.ayedata.simault.SimaultApplication`
* **Repository:** `spring-milestones` (Required for Spring AI M6 builds)