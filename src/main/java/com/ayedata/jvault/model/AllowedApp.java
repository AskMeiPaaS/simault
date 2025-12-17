package com.ayedata.jvault.model;

import java.time.Instant;

/**
 * A simple Java Record to represent an authorized application.
 * Records are immutable data carriers, perfect for this use case.
 */
public record AllowedApp(
    String appId,
    String description,
    Instant registeredAt
) {}