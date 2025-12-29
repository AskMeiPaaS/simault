package com.ayedata.simault.controller;

import com.ayedata.simault.model.AllowedApp;
import com.ayedata.simault.repository.AppRegistryRepository;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// New Spring Boot 3.4+ Mocking Annotation
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// Standard Java Imports
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Mockito & MVC Imports
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@TestPropertySource(properties = {
    "vault.admin.api-key=test-admin-key",
    "vault.security.key-alt-name=test-key-alias"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Use @MockitoBean (replaces @MockBean in Spring Boot 3.4+)
    @MockitoBean
    private AppRegistryRepository registry;

    // We must mock the MongoClient chain because AdminController injects it directly
    @MockitoBean private MongoClient mongoClient;
    @MockitoBean private MongoDatabase mongoDatabase;
    @MockitoBean private MongoCollection<Document> mongoCollection;
    @MockitoBean private FindIterable<Document> findIterable;

    private final String VALID_API_KEY = "test-admin-key";

    @Test
    void listApps_ShouldReturnList_WhenKeyIsValid() throws Exception {
        // 1. Create a valid AllowedApp matching the Repository's constructor usage (3 args)
        AllowedApp mockApp = new AllowedApp("app-1", "Billing Service", Instant.now());

        // 2. Use Collections.singletonList to explicitly type the return value
        when(registry.findAll()).thenReturn(Collections.singletonList(mockApp));

        // 3. Perform and Verify
        mockMvc.perform(get("/api/admin/apps")
                .header("X-ADMIN-KEY", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appId").value("app-1"))
                .andExpect(jsonPath("$[0].description").value("Billing Service"));
        
        verify(registry, times(1)).findAll();
    }

    @Test
    void listApps_ShouldReturnUnauthorized_WhenKeyIsMissing() throws Exception {
        mockMvc.perform(get("/api/admin/apps"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerApp_ShouldRegister_WhenPayloadIsValid() throws Exception {
        String jsonPayload = "{\"appId\": \"payment-service\", \"description\": \"Handles payments\"}";

        mockMvc.perform(post("/api/admin/apps")
                .header("X-ADMIN-KEY", VALID_API_KEY)
                .contentType("application/json")
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("✅ App registered: payment-service")));

        verify(registry, times(1)).registerApp("payment-service", "Handles payments");
    }

    @Test
    void removeApp_ShouldRevoke_WhenAuthorized() throws Exception {
        mockMvc.perform(delete("/api/admin/apps/payment-service")
                .header("X-ADMIN-KEY", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("🚫 Access revoked for: payment-service")));

        verify(registry, times(1)).removeApp("payment-service");
    }

    @Test
    void findKey_ShouldReturnKeyDetails_WhenFound() throws Exception {
        // 1. Mock the MongoDB Chain
        when(mongoClient.getDatabase("encryption")).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection("__keyVault")).thenReturn(mongoCollection);
        when(mongoCollection.find(any(Document.class))).thenReturn(findIterable);

        // 2. Create a Mock BSON Document resembling a Mongo Key
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        Binary binaryId = new Binary(bb.array());

        Document mockKeyDoc = new Document("_id", binaryId)
                .append("keyAltNames", List.of("test-key-alias"))
                .append("masterKey", new Document("provider", "local"));

        when(findIterable.first()).thenReturn(mockKeyDoc);

        // 3. Perform Request
        mockMvc.perform(get("/api/admin/keys")
                .header("X-ADMIN-KEY", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("local"));
    }
}