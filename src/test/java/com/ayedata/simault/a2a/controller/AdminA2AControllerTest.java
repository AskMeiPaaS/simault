package com.ayedata.simault.a2a.controller;

import com.ayedata.simault.mcp.admin.AdminRegistryTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Spring Boot 3.4+ Standard for mocking beans
import org.springframework.test.context.bean.override.mockito.MockitoBean; 
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminA2AController.class)
class AdminA2AControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // We mock the underlying tool service to ensure the controller loads
    @MockitoBean
    private AdminRegistryTools adminTools;

    @Test
    void getAdminAgentCard_ShouldReturnAdminCapabilities() throws Exception {
        mockMvc.perform(get("/api/admin/.well-known/agent-card"))
                .andExpect(status().isOk())
                // 1. Verify Identity
                .andExpect(jsonPath("$.identity.name").value("Simault-Admin-Agent"))
                
                // 2. Verify Auth Instruction (Admin uses API Key)
                .andExpect(jsonPath("$.authentication.type").value("api-key"))

                // 3. Verify Admin Capabilities Exist
                .andExpect(jsonPath("$.capabilities[?(@.name=='registerNewApp')]").exists())
                .andExpect(jsonPath("$.capabilities[?(@.name=='listAllowedApps')]").exists())
                .andExpect(jsonPath("$.capabilities[?(@.name=='removeApp')]").exists())

                // 4. Verify Client Capabilities are ABSENT (Segregation Check)
                .andExpect(jsonPath("$.capabilities[?(@.name=='getSecret')]").doesNotExist())
                .andExpect(jsonPath("$.capabilities[?(@.name=='rotateSecret')]").doesNotExist());
    }
}