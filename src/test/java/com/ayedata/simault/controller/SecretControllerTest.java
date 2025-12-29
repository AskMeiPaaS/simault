package com.ayedata.simault.controller;

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.service.SecretVaultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// New Spring Boot 3.4+ Mocking Annotation
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecretController.class)
class SecretControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Use @MockitoBean (replaces @MockBean in Spring Boot 3.4+)
    @MockitoBean
    private SecretVaultService vaultService;

    @Test
    void getSecret_ShouldReturnSecret_WhenAppIsAllowed() throws Exception {
        // Setup
        String appId = "valid-app";
        // Matches the AppSecret record signature used in your Repository
        AppSecret mockSecret = new AppSecret(appId, "encrypted-secret-value-123", Instant.now());
        
        when(vaultService.getAppSecret(appId)).thenReturn(mockSecret);

        // Execute & Verify
        mockMvc.perform(get("/api/secrets/{appId}", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.secretValue").value("encrypted-secret-value-123"));

        verify(vaultService, times(1)).getAppSecret(appId);
    }

    @Test
    void getSecret_ShouldFail_WhenServiceThrowsException() throws Exception {
        // Simulate Unauthorized access or missing secret (Service layer logic)
        String appId = "banned-app";
        when(vaultService.getAppSecret(appId))
            .thenThrow(new IllegalArgumentException("App not allowed"));

        // Expecting 5xx (or 403/400 depending on GlobalExceptionHandler)
        // Since controller rethrows, it bubbles up as 500 by default in tests
        mockMvc.perform(get("/api/secrets/{appId}", appId))
                .andExpect(status().is5xxServerError()); 
    }

    @Test
    void rotateSecret_ShouldReturnNewSecret_WhenCalled() throws Exception {
        String appId = "legacy-app";
        AppSecret newSecret = new AppSecret(appId, "newly-rotated-secret", Instant.now());

        when(vaultService.rotateSecret(appId)).thenReturn(newSecret);

        mockMvc.perform(post("/api/secrets/{appId}/rotate", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretValue").value("newly-rotated-secret"));

        verify(vaultService, times(1)).rotateSecret(appId);
    }
}