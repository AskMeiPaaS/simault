package com.ayedata.jvault.service;

import com.ayedata.jvault.model.AppSecret;
import com.ayedata.jvault.repository.AppRegistryRepository;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.model.vault.EncryptOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.BsonBinary;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SecretVaultService {

    // --- Configuration ---
    @Value("${vault.mongodb.uri}") private String uri;
    @Value("${vault.mongodb.database}") private String dbName;
    @Value("${vault.mongodb.collection}") private String collName;
    @Value("${vault.security.kms-provider}") private String kmsProviderName;
    @Value("${vault.security.master-key-path}") private String masterKeyPath;
    @Value("${vault.security.key-alt-name}") private String keyAltName;

    // --- Dependencies ---
    private final AppRegistryRepository appRegistry;
    private MongoClient regularClient;
    private ClientEncryption clientEncryption;
    private UUID dataKeyId;

    public SecretVaultService(AppRegistryRepository appRegistry) {
        this.appRegistry = appRegistry;
    }

    @PostConstruct
    public void init() {
        System.out.println("⚙️ Initializing SecretVaultService...");
        this.regularClient = MongoClients.create(uri);

        // Ensure Indexes
        MongoCollection<Document> keyVaultColl = regularClient.getDatabase("encryption").getCollection("__keyVault");
        keyVaultColl.createIndex(Indexes.ascending("keyAltNames"), 
            new IndexOptions().unique(true).partialFilterExpression(Filters.exists("keyAltNames")));

        MongoCollection<Document> secretColl = regularClient.getDatabase(dbName).getCollection(collName);
        secretColl.createIndex(Indexes.ascending("createdAt"), 
            new IndexOptions().expireAfter(3600L, TimeUnit.SECONDS));

        // Setup Encryption
        Map<String, Map<String, Object>> kmsProviders = loadMasterKey(masterKeyPath);
        ClientEncryptionSettings encryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri)).build())
                .keyVaultNamespace("encryption.__keyVault")
                .kmsProviders(kmsProviders)
                .build();
        this.clientEncryption = ClientEncryptions.create(encryptionSettings);

        this.dataKeyId = ensureDataKeyExists();
        System.out.println("✅ SecretVaultService Ready.");
    }

    public AppSecret getAppSecret(String appId) {
        // 🛑 STRICT DATABASE CHECK
        // We do NOT check properties files. We ONLY check MongoDB via the Repository.
        if (!appRegistry.isAppAllowed(appId)) {
            System.out.println("⛔ BLOCKING request for unregistered app: " + appId);
            throw new IllegalArgumentException("❌ Access Denied: Application '" + appId + "' is not registered. Please contact Admin.");
        }

        MongoCollection<Document> coll = regularClient.getDatabase(dbName).getCollection(collName);
        Document doc = coll.find(new Document("appId", appId)).first();

        if (doc == null) {
            return rotateSecret(appId);
        }

        try {
            Binary encryptedData = doc.get("secret", Binary.class);
            String decryptedSecret = clientEncryption.decrypt(
                new BsonBinary(encryptedData.getType(), encryptedData.getData())
            ).asString().getValue();
            return new AppSecret(appId, decryptedSecret, doc.getDate("createdAt").toInstant());
        } catch (Exception e) {
            return rotateSecret(appId);
        }
    }

    public AppSecret rotateSecret(String appId) {
        // 🛑 STRICT DATABASE CHECK
        if (!appRegistry.isAppAllowed(appId)) {
            throw new IllegalArgumentException("❌ Access Denied: Application '" + appId + "' is not registered.");
        }

        String newSecretRaw = generateRandomString();

        BsonBinary encryptedBson = clientEncryption.encrypt(
            new BsonString(newSecretRaw),
            new EncryptOptions("AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic")
                .keyId(new BsonBinary(dataKeyId))
        );
        Binary encryptedStandard = new Binary(encryptedBson.getType(), encryptedBson.getData());

        Document secretDoc = new Document()
                .append("appId", appId)
                .append("secret", encryptedStandard)
                .append("createdAt", Date.from(Instant.now()));

        MongoCollection<Document> coll = regularClient.getDatabase(dbName).getCollection(collName);
        coll.deleteOne(new Document("appId", appId)); 
        coll.insertOne(secretDoc);

        return new AppSecret(appId, newSecretRaw, Instant.now());
    }

    private UUID ensureDataKeyExists() {
        Document query = new Document("keyAltNames", keyAltName);
        Document keyDoc = regularClient.getDatabase("encryption").getCollection("__keyVault").find(query).first();

        if (keyDoc != null) {
            Binary bsonBinary = keyDoc.get("_id", Binary.class);
            ByteBuffer buffer = ByteBuffer.wrap(bsonBinary.getData());
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        
        System.out.println("⚠️ Creating new Data Key...");
        return clientEncryption.createDataKey(kmsProviderName, 
                new DataKeyOptions().keyAltNames(List.of(keyAltName))).asUuid();
    }

    private Map<String, Map<String, Object>> loadMasterKey(String path) {
        byte[] localMasterKey = new byte[96];
        try (FileInputStream fis = new FileInputStream(new File(path))) {
            if (fis.read(localMasterKey) < 96) throw new RuntimeException("Key too short!");
        } catch (IOException e) {
            throw new RuntimeException("Cannot load master key from: " + path, e);
        }
        Map<String, Map<String, Object>> kms = new HashMap<>();
        kms.put(kmsProviderName, Map.of("key", localMasterKey));
        return kms;
    }

    private String generateRandomString() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @PreDestroy
    public void close() {
        if (clientEncryption != null) clientEncryption.close();
        if (regularClient != null) regularClient.close();
    }
}