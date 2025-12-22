package com.ayedata.simault.mcp.observability;

import com.ayedata.simault.repository.AppRegistryRepository;
import com.ayedata.simault.service.SecretVaultService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class SecretHealthTools {

    // 1. Initialize Log4j 2 Logger
    private static final Logger logger = LogManager.getLogger(SecretHealthTools.class);

    private final AppRegistryRepository registry;
    private final SecretVaultService vaultService;

    public SecretHealthTools(AppRegistryRepository registry, SecretVaultService vaultService) {
        this.registry = registry;
        this.vaultService = vaultService;
    }

    @Tool(description = "Check secret health and TTL without revealing the value.")
    public Map<String, Object> checkSecretHealth(String appId) {
        // Log the tool trigger
        logger.info("🤖 AI Tool Triggered: checkSecretHealth for appId={}", appId);

        if (!registry.isAppAllowed(appId)) {
            logger.warn("Health check failed: App not allowed or unregistered: {}", appId);
            return Map.of("status", "UNREGISTERED", "msg", "App not in whitelist");
        }
        
        try {
            var secret = vaultService.getAppSecret(appId);
            long ttl = 3600 - (Instant.now().getEpochSecond() - secret.createdAt().getEpochSecond());

            logger.info("✅ Secret health status retrieved successfully for: {}", appId);
            logger.debug("TTL for appId={} is {} seconds", appId, ttl);

            return Map.of(
                "appId", secret.appId(),
                "status", "ACTIVE",
                "generatedAt", secret.createdAt().toString(),
                "expiresInSeconds", Math.max(0, ttl),
                "isExpired", ttl <= 0
            );
        } catch (Exception e) {
            logger.error("❌ Error checking secret health for: {}", appId, e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }
}