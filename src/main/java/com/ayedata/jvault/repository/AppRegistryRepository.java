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
    
    @Value("${vault.mongodb.database}") 
    private String dbName;
    
    private static final String COLLECTION_NAME = "allowed_apps";

    public AppRegistryRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @PostConstruct
    public void init() {
        // Create a unique index on 'appId' to prevent duplicates
        getCollection().createIndex(
            Indexes.ascending("appId"), 
            new IndexOptions().unique(true)
        );
    }

    private MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase(dbName).getCollection(COLLECTION_NAME);
    }

    /**
     * Registers an app. If it exists, it updates the description.
     */
    public void registerApp(String appId, String description) {
        Document doc = new Document()
                .append("appId", appId)
                .append("description", description)
                .append("registeredAt", Date.from(Instant.now()));

        // Use replaceOne with upsert=true. 
        // This acts as "Insert if new, Update if exists".
        getCollection().replaceOne(
                Filters.eq("appId", appId), 
                doc, 
                new ReplaceOptions().upsert(true)
        );
    }

    /**
     * Checks if an app is in the allowed list.
     */
    public boolean isAppAllowed(String appId) {
        return getCollection().countDocuments(Filters.eq("appId", appId)) > 0;
    }

    /**
     * Fetches all registered apps from the database.
     */
    public List<AllowedApp> findAll() {
        List<AllowedApp> apps = new ArrayList<>();
        
        for (Document doc : getCollection().find()) {
            apps.add(new AllowedApp(
                doc.getString("appId"),
                doc.getString("description"),
                doc.getDate("registeredAt") != null ? doc.getDate("registeredAt").toInstant() : null
            ));
        }
        return apps;
    }

    /**
     * Removes an app from the allowed list.
     */
    public void removeApp(String appId) {
        getCollection().deleteOne(Filters.eq("appId", appId));
    }
}