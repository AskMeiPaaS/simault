package com.ayedata.simault.controller;

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.service.SecretVaultService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secrets")
public class SecretController {

    // 1. Initialize Log4j 2 Logger
    private static final Logger logger = LogManager.getLogger(SecretController.class);

    private final SecretVaultService vaultService;

    // Notice: We DO NOT inject AppRegistryRepository here. 
    // This controller physically cannot register new apps.
    public SecretController(SecretVaultService vaultService) {
        this.vaultService = vaultService;
    }

    /**
     * GET SECRET
     * Validates if app is allowed. If allowed but missing, generates a new one.
     * If NOT allowed, throws 403 Forbidden (via Service check).
     */
    @GetMapping("/{appId}")
    public ResponseEntity<AppSecret> getSecret(@PathVariable String appId) {
        logger.info("Request received to retrieve secret for appId: {}", appId);

        try {
            // The service performs the "isAppAllowed()" check immediately.
            AppSecret secret = vaultService.getAppSecret(appId);
            
            // We log success, but we avoid logging the 'secret' object itself to prevent
            // accidental leakage, even though our Log4j regex would likely catch it.
            logger.info("Secret successfully retrieved for appId: {}", appId);
            
            return ResponseEntity.ok(secret);
        } catch (Exception e) {
            // Log the error. If the error message contains sensitive info, Log4j will redact it.
            logger.error("Failed to retrieve secret for appId: {}", appId, e);
            throw e; // Re-throw to let Spring handle the error response (e.g., 403 or 500)
        }
    }

    /**
     * ROTATE SECRET (Manual Trigger)
     * Forces a new password generation for an ALREADY REGISTERED app.
     * Cannot be used to register a new app.
     */
    @PostMapping("/{appId}/rotate")
    public ResponseEntity<AppSecret> rotateSecret(@PathVariable String appId) {
        logger.warn("Manual secret rotation requested for appId: {}", appId);

        try {
            // The service performs the "isAppAllowed()" check immediately.
            AppSecret secret = vaultService.rotateSecret(appId);
            
            logger.info("Secret successfully rotated for appId: {}", appId);
            
            return ResponseEntity.ok(secret);
        } catch (Exception e) {
            logger.error("Failed to rotate secret for appId: {}", appId, e);
            throw e;
        }
    }
}