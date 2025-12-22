package com.ayedata.simault.mcp.vault;

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.repository.AppRegistryRepository;
import com.ayedata.simault.service.SecretVaultService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class SecretVaultTools {

    // 1. Initialize Log4j 2 Logger
    private static final Logger logger = LogManager.getLogger(SecretVaultTools.class);

    private final AppRegistryRepository registry;
    private final SecretVaultService vaultService;

    public SecretVaultTools(AppRegistryRepository registry, SecretVaultService vaultService) {
        this.registry = registry;
        this.vaultService = vaultService;
    }

    @Tool(description = "Retrieve the decrypted secret value for a registered application. WARNING: This exposes the raw password to the AI context.")
    public Map<String, Object> getSecret(String appId) {
        logger.info("🤖 AI Tool Triggered: getSecret for appId={}", appId);

        if (!registry.isAppAllowed(appId)) {
            logger.warn("🛑 Access Denied: AI attempted to fetch secret for unauthorized app: {}", appId);
            return Map.of("status", "FAILED", "msg", "Access Denied: App not in whitelist");
        }

        try {
            AppSecret secret = vaultService.getAppSecret(appId);
            long ttl = 3600 - (Instant.now().getEpochSecond() - secret.createdAt().getEpochSecond());

            // We log that the action succeeded, but we DO NOT log the secret value itself.
            // Defense in depth: even if redaction fails, the secret is never written here.
            logger.info("✅ Secret successfully retrieved and returned to AI context for: {}", appId);

            return Map.of(
                "appId", secret.appId(),
                "status", "SUCCESS",
                "secretValue", secret.secretValue(), // RAW DATA (Sent to AI, not logged)
                "expiresInSeconds", Math.max(0, ttl)
            );
        } catch (Exception e) {
            logger.error("❌ Error retrieving secret for: {}", appId, e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    @Tool(description = "Forcefully rotate (regenerate) the secret for a specific application. Use this if a secret is compromised or expired.")
    public Map<String, Object> rotateSecret(String appId) {
        logger.warn("🤖 AI Tool Triggered: rotateSecret for appId={}", appId);

        if (!registry.isAppAllowed(appId)) {
            logger.warn("🛑 Rotate Failed: App not in whitelist: {}", appId);
            return Map.of("status", "FAILED", "msg", "Access Denied: App not in whitelist");
        }

        try {
            AppSecret newSecret = vaultService.rotateSecret(appId);
            
            logger.info("✅ Secret successfully rotated for: {}", appId);
            
            return Map.of(
                "appId", newSecret.appId(),
                "action", "ROTATED",
                "status", "SUCCESS",
                "newGeneratedAt", newSecret.createdAt().toString(),
                "message", "Secret has been securely rotated."
            );
        } catch (Exception e) {
            logger.error("❌ Error rotating secret for: {}", appId, e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }
}