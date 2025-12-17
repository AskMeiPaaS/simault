package com.ayedata.jvault.controller;

import com.ayedata.jvault.model.AppSecret;
import com.ayedata.jvault.service.SecretService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secrets")
public class SecretController {

    private final SecretService secretService;

    public SecretController(SecretService secretService) {
        this.secretService = secretService;
    }

    @GetMapping("/{appId}")
    public ResponseEntity<AppSecret> getSecret(@PathVariable String appId) {
        return ResponseEntity.ok(secretService.getAppSecret(appId));
    }

    @PostMapping("/{appId}/rotate")
    public ResponseEntity<AppSecret> rotateSecret(@PathVariable String appId) {
        return ResponseEntity.ok(secretService.rotateSecret(appId));
    }
}
