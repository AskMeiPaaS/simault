package com.ayedata.simault.a2a.model;

import java.util.Map;

/**
 * Represents a single tool or function available to other agents.
 */
public record AgentCapability(
    String name,                      // e.g., "registerNewApp"
    String description,               // e.g., "Register a new application..."
    String type,                      // e.g., "function"
    Map<String, Object> inputSchema,  // JSON Schema defining the arguments
    Map<String, Object> outputSchema  // JSON Schema defining the return value
) {}