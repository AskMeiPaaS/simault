package com.ayedata.simault.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class KeyUtils {
    public static Map<String, Map<String, Object>> loadMasterKey(String path, String providerName) {
        byte[] localMasterKey = new byte[96];
        try (FileInputStream fis = new FileInputStream(path)) {
            if (fis.read(localMasterKey) < 96) throw new RuntimeException("Key too short!");
        } catch (IOException e) {
            throw new RuntimeException("Could not read master key: " + path);
        }
        Map<String, Map<String, Object>> kms = new HashMap<>();
        kms.put(providerName, Map.of("key", localMasterKey));
        return kms;
    }
}