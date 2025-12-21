# 🔐 Simault® (Simple Vault): A Simple Secrets Manager with Model Context Protocol (MCP) Enabled
Simault® is a secure, lightweight Vault service built built for the AI era with **Java 21** and **Spring Boot 3**. It provides ephemeral (short-lived) secrets for microservices using **MongoDB Client-Side Field Level Encryption (CSFLE)** and comes with **Model Context Protocol (MCP)***

## 🚀 Features

* **Zero-Knowledge Database**: Secrets are encrypted *before* they leave the application. The database only stores binary ciphertext (`Type 6` Binary).
* **Auto-Expiration (TTL)**: Secrets automatically expire and are deleted after **1 hour**.
* **Lazy Rotation**: If a secret is expired or missing, a new one is automatically generated and encrypted on the next request.
* **AI-Ready (MCP)**: Native MCP Server implementation allowing tools like **Google Antigravity** or **Claude Desktop** to manage secrets via natural language.
* **Modular Architecture**: MCP tools are strictly separated into Admin, Vault (Ops), and Observability domains for security.
* **Whitelist Security**: Only applications explicitly registered via the Admin API can request secrets.
* **Secure Admin API**: Administrative actions are protected by a configurable API Key.
* **API-First Design**: No hardcoded application lists; everything is managed dynamically via REST endpoints.
* **Highly Resilient Architecture**: Stateless Application + MongoDB Replica Set

---

## 🛠️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.2.1
* **AI Framework**: Spring AI (Milestone 1.0.0-M6)
* **Database**: MongoDB 4.2+ (Community or Enterprise)
* **Security**: MongoDB Client-Side Field Level Encryption (CSFLE)
* **Build Tool**: Maven

---

## ⚙️ Prerequisites

1.  **Java 21 JDK** installed.
2.  **MongoDB** running locally on port `27017`.
3.  **Maven** installed.

---

## 📦 Installation & Setup

### 1. Clone & Configure
Clone the repository and verify the `application.properties` file in the **project root**.

```properties
server.port=8080

# MongoDB Configuration
vault.mongodb.uri=mongodb://localhost:27017
vault.mongodb.database=encryption_vault
vault.mongodb.collection=secrets

# Security (Local KMS)
vault.security.kms-provider=local
vault.security.master-key-path=master-key.txt
vault.security.key-alt-name=simault-key-1

# Admin API Security
vault.admin.api-key=super-secret-admin-password-123

# MCP Server Config
spring.ai.mcp.server.name=simault-mcp-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.type=SYNC
```

### 2. Generate Master Key
The system requires a 96-byte local master key to unlock the vault. Run this command in the project root:

```properties
openssl rand -out master-key.txt 96
```

### 3. Build & Run
Clean the build to ensure MongoDB driver compatibility, then start the server.

```properties
mvn clean install -U
mvn spring-boot:run
```
You should see: ✅ SecretVaultService Ready

### 4. 🤖 Model Context Protocol (MCP)
Simault exposes an MCP Server over Stdio (or SSE), allowing AI Agents to perform tasks securely.

### 5. How to Connect (Google Antigravity)
Google Antigravity has native support for MCP servers. Follow these steps to connect Simault:
Open Antigravity and open the Agent Panel (Right Sidebar).
Click the ... (More Options) menu at the top of the Agent Panel.
Select MCP Servers -> Manage MCP Servers.
Click "View raw config" to open your mcp_config.json file.
Add the Simault configuration to the mcpServers object:
```properties
{
  "mcpServers": {
    "simault": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/simault/target/simault-1.0.0-SNAPSHOT.jar"
      ]
    }
  }
}
```
Save the file and click Refresh in the MCP Servers menu.
Test it: Ask the Agent, "Check the secret health for payment-service" or "List all allowed apps in Simault."

🛡️ Admin API (Management)
All Admin endpoints require the header X-ADMIN-KEY matching the value in application.properties.

## 1. Register an App (Whitelist)
POST /api/admin/apps

```properties

curl -X POST http://localhost:8080/api/admin/apps \
     -H "Content-Type: application/json" \
     -H "X-ADMIN-KEY: super-secret-admin-password-123" \
     -d '{
           "appId": "payment-service", 
           "description": "Payment Processing Module"
         }'
```

## 2. List Allowed Apps
GET /api/admin/apps

```properties

curl -X GET http://localhost:8080/api/admin/apps \
     -H "X-ADMIN-KEY: super-secret-admin-password-123"
```
## 3. Revoke Access
DELETE /api/admin/apps/{appId}

```properties

curl -X DELETE http://localhost:8080/api/admin/apps/payment-service \
     -H "X-ADMIN-KEY: super-secret-admin-password-123"
```

## 4. Search Encryption Keys
GET /api/admin/keys (Optional param: ?altName=...)

```properties

curl -X GET http://localhost:8080/api/admin/keys \
     -H "X-ADMIN-KEY: super-secret-admin-password-123"
```

### 🔐 Client API (Integration)
Microservices use these endpoints to fetch their secrets. No API Key is required, but the appId must be whitelisted.

## 1. Fetch Secret
If the secret exists (and is < 1 hour old), it returns the existing one. If expired or missing, it generates a new one.

GET /api/secrets/{appId}

```properties
curl -X GET http://localhost:8080/api/secrets/payment-service
```
Response:

JSON

{
  "appId": "payment-service",
  "secretValue": "Xy9z...random_secure_string...",
  "createdAt": "2023-12-17T12:00:00Z"
}


## 2. Force Rotation
Forces the generation of a new secret immediately, regardless of expiration time.

POST /api/secrets/{appId}/rotate

```properties
curl -X POST http://localhost:8080/api/secrets/payment-service/rotate
```

### 📂 Project Structure
Plaintext

Simault®/
├── pom.xml
├── master-key.txt                 <-- Generated Security Key
├── application.properties         <-- Config (Root Level)
└── src
    └── main
        ├── java/com/ayedata/simault
           ├── config/            <-- Mongo & Encryption Config
           ├── controller/        <-- AdminController & SecretController
           ├── model/             <-- Java Records (AllowedApp, AppSecret)
           ├── repository/        <-- MongoDB Repositories
           ├── service/           <-- Core Business Logic
           ├── SimaultApplication.java
           ├── mcp/                    <-- MCP Server Implementation
               ├── admin/
               │   └── AdminRegistryTools.java       (Whitelist Mgmt)
               ├── vault/
               │   └── SecretVaultTools.java         (Get/Rotate Secrets)
               └── observability/
                    └── SecretHealthTools.java        (Safe Health Checks)
               
### ⚠️ Troubleshooting
Import org.springframework.ai cannot be resolved:
Ensure <repositories> in pom.xml includes spring-milestones.
```properties
Run mvn clean install -U 
```
to force update.

Duplicate Bean Definition:
Check that you deleted the old SimaultMcpTools.java if you switched to the modular folder structure (mcp/admin, mcp/vault, etc.).

403 Forbidden:
The app is not whitelisted. Use the Admin API or MCP Admin Tool to register it.

500 Internal Error:
Usually means master-key.txt is missing from the running directory.