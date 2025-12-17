package com.ayedata.jvault.controller;

import com.ayedata.jvault.model.AllowedApp;
import com.ayedata.jvault.repository.AppRegistryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/apps")
public class AdminController {

    private final AppRegistryRepository registry;
    
    // Inject the admin key from application.properties
    @Value("${vault.admin.api-key}")
    private String adminApiKey;

    public AdminController(AppRegistryRepository registry) {
        this.registry = registry;
    }

    // Security Helper: Checks if the provided key matches the configured secret
    private boolean isUnauthorized(String requestKey) {
        return requestKey == null || !requestKey.equals(adminApiKey);
    }

    /**
     * LIST ALL APPS
     * GET /api/admin/apps
     */
    @GetMapping
    public ResponseEntity<?> listApps(@RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey) {
        // 1. Security Check
        if (isUnauthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("⛔ Unauthorized: Invalid Admin Key");
        }
        
        // 2. Fetch and Return List
        List<AllowedApp> apps = registry.findAll();
        return ResponseEntity.ok(apps);
    }

    /**
     * REGISTER NEW APP
     * POST /api/admin/apps
     */
    @PostMapping
    public ResponseEntity<String> registerApp(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @RequestBody Map<String, String> payload) {
        
        if (isUnauthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("⛔ Unauthorized");
        }

        String appId = payload.get("appId");
        String description = payload.get("description");
        
        if (appId == null || appId.isBlank()) {
            return ResponseEntity.badRequest().body("❌ Error: appId is required");
        }

        registry.registerApp(appId, description);
        return ResponseEntity.ok("✅ App registered successfully: " + appId);
    }

    /**
     * REMOVE APP
     * DELETE /api/admin/apps/{appId}
     */
    @DeleteMapping("/{appId}")
    public ResponseEntity<String> removeApp(
            @RequestHeader(value = "X-ADMIN-KEY", required = false) String apiKey,
            @PathVariable String appId) {
        
        if (isUnauthorized(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("⛔ Unauthorized");
        }
        
        registry.removeApp(appId);
        return ResponseEntity.ok("🚫 Access revoked for: " + appId);
    }
}