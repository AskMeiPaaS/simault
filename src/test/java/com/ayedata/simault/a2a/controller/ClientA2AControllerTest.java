package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.mcp.vault.SecretVaultTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Spring Boot 3.4+ Standard for mocking beans
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
                // 1. Verify Identity
                .andExpect(jsonPath("$.identity.name").value("Simault-Client-Agent"))

                // 2. Verify Auth Instruction (Client is often public or app-specific)
                .andExpect(jsonPath("$.authentication.type").value("none"))

                // 3. Verify Client Capabilities Exist
                .andExpect(jsonPath("$.capabilities[?(@.name=='getSecret')]").exists())
                .andExpect(jsonPath("$.capabilities[?(@.name=='rotateSecret')]").exists())

                // 4. Verify Admin Capabilities are ABSENT (Segregation Check)
                .andExpect(jsonPath("$.capabilities[?(@.name=='registerNewApp')]").doesNotExist())
                .andExpect(jsonPath("$.capabilities[?(@.name=='removeApp')]").doesNotExist());
    }
}