package com.ayedata.simault.mcp.admin;

import com.ayedata.simault.model.AllowedApp;
import com.ayedata.simault.repository.AppRegistryRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AdminRegistryTools {

    private final AppRegistryRepository registry;

    public AdminRegistryTools(AppRegistryRepository registry) {
        this.registry = registry;
    }

    @Tool(description = "List all applications currently whitelisted in the Vault.")
    public List<AllowedApp> listAllowedApps() {
        return registry.findAll();
    }

    public record RegisterAppRequest(String appId, String description) {}

    @Tool(description = "Register a new application in the whitelist. Requires App ID and Description.")
    public String registerNewApp(RegisterAppRequest request) {
        registry.registerApp(request.appId(), request.description());
        return "✅ Successfully registered: " + request.appId();
    }
    
    @Tool(description = "Remove an application from the whitelist, revoking all access.")
    public String removeApp(String appId) {
        registry.removeApp(appId);
        return "🚫 Access revoked for: " + appId;
    }
}