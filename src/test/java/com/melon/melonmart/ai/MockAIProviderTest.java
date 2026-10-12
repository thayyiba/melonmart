package com.melon.melonmart.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAIProviderTest {

    @Test
    void answersMarketplaceQuestionsWithoutExternalProvider() {
        MockAIProvider provider = new MockAIProvider();

        String reply = provider.generateReply(
                "What products are available?",
                "Product: Fresh Apples\nPrice: ₹120.00\nStock: 8");

        assertTrue(reply.contains("Fresh Apples"));
        assertTrue(reply.contains("₹120.00"));
    }

    @Test
    void staysWithinMarketplaceHelpForGeneralQuestions() {
        MockAIProvider provider = new MockAIProvider();

        String reply = provider.generateReply("How can you help?", "");

        assertTrue(reply.contains("Melon Mart"));
        assertTrue(reply.contains("checkout"));
    }
}
