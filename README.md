# 🔐 jVault (Jugaad Vault)

> **"Military-grade encryption, on a community budget."**

**jVault** is a reference implementation of **MongoDB Client-Side Field Level Encryption (CSFLE)** built with **Java 21**.

It demonstrates how to build a "Zero-Trust" data vault using **MongoDB Community Edition** and **Explicit Encryption**. Unlike enterprise solutions that require expensive Cloud KMS (AWS/Azure/GCP) providers, jVault uses a "Jugaad" approach—managing the Master Key locally on the filesystem—to achieve the same cryptographic standard for free.

![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![MongoDB](https://img.shields.io/badge/MongoDB-Community-green?style=for-the-badge&logo=mongodb)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

---

## 🏗️ Architecture

jVault uses **Envelope Encryption**.

1.  **The Master Key:** A 96-byte file (`master-key.txt`) residing on your application server.
2.  **The Data Key:** Stored in MongoDB (`encryption.__keyVault`), but encrypted by the Master Key.
3.  **The Payload:** The actual secret data, encrypted by the Data Key before it ever leaves the Java application.

---

## 📋 Prerequisites

* **Java Development Kit (JDK) 21** or higher.
* **MongoDB Community Edition** (running locally on port `27017`).
* **Maven** (for dependency management).

---

## 🚀 Getting Started

### 1. Clone & Build
```bash
git clone [https://github.com/yourusername/jvault.git](https://github.com/yourusername/jvault.git)
cd jvault
mvn clean install

1a. Logical Project Structure

jvault/
├── pom.xml
├── master-key.txt
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── ayedata
    │   │           └── jvault
    │   │               ├── JVaultApplication.java       <-- Main Entry
    │   │               ├── config                       <-- Setup (Mongo & Encryption)
    │   │               │   ├── EncryptionConfig.java
    │   │               │   └── MongoConfig.java
    │   │               ├── controller                   <-- REST API Layer
    │   │               │   └── SecretController.java
    │   │               ├── model                        <-- Data Structures
    │   │               │   └── AppSecret.java
    │   │               ├── repository                   <-- DB & Crypto Access
    │   │               │   └── SecretRepository.java
    │   │               ├── service                      <-- Business Logic (Rotation)
    │   │               │   └── SecretService.java
    │   │               └── util                         <-- Helpers
    │   │                   └── KeyUtils.java
    │   └── resources
    │       └── application.properties
    └── test

2. The "Jugaad" Setup (Key Generation)
First, we must forge the physical key. This generates a secure 96-byte file on your local disk.

Bash

# Run the Key Generator
mvn exec:java -Dexec.mainClass="com.ayedata.jvault.tools.JVaultKeyGen"
Output: ✅ 'master-key.txt' created using Java NIO.

3. Initialize the Vault
Now, we connect to MongoDB and create the __keyVault collection. We also generate a unique Data Encryption Key (DEK) and store it (encrypted) in the DB.

Bash

# Run the Setup Script
mvn exec:java -Dexec.mainClass="com.ayedata.jvault.tools.JVaultSetup"
Output: ✅ jVault is ready for deposits.

4. Run the Application
Enter the interactive CLI vault.

Bash

# Launch jVault
mvn exec:java -Dexec.mainClass="com.ayedata.jvault.app.JVaultApp"
🎮 Usage Guide
Once the app is running, you will see the interactive menu:

Plaintext

🔐 Starting jVault Service...

1. Deposit Secret
2. Withdraw Secret
3. Peek Raw DB
4. Exit
> 
1. Deposit Secret
Enter an Owner (e.g., "admin") and a Secret (e.g., "MyPassword123").

What happens: The app fetches the Data Key, encrypts your text into binary ciphertext, and sends ONLY the ciphertext to MongoDB.

2. Withdraw Secret
Enter the Owner name.

What happens: The app fetches the ciphertext, uses the Data Key to decrypt it locally, and reveals the plain text.

3. Peek Raw DB (The "Trust" Test)
This query bypasses the decryption logic and shows you what a database administrator would see.

Result: You will see Binary(...) gibberish. This proves that even if someone steals your database, they cannot read your secrets without the master-key.txt.

🛠️ Tech Stack Highlights

Java 21 Records: Used for immutable data transfer (SecretRecord).
Switch Expressions: Clean, modern control flow in the CLI.
Java NIO: Efficient file handling for key management.
Explicit Encryption: Manual control over the encryption/decryption lifecycle using mongodb-crypt.

⚠️ Security Disclaimer
This project uses a Local Key Provider (master-key.txt) for demonstration and educational purposes (the "Jugaad" method).

In a high-security production environment, you should replace the Local Provider with a remote Key Management Service (AWS KMS, Azure Key Vault, or HashiCorp Vault) to prevent the "Key" and the "Lock" from existing on the same server.

🤝 Contributing
Fork the repository
Create your feature branch (git checkout -b feature/AmazingFeature)
Commit your changes (git commit -m 'Add some AmazingFeature')
Push to the branch (git push origin feature/AmazingFeature)
Open a Pull Request

How to Use it?

Here is the complete operation manual for your JVault system. This guide covers starting the infrastructure, launching the application, and interacting with the secure Vault API.

System Architecture Overview
Database: MongoDB (Port 27017) – Stores encrypted secrets.

Application: Spring Boot Service (Port 8080) – Handles encryption, rotation, and API requests.

Security:

Master Key: A local file (master-key.txt) used to unlock the vault.

Data Key: Stored in MongoDB, encrypted by the Master Key.

App Secrets: Stored in MongoDB, encrypted by the Data Key.

Step 1: Infrastructure Setup
Before running the Java code, you must start the database and generate the security key.

1. Start MongoDB
Open a terminal and start your MongoDB instance.

Bash

# Default command (ensure the data folder exists)
mongod --dbpath /data/db
Verification: Ensure it is listening on port 27017.

2. Generate the Master Key
The application requires a 96-byte key file to unlock the encryption system. Run this command in the root folder of your project (where pom.xml is located).

Bash

# Mac/Linux
openssl rand -out master-key.txt 96

# Windows (Git Bash)
openssl rand -out master-key.txt 96
3. Verify Configuration
Open src/main/resources/application.properties and ensure vault.security.master-key-path points to the file you just created.

Recommendation: Use an absolute path to avoid "File not found" errors.

Properties

# Example
vault.security.master-key-path=/Users/rankesh/projects/jvault/master-key.txt
Step 2: Build and Run the Application
Use Maven to compile the code and start the Spring Boot server.

1. Clean and Install Dependencies
This ensures all MongoDB 5.x drivers are downloaded and cached correctly.

Bash

mvn clean install
2. Start the Service
Bash

mvn spring-boot:run
Successful Startup Logs:

⚙️ Initializing SecretVaultService... ✅ SecretVaultService Ready. Data Key ID: ... Tomcat started on port 8080

Step 3: How to Use the Vault (API Guide)
You can interact with the vault using curl or any API client (like Postman).

Scenario A: Requesting a Secret (First Access)
When an application (e.g., payment-service) requests a secret for the first time, the Vault will generate a new random password, encrypt it, save it to MongoDB, and return the plain text to the requester.

Request:

Bash

curl -X GET http://localhost:8080/api/secrets/payment-service
Response:

JSON

{
  "appId": "payment-service",
  "secretValue": "Xy9z...random_string...",
  "createdAt": "2023-12-17T10:00:00Z"
}
Scenario B: Retrieving an Existing Secret
If the service requests the secret again within 1 hour, the Vault fetches the encrypted blob from MongoDB, decrypts it locally, and returns the same secret.

Request:

Bash

curl -X GET http://localhost:8080/api/secrets/payment-service
Result: You receive the exact same secretValue as Scenario A.

Scenario C: Force Rotation (Manual Security Reset)
If you suspect a secret is compromised or want to force a change immediately (ignoring the 1-hour timer), use the rotation endpoint.

Request:

Bash

curl -X POST http://localhost:8080/api/secrets/payment-service/rotate
Result: A new secret is generated and overwrites the old one in the database.

Step 4: Verifying Security (The "Eye Test")
To verify that your data is actually encrypted at rest, you can inspect the database directly.

Open MongoDB Compass or mongosh.

Connect to localhost:27017.

Navigate to database encryption_vault -> collection secrets.

Look at the document for payment-service.

What you will see (Encrypted):

JSON

{
  "_id": ObjectId("..."),
  "appId": "payment-service",
  "secret": Binary.createFromBase64("Ah7...<GARBAGE_CHARACTERS>...", 6),
  "createdAt": ISODate("...")
}
Key Observation: The secret field is binary data (Type 6). It is not readable text. Even a database administrator cannot see the password without the Master Key.

Step 5: How Expiration Works (TTL)
Automatic: MongoDB has a background thread that checks the createdAt index.

The Event: Once createdAt is older than 1 hour (3600 seconds), MongoDB automatically deletes the document.

The Next Request: The next time you run GET /api/secrets/payment-service, the Vault will see the document is missing and automatically trigger the generation of a new secret.