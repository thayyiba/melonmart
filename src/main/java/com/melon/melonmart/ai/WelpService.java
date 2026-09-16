package com.melon.melonmart.ai;

public class WelpService {

    private final WelpAIProvider aiProvider;

    public WelpService(WelpAIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String chat(String message, String productContext) {
        return aiProvider.generateReply(message, productContext);
    }
}