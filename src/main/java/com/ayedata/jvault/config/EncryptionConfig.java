package com.ayedata.jvault.config;

import com.ayedata.jvault.util.KeyUtils;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import org.bson.Document;
import org.bson.types.Binary;
import java.nio.ByteBuffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class EncryptionConfig {

    @Value("${vault.mongodb.uri}")
    private String uri;
    @Value("${vault.security.kms-provider}")
    private String kmsProviderName;
    @Value("${vault.security.master-key-path}")
    private String masterKeyPath;
    @Value("${vault.security.key-alt-name}")
    private String keyAltName;

    @Bean
    public ClientEncryption clientEncryption() {
        Map<String, Map<String, Object>> kmsProviders = KeyUtils.loadMasterKey(masterKeyPath, kmsProviderName);

        ClientEncryptionSettings encryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(
                        MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri)).build())
                .keyVaultNamespace("encryption.__keyVault")
                .kmsProviders(kmsProviders)
                .build();

        return ClientEncryptions.create(encryptionSettings);
    }

    @Bean
    public UUID dataKeyId(MongoClient regularClient, ClientEncryption clientEncryption) {
        // Ensure KeyVault Index
        regularClient.getDatabase("encryption").getCollection("__keyVault")
                .createIndex(Indexes.ascending("keyAltNames"),
                        new IndexOptions().unique(true).partialFilterExpression(Filters.exists("keyAltNames")));

        // Find or Create Data Key
        Document query = new Document("keyAltNames", keyAltName);
        Document keyDoc = regularClient.getDatabase("encryption").getCollection("__keyVault").find(query).first();

        if (keyDoc != null) {
            // FIX: Get as Binary first, then convert to UUID
            Binary bsonBinary = keyDoc.get("_id", Binary.class);
            ByteBuffer buffer = ByteBuffer.wrap(bsonBinary.getData());
            return new UUID(buffer.getLong(), buffer.getLong());
        }

        System.out.println("⚠️ Creating new Data Key...");
        return clientEncryption.createDataKey(kmsProviderName,
                new DataKeyOptions().keyAltNames(List.of(keyAltName))).asUuid();
    }
}