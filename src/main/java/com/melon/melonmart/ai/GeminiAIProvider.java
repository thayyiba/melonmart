package com.melon.melonmart.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GeminiAIProvider implements WelpAIProvider {

    private final Client client;

    public GeminiAIProvider() {
        this.client = Client.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();
    }

    @Override
    public String generateReply(String message, String productContext) {

        String prompt = """
                You are Welp, the shopping assistant for Melon Mart.

                User message:
                %s

                Available product information:
                %s

                Answer naturally and helpfully.
                Only recommend products that appear in the provided product information.
                """.formatted(message, productContext);

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-3.6-flash",
                        prompt,
                        null
                );

        return response.text();
    }
}