package com.ayedata.simault.mcp.admin;

import com.ayedata.simault.model.AllowedApp;
import com.ayedata.simault.repository.AppRegistryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminRegistryTools {

    private static final Logger logger = LogManager.getLogger(AdminRegistryTools.class);
    private final AppRegistryRepository registry;

    public AdminRegistryTools(AppRegistryRepository registry) {
        this.registry = registry;
    }

    @Tool(description = "List all applications currently whitelisted in the Vault.")
    public List<AllowedApp> listAllowedApps() {
        logger.info("🤖 AI Tool Triggered: listAllowedApps");
        List<AllowedApp> apps = registry.findAll();
        logger.debug("Returning {} apps to AI context.", apps.size());
        return apps;
    }

    public record RegisterAppRequest(String appId, String description) {}

    @Tool(description = "Register a new application in the whitelist. Requires App ID and Description.")
    public String registerNewApp(RegisterAppRequest request) {
        // Log4j2 redaction will handle sensitive IDs if your regex matches, 
        // but typically App IDs are safe to log.
        logger.info("🤖 AI Tool Triggered: registerNewApp for appId={}", request.appId());
        
        try {
            registry.registerApp(request.appId(), request.description());
            logger.info("✅ App registered successfully via AI Tool: {}", request.appId());
            return "✅ Successfully registered: " + request.appId();
        } catch (Exception e) {
            logger.error("❌ AI Tool Failed: registerNewApp", e);
            return "❌ Failed to register: " + e.getMessage();
        }
    }
    
    @Tool(description = "Remove an application from the whitelist, revoking all access.")
    public String removeApp(String appId) {
        logger.warn("🤖 AI Tool Triggered: removeApp for appId={}", appId);
        
        try {
            registry.removeApp(appId);
            logger.info("🚫 App access revoked via AI Tool: {}", appId);
            return "🚫 Access revoked for: " + appId;
        } catch (Exception e) {
            logger.error("❌ AI Tool Failed: removeApp", e);
            return "❌ Failed to remove: " + e.getMessage();
        }
    }
}