package com.ayedata.jvault.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig {

    @Value("${vault.mongodb.uri}") private String uri;
    @Value("${vault.mongodb.database}") private String dbName;
    @Value("${vault.mongodb.collection}") private String collName;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
    }

    @PostConstruct
    public void initIndexes() {
        try (MongoClient client = MongoClients.create(uri)) {
            // 1. Ensure TTL Index for Secrets (1 Hour)
            MongoCollection<Document> secretColl = client.getDatabase(dbName).getCollection(collName);
            secretColl.createIndex(Indexes.ascending("createdAt"), 
                new IndexOptions().expireAfter(3600L, TimeUnit.SECONDS));
            
            System.out.println("✅ Mongo Indexes Configured.");
        }
    }
}