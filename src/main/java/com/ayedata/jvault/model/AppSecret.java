package com.ayedata.jvault.model;

import java.time.Instant;

// Java 21 Record
public record AppSecret(
    String appId, 
    String secretValue, 
    Instant createdAt
) {}