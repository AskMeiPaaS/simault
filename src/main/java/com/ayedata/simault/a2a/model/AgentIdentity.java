package com.ayedata.simault.a2a.model;

/**
 * Metadata about the Agent's identity.
 */
public record AgentIdentity(
    String name,        // e.g., "Simault-Vault-Admin"
    String description, // e.g., "Administrative Agent for Simault Vault..."
    String version,     // e.g., "1.0.0"
    String license,     // e.g., "MIT"
    String author       // e.g., "Ayedata Inc."
) {}