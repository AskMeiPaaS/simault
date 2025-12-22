package com.ayedata.simault.mcp.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // 1. Initialize Log4j 2 Logger
    private static final Logger logger = LogManager.getLogger(ChatController.class);

    private final ChatClient chatClient;

    // We inject the ChatClient.Builder to construct a client with default system prompts if needed,
    // or inject a pre-configured ChatClient if you defined one in a Config class.
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are a Vault Admin AI. You help manage application access and monitor secret health. " +
                               "Use the provided tools to perform actions on the database. " +
                               "If a user asks to register an app, you MUST use the registerNewApp tool.")
                .build();
    }

    @PostMapping
    public String chat(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("message");
        
        // Log the incoming user message (Log4j will redact if they accidentally paste a token)
        logger.info("Incoming Chat Request: {}", userMessage);

        try {
            String response = chatClient.prompt()
                    .user(userMessage)
                    // 2. ENABLE THE TOOLS HERE
                    // These strings must match the method names annotated with @Tool 
                    // in your AdminRegistryTools and SecretHealthTools classes.
                    .tools("listAllowedApps", "registerNewApp", "removeApp", "checkSecretHealth","getSecret", "rotateSecret")
                    .call()
                    .content();

            logger.info("AI Response generated successfully.");
            return response;

        } catch (Exception e) {
            logger.error("Error during AI Chat processing", e);
            return "⚠️ I encountered an error while processing your request.";
        }
    }
}