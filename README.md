# 🔐 Simault®® (Simple Vault): A Simple Secrets Manager

Simault®® is a secure, lightweight Vault service built with **Java 21** and **Spring Boot 3**. It provides ephemeral (short-lived) secrets for microservices using **MongoDB Client-Side Field Level Encryption (CSFLE)**.

## 🚀 Features

* **Zero-Knowledge Database**: Secrets are encrypted *before* they leave the application. The database only stores binary ciphertext (`Type 6` Binary).
* **Auto-Expiration (TTL)**: Secrets automatically expire and are deleted after **1 hour**.
* **Lazy Rotation**: If a secret is expired or missing, a new one is automatically generated and encrypted on the next request.
* **Whitelist Security**: Only applications explicitly registered via the Admin API can request secrets.
* **Secure Admin API**: Administrative actions are protected by a configurable API Key.
* **API-First Design**: No hardcoded application lists; everything is managed dynamically via REST endpoints.

---

## 🛠️ Technology Stack

* **Language**: Java 21
* **Framework**: Spring Boot 3.2.1
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

# Database
vault.mongodb.uri=mongodb://localhost:27017
vault.mongodb.database=encryption_vault
vault.mongodb.collection=secrets

# Encryption Keys (Local KMS)
vault.security.kms-provider=local
vault.security.master-key-path=master-key.txt
vault.security.key-alt-name=vault-key-1

# Security
vault.admin.api-key=super-secret-admin-password-123
```

### 2. Generate Master Key
The system requires a 96-byte local master key to unlock the vault. Run this command in the project root:

```properties
openssl rand -out master-key.txt 96
```

### 3. Build & Run
Clean the build to ensure MongoDB driver compatibility, then start the server.

```properties
mvn clean install
mvn spring-boot:run
```
You should see: ✅ SecretVaultService Ready


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
        │   ├── config/            <-- Mongo & Encryption Config
        │   ├── controller/        <-- AdminController & SecretController
        │   ├── model/             <-- Java Records (AllowedApp, AppSecret)
        │   ├── repository/        <-- MongoDB Repositories
        │   ├── service/           <-- Core Business Logic
        │   └── SimaultApplication.java
        └── resources/


### ⚠️ Troubleshooting
403 Forbidden: The application is not registered. Use the Admin API to register it first.

500 Internal Error: Usually means master-key.txt is missing from the running directory.

ClassCastException: Ensure you ran mvn clean install to fix dependency conflicts between MongoDB Driver versions.