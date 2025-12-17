package com.ayedata.jvault.repository;

import com.ayedata.jvault.model.AllowedApp;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class AppRegistryRepository {

    private final MongoClient mongoClient;
    
    // Inject database name from application.properties
    @Value("${vault.mongodb.database}") 
    private String dbName;
    
    private static final String COLLECTION_NAME = "allowed_apps";

    public AppRegistryRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    /**
     * Initialization: Runs automatically on startup.
     * Ensures the 'appId' index exists and is unique.
     */
    @PostConstruct
    public void init() {
        getCollection().createIndex(
            Indexes.ascending("appId"), 
            new IndexOptions().unique(true)
        );
    }

    // Helper to get the collection connection
    private MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase(dbName).getCollection(COLLECTION_NAME);
    }

    /**
     * Registers a new app or updates an existing one (Upsert).
     * * @param appId The unique identifier for the application (e.g., "payment-service")
     * @param description A human-readable description
     */
    public void registerApp(String appId, String description) {
        Document doc = new Document()
                .append("appId", appId)
                .append("description", description)
                .append("registeredAt", Date.from(Instant.now()));

        // replaceOne with upsert=true acts as "Insert or Update"
        getCollection().replaceOne(
                Filters.eq("appId", appId), 
                doc, 
                new ReplaceOptions().upsert(true)
        );
    }

    /**
     * Checks if an application is currently authorized.
     * * @param appId The application ID to check
     * @return true if allowed, false otherwise
     */
    public boolean isAppAllowed(String appId) {
        return getCollection().countDocuments(Filters.eq("appId", appId)) > 0;
    }

    /**
     * Retrieves all registered applications.
     * * @return List of AllowedApp model objects
     */
    public List<AllowedApp> findAll() {
        List<AllowedApp> apps = new ArrayList<>();
        
        for (Document doc : getCollection().find()) {
            apps.add(new AllowedApp(
                doc.getString("appId"),
                doc.getString("description"),
                // Handle potential null dates safely
                doc.getDate("registeredAt") != null ? doc.getDate("registeredAt").toInstant() : null
            ));
        }
        return apps;
    }

    /**
     * Removes an application from the registry.
     * This immediately revokes its access to get or rotate secrets.
     * * @param appId The application ID to remove
     */
    public void removeApp(String appId) {
        getCollection().deleteOne(Filters.eq("appId", appId));
    }
}