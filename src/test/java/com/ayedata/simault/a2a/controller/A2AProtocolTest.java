package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.mcp.admin.AdminRegistryTools;
import com.ayedata.simault.mcp.vault.SecretVaultTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminA2AController.class, ClientA2AController.class})
class A2AProtocolTest {

    @Autowired private MockMvc mockMvc;
    
    // Mock the tools so we don't need real DB connections
    @MockBean private AdminRegistryTools adminTools;
    @MockBean private SecretVaultTools vaultTools;

    @Test
    void testAdminDiscovery() throws Exception {
        mockMvc.perform(get("/api/admin/.well-known/agent-card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.name").value("Simault-Admin-Agent"))
                .andExpect(jsonPath("$.capabilities[?(@.name=='registerNewApp')]").exists());
    }

    @Test
    void testClientDiscovery() throws Exception {
        mockMvc.perform(get("/api/client/.well-known/agent-card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.name").value("Simault-Client-Agent"))
                .andExpect(jsonPath("$.capabilities[?(@.name=='getSecret')]").exists());
    }
}