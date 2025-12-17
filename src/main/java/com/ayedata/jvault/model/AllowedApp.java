package com.ayedata.jvault.model;

import java.time.Instant;

// Java 21 Record
public record AllowedApp(
    String appId,
    String description,
    Instant registeredAt
) {}