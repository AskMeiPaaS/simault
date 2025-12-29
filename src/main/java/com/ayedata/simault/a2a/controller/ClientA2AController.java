package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.a2a.model.*;
import com.ayedata.simault.mcp.vault.SecretVaultTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client") // <--- Distinct base path for clients
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
                "api-key",
                "Requires 'X-ADMIN-KEY' (for rotation) or App-specific auth."
            )
        );
    }

    private List<AgentCapability> generateClientCapabilities() {
        return List.of(
            new AgentCapability(
                "getSecret",
                "Retrieve the decrypted secret value for a registered application.",
                "function",
                Map.of(
                    "type", "object",
                    "properties", Map.of("appId", Map.of("type", "string"))
                ),
                Map.of("type", "object", "description", "Secret details including raw value")
            ),
            new AgentCapability(
                "rotateSecret",
                "Forcefully rotate the secret for a specific application.",
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