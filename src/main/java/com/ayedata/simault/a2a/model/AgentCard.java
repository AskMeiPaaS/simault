package com.ayedata.simault.a2a.model;

import java.util.List;

/**
 * Root object for the Agent-to-Agent (A2A) Protocol Card.
 * Hosted at /.well-known/agent-card
 */
public record AgentCard(
    String type,            // e.g., "agent-card"
    String version,         // e.g., "1.0.0"
    AgentIdentity identity,
    List<AgentCapability> capabilities,
    AgentAuth authentication
) {}