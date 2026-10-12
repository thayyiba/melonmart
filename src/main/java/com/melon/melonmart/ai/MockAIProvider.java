package com.melon.melonmart.ai;

import java.util.Locale;

/**
 * Deterministic local provider for demos, CI, and offline development.
 * It never calls an external AI service.
 */
public class MockAIProvider implements WelpAIProvider {

    @Override
    public String generateReply(String message, String productContext) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (normalized.contains("price")
                || normalized.contains("stock")
                || normalized.contains("product")
                || normalized.contains("fruit")
                || normalized.contains("snack")
                || normalized.contains("drink")
                || normalized.contains("juice")) {
            if (productContext == null || productContext.isBlank()) {
                return "I couldn't find any current product details. Please check the product catalogue.";
            }
            String context = productContext.length() > 900
                    ? productContext.substring(0, 900) + "…"
                    : productContext;
            return "Here is the current catalogue information from Melon Mart:\n" + context;
        }

        return "I'm Welp, the Melon Mart shopping helper. I can help with catalogue items, prices, stock, your cart, checkout, and order history. For current prices or stock, ask about a product from the catalogue.";
    }
}
