package com.ayedata.simault.mcp.vault; // <--- Package Updated

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.repository.AppRegistryRepository;
import com.ayedata.simault.service.SecretVaultService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;

@Component
public class SecretVaultTools {

    private final AppRegistryRepository registry;
    private final SecretVaultService vaultService;

    public SecretVaultTools(AppRegistryRepository registry, SecretVaultService vaultService) {
        this.registry = registry;
        this.vaultService = vaultService;
    }

    @Tool(description = "Retrieve the decrypted secret value for a registered application. WARNING: This exposes the raw password to the AI context.")
    public Map<String, Object> getSecret(String appId) {
        if (!registry.isAppAllowed(appId)) {
            return Map.of("status", "FAILED", "msg", "Access Denied: App not in whitelist");
        }

        try {
            AppSecret secret = vaultService.getAppSecret(appId);
            long ttl = 3600 - (Instant.now().getEpochSecond() - secret.createdAt().getEpochSecond());

            return Map.of(
                "appId", secret.appId(),
                "status", "SUCCESS",
                "secretValue", secret.secretValue(), // RAW DATA
                "expiresInSeconds", Math.max(0, ttl)
            );
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    @Tool(description = "Forcefully rotate (regenerate) the secret for a specific application. Use this if a secret is compromised or expired.")
    public Map<String, Object> rotateSecret(String appId) {
        if (!registry.isAppAllowed(appId)) {
            return Map.of("status", "FAILED", "msg", "Access Denied: App not in whitelist");
        }

        try {
            AppSecret newSecret = vaultService.rotateSecret(appId);
            return Map.of(
                "appId", newSecret.appId(),
                "action", "ROTATED",
                "status", "SUCCESS",
                "newGeneratedAt", newSecret.createdAt().toString(),
                "message", "Secret has been securely rotated."
            );
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }
}