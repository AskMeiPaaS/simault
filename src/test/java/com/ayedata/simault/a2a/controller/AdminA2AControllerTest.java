package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.mcp.admin.AdminRegistryTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Spring Boot 3.4+
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminA2AController.class)
class AdminA2AControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // We must mock the tool bean, even if the controller only uses it for structural dependency
    @MockitoBean
    private AdminRegistryTools adminTools;

    @Test
    void getAdminAgentCard_ShouldReturnAdminCapabilities() throws Exception {
        mockMvc.perform(get("/api/admin/.well-known/agent-card"))
                .andExpect(status().isOk())
                // Verify Identity
                .andExpect(jsonPath("$.identity.name").value("Simault-Admin-Agent"))
                // Verify Auth Instruction
                .andExpect(jsonPath("$.authentication.instruction").value("Requires 'X-ADMIN-KEY' header for all operations."))
                // Verify Admin Capabilities exist
                .andExpect(jsonPath("$.capabilities[?(@.name=='registerNewApp')]").exists())
                .andExpect(jsonPath("$.capabilities[?(@.name=='listAllowedApps')]").exists())
                .andExpect(jsonPath("$.capabilities[?(@.name=='removeApp')]").exists())
                // Verify Client Capabilities are ABSENT (Segregation check)
                .andExpect(jsonPath("$.capabilities[?(@.name=='getSecret')]").doesNotExist());
    }
}