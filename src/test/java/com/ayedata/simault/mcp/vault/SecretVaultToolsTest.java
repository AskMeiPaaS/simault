package com.ayedata.simault.mcp.vault;

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.repository.AppRegistryRepository;
import com.ayedata.simault.service.SecretVaultService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecretVaultToolsTest {

    @Mock private AppRegistryRepository registry;
    @Mock private SecretVaultService vaultService;
    @InjectMocks private SecretVaultTools vaultTools;

    @Test
    void testGetSecret_Success() {
        // Setup
        String appId = "payment-app";
        AppSecret mockSecret = new AppSecret(appId, "secret-123", Instant.now());
        
        when(registry.isAppAllowed(appId)).thenReturn(true);
        when(vaultService.getAppSecret(appId)).thenReturn(mockSecret);

        // Execute
        Map<String, Object> result = vaultTools.getSecret(appId);

        // Assert
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("secret-123", result.get("secretValue")); // Ensure raw value is passed to AI
    }

    @Test
    void testGetSecret_AccessDenied() {
        String appId = "unknown-app";
        when(registry.isAppAllowed(appId)).thenReturn(false);

        Map<String, Object> result = vaultTools.getSecret(appId);

        assertEquals("FAILED", result.get("status"));
        assertTrue(result.get("msg").toString().contains("Access Denied"));
        verify(vaultService, never()).getAppSecret(anyString());
    }
}