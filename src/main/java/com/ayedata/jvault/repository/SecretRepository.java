package com.ayedata.jvault.repository;

import com.ayedata.jvault.model.AppSecret;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.vault.EncryptOptions;
import com.mongodb.client.vault.ClientEncryption;
import org.bson.BsonBinary;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Repository
public class SecretRepository {

    private final MongoClient mongoClient;
    private final ClientEncryption clientEncryption;
    private final UUID dataKeyId;

    @Value("${vault.mongodb.database}") private String dbName;
    @Value("${vault.mongodb.collection}") private String collName;

    public SecretRepository(MongoClient mongoClient, ClientEncryption clientEncryption, UUID dataKeyId) {
        this.mongoClient = mongoClient;
        this.clientEncryption = clientEncryption;
        this.dataKeyId = dataKeyId;
    }

    public AppSecret findByAppId(String appId) {
        MongoCollection<Document> coll = mongoClient.getDatabase(dbName).getCollection(collName);
        Document doc = coll.find(new Document("appId", appId)).first();

        if (doc == null) return null;

        Binary encryptedData = doc.get("secret", Binary.class);

        // Fix 1: Decrypt requires BsonBinary, not Binary
        String decryptedSecret = clientEncryption.decrypt(
            new BsonBinary(encryptedData.getType(), encryptedData.getData())
        ).asString().getValue();

        return new AppSecret(appId, decryptedSecret, doc.getDate("createdAt").toInstant());
    }

    public void save(String appId, String rawSecret) {
        // Fix 2: keyId requires BsonBinary, not UUID
        BsonBinary keyIdBson = new BsonBinary(dataKeyId);

        BsonBinary encryptedSecret = clientEncryption.encrypt(
            new BsonString(rawSecret),
            new EncryptOptions("AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic").keyId(keyIdBson)
        );

        Document secretDoc = new Document()
                .append("appId", appId)
                .append("secret", encryptedSecret)
                .append("createdAt", Date.from(Instant.now()));

        MongoCollection<Document> coll = mongoClient.getDatabase(dbName).getCollection(collName);
        
        coll.deleteOne(new Document("appId", appId));
        coll.insertOne(secretDoc);
    }
}