package com.ayedata.simault.controller;

import com.ayedata.simault.model.AllowedApp;
import com.ayedata.simault.model.VaultKey;
import com.ayedata.simault.repository.AppRegistryRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 1. Initialize Log4j 2 Logger
    private static final Logger logger = LogManager.getLogger(AdminController.class);

    // 2. XSS PREVENTION: Define a strict allowlist pattern for Identifiers.
    // Allows: alphanumeric, underscores, and hyphens.
    // Blocks: HTML tags (< >), quotes, slashes, and spaces.
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    private final AppRegistryRepository registry;
    private final MongoClient mongoClient;

    @Value("${vault.admin.api-key}")
    private String adminApiKey;

    @Value("${vault.security.key-alt-name}")
    private String defaultKeyAltName;

    public AdminController(AppRegistryRepository registry, MongoClient mongoClient) {
        this.registry = registry;
        this.mongoClient = mongoClient;
    }

    /**
     * Helper to check authorization and log failures.
     */
    private boolean isUnauthorized(String requestKey) {
        // Log the key with "token=" prefix so the Regex Replacement picks it up and redacts it.
        logger.debug("Validating admin access for token={}", requestKey);

        if (requestKey == null || !requestKey.equals(adminApiKey)) {
            logger.warn("Unauthorized access attempt. Invalid or missing API key.");
            return true;
        }
        return false;
    }

    /**
     * XSS VALIDATION HELPER
     * Returns true if the input contains invalid characters (potential scripts).
     */
    private boolean isInvalidInput(String input) {
        return input != null && !SAFE_ID_PATTERN.matcher(input).matches();
    }

    // --- KEY ENDPOINT ---

    @GetMapping("/keys")
    public ResponseEntity<VaultKey> findKey(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @RequestParam(required = false) String altName) {

        if (isUnauthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 3. XSS FIX: Validate altName if provided
        // This prevents malicious scripts from entering the searchName logic or being logged
        if (altName != null && isInvalidInput(altName)) {
            logger.warn("Potential XSS attempt rejected for altName: {}", altName);
            return ResponseEntity.badRequest().build();
        }

        // 1. Determine which key name to search for
        String searchName = (altName != null && !altName.isBlank()) ? altName : defaultKeyAltName;
        logger.info("Admin requesting key details for alias: '{}'", searchName);

        try {
            // 2. Query MongoDB
            MongoCollection<Document> keyVault = mongoClient.getDatabase("encryption").getCollection("__keyVault");
            Document keyDoc = keyVault.find(new Document("keyAltNames", searchName)).first();

            if (keyDoc == null) {
                logger.warn("Key not found for alias: '{}'", searchName);
                return ResponseEntity.notFound().build();
            }

            // 3. Map BSON to Java Model (VaultKey)
            Binary bsonBinary = keyDoc.get("_id", Binary.class);
            ByteBuffer buffer = ByteBuffer.wrap(bsonBinary.getData());
            UUID keyId = new UUID(buffer.getLong(), buffer.getLong());

            String provider = keyDoc.get("masterKey", Document.class).getString("provider");

            VaultKey response = new VaultKey(
                searchName,
                keyId.toString(),
                "Active",
                provider
            );

            logger.info("Key details retrieved successfully for alias: '{}'", searchName);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Database error while retrieving key for alias: '{}'", searchName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- APP ENDPOINTS ---

    @GetMapping("/apps")
    public ResponseEntity<List<AllowedApp>> listApps(@RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey) {
        if (isUnauthorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        logger.info("Fetching list of all allowed apps.");
        List<AllowedApp> apps = registry.findAll();
        logger.debug("Found {} registered apps.", apps.size());
        
        return ResponseEntity.ok(apps);
    }

    @PostMapping("/apps")
    public ResponseEntity<String> registerApp(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @RequestBody Map<String, String> payload) {
        
        if (isUnauthorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("⛔ Unauthorized");
        
        String appId = payload.get("appId");
        
        // 4. XSS FIX: Validate appId input
        // Since we reflect 'appId' back in the response string ("App registered: " + appId),
        // we MUST validate it to prevent Reflected XSS.
        if (appId == null || isInvalidInput(appId)) {
            logger.warn("Registration rejected. Invalid format or potential XSS in appId: {}", appId);
            return ResponseEntity.badRequest().body("Invalid App ID format. Use alphanumeric, dashes, or underscores only.");
        }

        // Log the payload safely. Log4j would automatically redact sensitive fields if configured.
        logger.info("Attempting to register app. Payload: {}", payload);

        try {
            registry.registerApp(appId, payload.get("description"));
            logger.info("✅ App registered successfully: {}", appId);
            return ResponseEntity.ok("✅ App registered: " + appId);
        } catch (Exception e) {
            logger.error("Failed to register app: {}", appId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed");
        }
    }

    @DeleteMapping("/apps/{appId}")
    public ResponseEntity<String> removeApp(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @PathVariable String appId) {
        
        if (isUnauthorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("⛔ Unauthorized");

        // 5. XSS FIX: Validate path variable
        if (isInvalidInput(appId)) {
             logger.warn("Revocation rejected. Invalid format or potential XSS in appId: {}", appId);
             return ResponseEntity.badRequest().body("Invalid App ID format.");
        }

        logger.info("Attempting to revoke access for app: {}", appId);

        try {
            registry.removeApp(appId);
            logger.info("🚫 Access revoked successfully for: {}", appId);
            return ResponseEntity.ok("🚫 Access revoked for: " + appId);
        } catch (Exception e) {
            logger.error("Failed to revoke app: {}", appId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Revocation failed");
        }
    }
}