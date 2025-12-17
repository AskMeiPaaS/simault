package com.ayedata.simault.controller;

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.service.SecretVaultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secrets")
public class SecretController {

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
        // The service performs the "isAppAllowed()" check immediately.
        AppSecret secret = vaultService.getAppSecret(appId);
        return ResponseEntity.ok(secret);
    }

    /**
     * ROTATE SECRET (Manual Trigger)
     * Forces a new password generation for an ALREADY REGISTERED app.
     * Cannot be used to register a new app.
     */
    @PostMapping("/{appId}/rotate")
    public ResponseEntity<AppSecret> rotateSecret(@PathVariable String appId) {
        // The service performs the "isAppAllowed()" check immediately.
        AppSecret secret = vaultService.rotateSecret(appId);
        return ResponseEntity.ok(secret);
    }
}