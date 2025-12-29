package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.a2a.model.*;
import com.ayedata.simault.mcp.admin.AdminRegistryTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes Administrative capabilities to autonomous agents.
 * Base Path: /api/admin/.well-known/agent-card
 */
@RestController
@RequestMapping("/api/admin")
public class AdminA2AController {

    private final AdminRegistryTools adminTools;

    public AdminA2AController(AdminRegistryTools adminTools) {
        this.adminTools = adminTools;
    }

    @GetMapping("/.well-known/agent-card")
    public AgentCard getAdminAgentCard() {
        return new AgentCard(
            "agent-card",
            "1.0.0",
            new AgentIdentity(
                "Simault-Admin-Agent",
                "Administrative Agent. Manages application whitelists and revocations.",
                "1.0.0",
                "MIT",
                "Ayedata Inc."
            ),
            generateAdminCapabilities(),
            new AgentAuth(
                "api-key",
                "Requires 'X-ADMIN-KEY' header for all operations."
            )
        );
    }

    private List<AgentCapability> generateAdminCapabilities() {
        return List.of(
            // Maps to AdminController.registerApp()
            new AgentCapability(
                "registerNewApp",
                "Register a new application in the whitelist.",
                "function",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "appId", Map.of("type", "string", "description", "The unique application identifier"),
                        "description", Map.of("type", "string", "description", "Purpose of the application")
                    ),
                    "required", List.of("appId", "description")
                ),
                Map.of("type", "string", "description", "Confirmation message")
            ),
            
            // Maps to AdminController.listApps()
            new AgentCapability(
                "listAllowedApps",
                "List all applications currently whitelisted.",
                "function",
                Map.of("type", "object", "properties", Map.of()), // No arguments
                Map.of("type", "array", "description", "List of allowed applications")
            ),

            // Maps to AdminController.removeApp()
            new AgentCapability(
                "removeApp",
                "Remove an application from the whitelist, immediately revoking access.",
                "function",
                Map.of(
                    "type", "object", 
                    "properties", Map.of("appId", Map.of("type", "string"))
                ),
                Map.of("type", "string", "description", "Revocation confirmation")
            )
        );
    }
}