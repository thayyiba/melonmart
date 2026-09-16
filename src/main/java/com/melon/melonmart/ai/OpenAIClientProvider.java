package com.melon.melonmart.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

public class OpenAIClientProvider {

    private static final OpenAIClient CLIENT =
            OpenAIOkHttpClient.fromEnv();

    public static OpenAIClient getClient() {
        return CLIENT;
    }
}