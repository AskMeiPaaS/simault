package com.ayedata.simault.controller;

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.service.SecretVaultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

    @MockitoBean
    private SecretVaultService vaultService;

    @Test
    void getSecret_ShouldReturnSecret_WhenAppIsAllowed() throws Exception {
        String appId = "valid-app";
        AppSecret mockSecret = new AppSecret(appId, "encrypted-secret-value-123", Instant.now());
        
        when(vaultService.getAppSecret(appId)).thenReturn(mockSecret);

        mockMvc.perform(get("/api/secrets/{appId}", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.secretValue").value("encrypted-secret-value-123"));
        
        verify(vaultService, times(1)).getAppSecret(appId);
    }

    @Test
    void getSecret_ShouldFail_WhenServiceThrowsException() throws Exception {
        String appId = "banned-app";
        
        // Simulate an unauthorized access exception from the service
        when(vaultService.getAppSecret(appId))
            .thenThrow(new IllegalArgumentException("App not allowed"));

        // FIX: The application maps this failure to 403 Forbidden, so we must expect isForbidden()
        mockMvc.perform(get("/api/secrets/{appId}", appId))
                .andExpect(status().isForbidden()); 
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