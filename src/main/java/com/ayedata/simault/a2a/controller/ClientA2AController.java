package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.a2a.model.*;
import com.ayedata.simault.mcp.vault.SecretVaultTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes Client/Runtime capabilities to autonomous agents.
 * Base Path: /api/client/.well-known/agent-card
 * Matches logic in: com.ayedata.simault.controller.SecretController
 */
@RestController
@RequestMapping("/api/client")
public class ClientA2AController {

    private final SecretVaultTools vaultTools;

    public ClientA2AController(SecretVaultTools vaultTools) {
        this.vaultTools = vaultTools;
    }

    @GetMapping("/.well-known/agent-card")
    public AgentCard getClientAgentCard() {
        return new AgentCard(
            "agent-card",
            "1.0.0",
            new AgentIdentity(
                "Simault-Client-Agent",
                "Client Agent. Retrieves and rotates secrets for registered apps.",
                "1.0.0",
                "MIT",
                "Ayedata Inc."
            ),
            generateClientCapabilities(),
            new AgentAuth(
                "none",
                "Public endpoint, but 'appId' must be whitelisted by an Admin first."
            )
        );
    }

    private List<AgentCapability> generateClientCapabilities() {
        return List.of(
            // Maps to SecretController.getSecret()
            new AgentCapability(
                "getSecret",
                "Retrieve the decrypted secret value for a registered application.",
                "function",
                Map.of(
                    "type", "object",
                    "properties", Map.of("appId", Map.of("type", "string"))
                ),
                Map.of("type", "object", "description", "Secret object containing 'secretValue'")
            ),

            // Maps to SecretController.rotateSecret()
            new AgentCapability(
                "rotateSecret",
                "Forcefully rotate (regenerate) the secret for a specific application.",
                "function",
                Map.of(
                    "type", "object",
                    "properties", Map.of("appId", Map.of("type", "string"))
                ),
                Map.of("type", "object", "description", "New secret metadata")
            )
        );
    }
}