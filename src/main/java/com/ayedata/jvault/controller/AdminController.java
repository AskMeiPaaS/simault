package com.ayedata.jvault.controller;

import com.ayedata.jvault.model.AllowedApp;
import com.ayedata.jvault.model.VaultKey; // Import new model
import com.ayedata.jvault.repository.AppRegistryRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

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

    private boolean isUnauthorized(String requestKey) {
        return requestKey == null || !requestKey.equals(adminApiKey);
    }

    // --- KEY ENDPOINT ---

    @GetMapping("/keys")
    public ResponseEntity<VaultKey> findKey(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @RequestParam(required = false) String altName) {

        if (isUnauthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 1. Determine which key name to search for
        String searchName = (altName != null && !altName.isBlank()) ? altName : defaultKeyAltName;

        // 2. Query MongoDB
        MongoCollection<Document> keyVault = mongoClient.getDatabase("encryption").getCollection("__keyVault");
        Document keyDoc = keyVault.find(new Document("keyAltNames", searchName)).first();

        if (keyDoc == null) {
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

        return ResponseEntity.ok(response);
    }

    // --- APP ENDPOINTS ---

    @GetMapping("/apps")
    public ResponseEntity<List<AllowedApp>> listApps(@RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey) {
        if (isUnauthorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<AllowedApp> apps = registry.findAll();
        return ResponseEntity.ok(apps);
    }

    @PostMapping("/apps")
    public ResponseEntity<String> registerApp(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @RequestBody Map<String, String> payload) {
        
        if (isUnauthorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("⛔ Unauthorized");
        
        String appId = payload.get("appId");
        if (appId == null || appId.isBlank()) return ResponseEntity.badRequest().body("appId is required");

        registry.registerApp(appId, payload.get("description"));
        return ResponseEntity.ok("✅ App registered: " + appId);
    }

    @DeleteMapping("/apps/{appId}")
    public ResponseEntity<String> removeApp(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @PathVariable String appId) {
        
        if (isUnauthorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("⛔ Unauthorized");
        registry.removeApp(appId);
        return ResponseEntity.ok("🚫 Access revoked for: " + appId);
    }
}