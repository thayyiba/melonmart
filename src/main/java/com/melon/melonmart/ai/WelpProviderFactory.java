package com.melon.melonmart.ai;

/**
 * Creates the configured chat provider.
 *
 * <p>Set the system property {@code ai.chatbot.provider} or environment
 * variable {@code MELONMART_CHAT_PROVIDER} to {@code mock} or {@code gemini}.
 * Gemini remains the default for backwards compatibility.</p>
 */
public final class WelpProviderFactory {

    private static final String CONFIG_PROPERTY = "ai.chatbot.provider";
    private static final String CONFIG_ENV = "MELONMART_CHAT_PROVIDER";

    private WelpProviderFactory() {
    }

    public static WelpAIProvider create() {
        String configured = System.getProperty(CONFIG_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(CONFIG_ENV);
        }
        String provider = configured == null || configured.isBlank()
                ? "gemini"
                : configured.trim().toLowerCase(java.util.Locale.ROOT);

        switch (provider) {
            case "mock":
                return new MockAIProvider();
            case "gemini":
                return new GeminiAIProvider();
            default:
                throw new IllegalArgumentException(
                        "Unsupported Welp provider. Use 'gemini' or 'mock'.");
        }
    }
}
