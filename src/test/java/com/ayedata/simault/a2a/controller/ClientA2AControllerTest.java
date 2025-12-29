package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.mcp.vault.SecretVaultTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Spring Boot 3.4+
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientA2AController.class)
class ClientA2AControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecretVaultTools vaultTools;

    @Test
    void getClientAgentCard_ShouldReturnClientCapabilities() throws Exception {
        mockMvc.perform(get("/api/client/.well-known/agent-card"))
                .andExpect(status().isOk())
                // Verify Identity
                .andExpect(jsonPath("$.identity.name").value("Simault-Client-Agent"))
                // Verify Auth Instruction (Clients use different auth than Admins)
                .andExpect(jsonPath("$.authentication.type").value("none"))
                // Verify Client Capabilities exist
                .andExpect(jsonPath("$.capabilities[?(@.name=='getSecret')]").exists())
                .andExpect(jsonPath("$.capabilities[?(@.name=='rotateSecret')]").exists())
                // Verify Admin Capabilities are ABSENT (Segregation check)
                .andExpect(jsonPath("$.capabilities[?(@.name=='registerNewApp')]").doesNotExist());
    }
}