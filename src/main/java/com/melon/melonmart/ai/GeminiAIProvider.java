package com.melon.melonmart.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (!enabled) return offlineReply(message, productContext);
        try {
            String prompt = """
                    You are Welp, the shopping assistant for MelonMart.
                    Keep replies short, friendly and useful. Never invent products, prices, discounts, stock, shipping rules, or order status.
                    Use only the product information provided. If the catalog does not specify a discount, say so rather than inventing an offer.
                    For cart changes, explain that the user can say "add [quantity] [product] to cart" or "remove [product] from cart".
                    For checkout, explain that the user should open the cart and complete the delivery and payment details.
                    User message: %s
                    Available product information: %s
                    """.formatted(message, productContext);
            GenerateContentResponse response = client.models.generateContent("gemini-3.6-flash", prompt, null);
            String text = response.text();
            return text == null || text.isBlank()
                    ? offlineReply(message, productContext)
                    : text;
        } catch (Exception e) {
            return offlineReply(message, productContext);
        }
    }

    /*
     * Useful catalogue-aware replies keep Welp helpful even when no Gemini API key
     * is configured or the external AI service is temporarily unavailable.
     */
    private String offlineReply(String message, String productContext) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "hello", "hi", "hey", "good morning", "good evening")) {
            return "Hey! 🍉 I’m Welp, your MelonMart shopping helper. Ask me to find a product, suggest something for tea-time, show budget-friendly picks, or help with your cart and checkout.";
        }
        if (containsAny(lower, "help", "what can you do", "how to use")) {
            return "I can help you find products, compare listed prices, suggest tea-time/snack picks, and explain cart or checkout. Try “show tea-time snacks”, “cheapest drinks”, or “add 2 [product name] to cart”. I only show discounts if the catalogue actually lists them.";
        }
        if (containsAny(lower, "checkout", "payment", "delivery", "place order", "order")) {
            return "To place an order, open your cart, check the quantities, then continue to checkout and complete the contact, delivery and payment fields. Your order history is available from your account after placing the order. 🍉";
        }
        if (containsAny(lower, "cart", "basket")) {
            return "I can update your cart when you name the item: try “add 2 [product name] to cart” or “remove [product name] from cart”. You need to be logged in, and I’ll check the available stock. 🛒";
        }

        List<CatalogItem> items = parseCatalog(productContext);
        boolean asksForOffer = containsAny(lower, "offer", "offers", "deal", "deals", "discount", "sale", "cheap", "cheapest", "budget");
        boolean asksForTeaTime = containsAny(lower, "tea", "tea-time", "teatime", "snack", "snacks", "tonight", "evening");
        List<String> terms = searchTerms(lower);

        List<CatalogItem> matches = new ArrayList<>();
        for (CatalogItem item : items) {
            String searchable = (item.name + " " + item.description + " " + item.category)
                    .toLowerCase(Locale.ROOT);
            boolean matchesTerms = terms.isEmpty() || terms.stream().anyMatch(searchable::contains);
            boolean inStock = item.stock > 0;
            if (inStock && matchesTerms) matches.add(item);
        }

        if (asksForOffer || asksForTeaTime || containsAny(lower, "recommend", "suggest", "find", "show", "looking for", "product", "products")) {
            if (!matches.isEmpty()) {
                matches.sort(Comparator.comparing(item -> item.price));
                StringBuilder reply = new StringBuilder();
                if (asksForOffer) {
                    reply.append("Here are budget-friendly matches from the current catalogue 🍉. I can’t see a separate discount/offer field, so these are listed prices—not claimed discounts:\n");
                } else if (asksForTeaTime) {
                    reply.append("Tea-time / evening picks available in the catalogue 🍵:\n");
                } else {
                    reply.append("Here are matching products currently in stock 🍉:\n");
                }
                int limit = Math.min(5, matches.size());
                for (int i = 0; i < limit; i++) {
                    CatalogItem item = matches.get(i);
                    reply.append("• ").append(item.name).append(" — ₹").append(item.price);
                    if (!item.category.isBlank()) reply.append(" (").append(item.category).append(")");
                    reply.append("\n");
                }
                reply.append("To add one, say “add [quantity] [exact product name] to cart”.");
                return reply.toString();
            }
            if (asksForOffer) {
                return "I can help find lower-priced items, but this catalogue doesn’t provide verified discount amounts. Try “show budget-friendly snacks” or “show drinks” and I’ll list current prices.";
            }
            return "I couldn’t find an in-stock match for that wording. Try a shorter product name or ask me to show snacks, drinks, or budget-friendly picks.";
        }

        return "I can still help using MelonMart’s live catalogue, even without the external AI service. Ask me to find a product, suggest tea-time snacks, compare listed prices, or help with your cart/checkout. 🍉";
    }

    private List<CatalogItem> parseCatalog(String context) {
        List<CatalogItem> items = new ArrayList<>();
        if (context == null || context.isBlank()) return items;
        for (String block : context.split("\\R\\s*\\R")) {
            String name = field(block, "Product");
            String price = field(block, "Price").replace("₹", "").trim();
            if (name.isBlank() || price.isBlank()) continue;
            try {
                int stock = Integer.parseInt(field(block, "Stock").trim());
                items.add(new CatalogItem(name, field(block, "Description"),
                        field(block, "Category"), new java.math.BigDecimal(price), stock));
            } catch (RuntimeException ignored) {
                // Skip malformed catalog rows rather than making up details.
            }
        }
        return items;
    }

    private String field(String block, String label) {
        Matcher matcher = Pattern.compile("(?m)^" + Pattern.quote(label) + ":\\s*(.*)$").matcher(block);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private List<String> searchTerms(String lower) {
        Set<String> stopWords = Set.of("a", "an", "the", "me", "my", "for", "to", "of", "in",
                "on", "at", "is", "are", "show", "find", "get", "give", "some", "please",
                "i", "want", "need", "looking", "with", "under", "over", "tonight", "today",
                "offer", "offers", "deal", "deals", "discount", "discounts", "cheap", "cheapest",
                "budget", "friendly", "tea", "time", "tea-time", "teatime", "snack", "snacks",
                "recommend", "suggest", "product", "products", "available", "in-stock");
        List<String> terms = new ArrayList<>();
        for (String token : lower.replaceAll("[^a-z0-9 -]", " ").split("\\s+")) {
            if (token.length() > 1 && !stopWords.contains(token)) terms.add(token);
        }
        return terms;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static final class CatalogItem {
        final String name;
        final String description;
        final String category;
        final java.math.BigDecimal price;
        final int stock;

        CatalogItem(String name, String description, String category,
                    java.math.BigDecimal price, int stock) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.price = price;
            this.stock = stock;
        }
    }
}
