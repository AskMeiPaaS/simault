package com.ayedata.jvault.service;

import com.ayedata.jvault.model.AppSecret;
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

    // Properties injected from application.properties
    @Value("${vault.mongodb.uri}") private String uri;
    @Value("${vault.mongodb.database}") private String dbName;
    @Value("${vault.mongodb.collection}") private String collName;
    @Value("${vault.security.kms-provider}") private String kmsProviderName;
    @Value("${vault.security.master-key-path}") private String masterKeyPath;
    @Value("${vault.security.key-alt-name}") private String keyAltName;

    private MongoClient regularClient;
    private ClientEncryption clientEncryption;
    private UUID dataKeyId;

    @PostConstruct
    public void init() {
        System.out.println("⚙️ Initializing SecretVaultService...");

        // 1. Initialize Standard Client (for reading/writing secrets)
        this.regularClient = MongoClients.create(uri);

        // 2. Ensure KeyVault Index exists (Required for key lookups)
        MongoCollection<Document> keyVaultColl = regularClient.getDatabase("encryption").getCollection("__keyVault");
        keyVaultColl.createIndex(Indexes.ascending("keyAltNames"), 
            new IndexOptions().unique(true).partialFilterExpression(Filters.exists("keyAltNames")));

        // 3. Ensure TTL Index on Secrets Collection (Expires after 1 Hour)
        MongoCollection<Document> secretColl = regularClient.getDatabase(dbName).getCollection(collName);
        secretColl.createIndex(Indexes.ascending("createdAt"), 
            new IndexOptions().expireAfter(3600L, TimeUnit.SECONDS));

        // 4. Load Master Key from File
        Map<String, Map<String, Object>> kmsProviders = loadMasterKey(masterKeyPath);

        // 5. Setup Client Encryption (The core encryption engine)
        ClientEncryptionSettings encryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri)).build())
                .keyVaultNamespace("encryption.__keyVault")
                .kmsProviders(kmsProviders)
                .build();
        this.clientEncryption = ClientEncryptions.create(encryptionSettings);

        // 6. Get or Create the Data Key
        this.dataKeyId = ensureDataKeyExists();
        
        System.out.println("✅ SecretVaultService Ready. Data Key ID: " + dataKeyId);
    }

    private UUID ensureDataKeyExists() {
        // Try to find an existing key by its Alt Name
        Document query = new Document("keyAltNames", keyAltName);
        Document keyDoc = regularClient.getDatabase("encryption").getCollection("__keyVault").find(query).first();

        if (keyDoc != null) {
            // FIX: Convert MongoDB Binary to Java UUID
            Binary bsonBinary = keyDoc.get("_id", Binary.class);
            ByteBuffer buffer = ByteBuffer.wrap(bsonBinary.getData());
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        
        System.out.println("⚠️ Data Key not found. Generating new one...");
        
        // Create new Data Key
        BsonBinary newKeyId = clientEncryption.createDataKey(kmsProviderName, 
                new DataKeyOptions().keyAltNames(List.of(keyAltName)));
        
        return newKeyId.asUuid();
    }

    public AppSecret getAppSecret(String appId) {
        MongoCollection<Document> coll = regularClient.getDatabase(dbName).getCollection(collName);
        Document doc = coll.find(new Document("appId", appId)).first();

        // If secret is missing or expired (TTL), rotate (create new)
        if (doc == null) {
            System.out.println("🔄 Secret for " + appId + " missing/expired. Rotating...");
            return rotateSecret(appId);
        }

        // Decrypt Logic
        Binary encryptedData = doc.get("secret", Binary.class);

        // FIX: Wrap Binary in BsonBinary for decrypt()
        String decryptedSecret = clientEncryption.decrypt(
            new BsonBinary(encryptedData.getType(), encryptedData.getData())
        ).asString().getValue();

        return new AppSecret(appId, decryptedSecret, doc.getDate("createdAt").toInstant());
    }

    public AppSecret rotateSecret(String appId) {
        String newSecretRaw = generateRandomString();

        // Encrypt Logic
        // FIX: Wrap UUID in BsonBinary for keyId()
        BsonBinary encryptedBson = clientEncryption.encrypt(
            new BsonString(newSecretRaw),
            new EncryptOptions("AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic")
                .keyId(new BsonBinary(dataKeyId))
        );

        // FIX: Convert BsonBinary -> Binary for storage
        Binary encryptedStandard = new Binary(encryptedBson.getType(), encryptedBson.getData());

        Document secretDoc = new Document()
                .append("appId", appId)
                .append("secret", encryptedStandard)
                .append("createdAt", Date.from(Instant.now()));

        MongoCollection<Document> coll = regularClient.getDatabase(dbName).getCollection(collName);
        
        // Upsert: Delete existing (if any) then insert new to reset TTL cleanly
        coll.deleteOne(new Document("appId", appId)); 
        coll.insertOne(secretDoc);

        return new AppSecret(appId, newSecretRaw, Instant.now());
    }

    private Map<String, Map<String, Object>> loadMasterKey(String path) {
        byte[] localMasterKey = new byte[96];
        
        File file = new File(path);
        System.out.println("🔑 Loading Master Key from: " + file.getAbsolutePath());

        try (FileInputStream fis = new FileInputStream(file)) {
            if (fis.read(localMasterKey) < 96) {
                throw new RuntimeException("❌ Master Key file is too short! It must be 96 bytes.");
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to load Master Key from '" + file.getAbsolutePath() + "'. \n" +
                    "Run 'openssl rand -out master-key.txt 96' in the project root.", e);
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
        System.out.println("🛑 Closing Vault Service...");
        if (clientEncryption != null) clientEncryption.close();
        if (regularClient != null) regularClient.close();
    }
}