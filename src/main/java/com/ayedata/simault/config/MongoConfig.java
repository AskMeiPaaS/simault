package com.ayedata.simault.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig {

    private final MongoClient mongoClient;

    @Value("${vault.mongodb.database}")
    private String dbName;

    @Value("${vault.mongodb.collection}")
    private String secretsCollName;

    public MongoConfig(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @PostConstruct
    public void configureIndexes() {
        System.out.println("⚙️ Configuring MongoDB Indexes...");

        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoDatabase encryptionDb = mongoClient.getDatabase("encryption");

        // =================================================================
        // 1. KEY VAULT COLLECTION (__keyVault)
        // =================================================================
        // Critical for CSFLE: Ensures unique key names
        MongoCollection<Document> keyVaultColl = encryptionDb.getCollection("__keyVault");
        keyVaultColl.createIndex(
            Indexes.ascending("keyAltNames"),
            new IndexOptions().unique(true).partialFilterExpression(Filters.exists("keyAltNames"))
        );

        // =================================================================
        // 2. SECRETS COLLECTION
        // =================================================================
        MongoCollection<Document> secretsColl = db.getCollection(secretsCollName);

        // [TTL Index] Auto-expire secrets after 1 hour (3600 seconds)
        secretsColl.createIndex(
            Indexes.ascending("createdAt"),
            new IndexOptions().expireAfter(3600L, TimeUnit.SECONDS)
        );

        // [Performance Index] Fast lookup by App ID (Critical for getSecret)
        secretsColl.createIndex(
            Indexes.ascending("appId"),
            new IndexOptions().background(true)
        );

        // =================================================================
        // 3. ALLOWED APPS COLLECTION (Registry)
        // =================================================================
        MongoCollection<Document> appsColl = db.getCollection("allowed_apps");

        // [Unique Index] Prevent duplicate registrations
        appsColl.createIndex(
            Indexes.ascending("appId"),
            new IndexOptions().unique(true)
        );

        // [Sort Index] Efficiently list apps by registration date (Admin Dashboard)
        appsColl.createIndex(
            Indexes.descending("registeredAt")
        );

        // [Text Index] Enable fuzzy search by description (for AI/MCP Search)
        appsColl.createIndex(
            Indexes.text("description")
        );

        System.out.println("✅ All MongoDB Indexes Configured Successfully.");
    }
}