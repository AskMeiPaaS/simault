package com.ayedata.jvault.model;

// Java 21 Record for immutable data
public record VaultKey(
    String keyAltName,
    String keyId,
    String status,
    String provider
) {}