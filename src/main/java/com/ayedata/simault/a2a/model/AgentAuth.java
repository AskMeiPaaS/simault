package com.ayedata.simault.a2a.model;

/**
 * Authentication instructions for calling this agent.
 */
public record AgentAuth(
    String type,        // e.g., "api-key", "oauth2", "bearer"
    String instruction  // e.g., "Include 'X-ADMIN-KEY' in headers"
) {}