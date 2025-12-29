package com.ayedata.simault.logging;

import org.junit.jupiter.api.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogRedactionTest {

    // This must match the Regex in your log4j2.xml exactly
    private static final String LOG_REGEX = "(?i)(Bearer\\s+|token=|password=|key\"\\s*:\\s*\")([a-zA-Z0-9\\._-]+)";
    private static final String REPLACEMENT = "$1*****REDACTED*****";
    private final Pattern pattern = Pattern.compile(LOG_REGEX);

    @Test
    void testBearerTokenRedaction() {
        String log = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String redacted = log.replaceAll(LOG_REGEX, REPLACEMENT);
        
        assertEquals("Authorization: Bearer *****REDACTED*****", redacted);
    }

    @Test
    void testJsonSecretRedaction() {
        String log = "Payload: {\"id\": 1, \"key\":\"sk_live_123456\"}";
        String redacted = log.replaceAll(LOG_REGEX, REPLACEMENT);
        
        assertEquals("Payload: {\"id\": 1, \"key\":\"*****REDACTED*****\"}", redacted);
    }

    @Test
    void testUrlParamRedaction() {
        String log = "Request to /api?token=secret123&force=true";
        String redacted = log.replaceAll(LOG_REGEX, REPLACEMENT);
        
        assertEquals("Request to /api?token=*****REDACTED*****&force=true", redacted);
    }
}