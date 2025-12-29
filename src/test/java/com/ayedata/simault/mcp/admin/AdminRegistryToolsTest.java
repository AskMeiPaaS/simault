package com.ayedata.simault.mcp.admin;

import com.ayedata.simault.repository.AppRegistryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminRegistryToolsTest {

    @Mock private AppRegistryRepository registry;
    @InjectMocks private AdminRegistryTools adminTools;

    @Test
    void testRegisterNewApp() {
        var request = new AdminRegistryTools.RegisterAppRequest("payment-service", "Handles Payments");
        
        String response = adminTools.registerNewApp(request);
        
        verify(registry).registerApp("payment-service", "Handles Payments");
        assertTrue(response.contains("Successfully registered"));
    }

    @Test
    void testRemoveApp() {
        String response = adminTools.removeApp("legacy-service");
        
        verify(registry).removeApp("legacy-service");
        assertTrue(response.contains("Access revoked"));
    }
}