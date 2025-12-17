package com.ayedata.simault.service;

import com.ayedata.simault.model.AppSecret;
import com.ayedata.simault.repository.SecretRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class SecretService {

    private final SecretRepository secretRepository;

    public SecretService(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    public AppSecret getAppSecret(String appId) {
        AppSecret secret = secretRepository.findByAppId(appId);

        if (secret == null) {
            System.out.println("🔄 Secret for " + appId + " missing/expired. Rotating...");
            return rotateSecret(appId);
        }
        return secret;
    }

    public AppSecret rotateSecret(String appId) {
        String newSecret = generateRandomString();
        secretRepository.save(appId, newSecret);
        return new AppSecret(appId, newSecret, Instant.now());
    }

    private String generateRandomString() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}