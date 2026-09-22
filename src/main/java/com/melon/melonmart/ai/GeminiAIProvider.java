package com.melon.melonmart.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GeminiAIProvider implements WelpAIProvider {
    private final Client client;
    private final boolean enabled;

    public GeminiAIProvider() {
        String key = System.getenv("GEMINI_API_KEY");
        enabled = key != null && !key.isBlank();
        client = enabled ? Client.builder().apiKey(key).build() : null;
    }

    @Override
    public String generateReply(String message, String productContext) {
        if (!enabled) return "I’m your MelonMart helper 🍉. Try asking about fruits, drinks, snacks, cart, or checkout. AI is running in demo mode right now.";
        try {
            String prompt = """
                    You are Welp, the shopping assistant for MelonMart.
                    Keep replies short, friendly and useful. Never invent products, prices or stock.
                    User message: %s
                    Available product information: %s
                    """.formatted(message, productContext);
            GenerateContentResponse response = client.models.generateContent("gemini-3.6-flash", prompt, null);
            String text = response.text();
            return text == null || text.isBlank() ? "I couldn't find a helpful answer. Try asking about a product or your cart." : text;
        } catch (Exception e) {
            return "Welp is temporarily in demo mode. I can still help you browse products, add items to cart, and place a mock order.";
        }
    }
}
