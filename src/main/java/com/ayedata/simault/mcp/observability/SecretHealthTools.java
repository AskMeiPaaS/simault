package com.ayedata.simault.mcp.observability;

import com.ayedata.simault.repository.AppRegistryRepository;
import com.ayedata.simault.service.SecretVaultService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;

@Component
public class SecretHealthTools {

    private final AppRegistryRepository registry;
    private final SecretVaultService vaultService;

    public SecretHealthTools(AppRegistryRepository registry, SecretVaultService vaultService) {
        this.registry = registry;
        this.vaultService = vaultService;
    }

    @Tool(description = "Check secret health and TTL without revealing the value.")
    public Map<String, Object> checkSecretHealth(String appId) {
        if (!registry.isAppAllowed(appId)) {
            return Map.of("status", "UNREGISTERED", "msg", "App not in whitelist");
        }
        
        try {
            var secret = vaultService.getAppSecret(appId);
            long ttl = 3600 - (Instant.now().getEpochSecond() - secret.createdAt().getEpochSecond());

            return Map.of(
                "appId", secret.appId(),
                "status", "ACTIVE",
                "generatedAt", secret.createdAt().toString(),
                "expiresInSeconds", Math.max(0, ttl),
                "isExpired", ttl <= 0
            );
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }
}