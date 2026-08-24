package org.jmouse.ai.provider;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Messages API, which the canonical types already look like.
 *
 * <p>Nearly a pass-through, and that is not an accident: the canonical shape was chosen to be this one
 * because a tool-calling conversation is expressed more directly here than anywhere else — content is a
 * list of blocks, a tool call is a block, a tool result is a block, and the whole conversation is one
 * array that grows. {@link OpenAiChatModel} is what proves the shape is canonical rather than this
 * provider wearing a hat.
 *
 * <p>⚠️ Which means a bug here is invisible and a bug there is obvious. Everything this class does not
 * translate, it forwards, so a message shape that is wrong arrives at the provider unexamined.
 *
 * <p>⚠️ <strong>An image is one of the shapes forwarded, and the absence of any code for it here is the
 * design rather than an omission.</strong> {@link ContentBlock#IMAGE} <em>is</em> this provider's block,
 * so a picture attached to a question and one handed back by a tool both arrive already correct. Anyone
 * looking for the translation will not find it; there is nothing to translate.
 */
public final class AnthropicChatModel extends HttpChatModel {

    public static final String PROVIDER_NAME = "anthropic";

    private static final String DEFAULT_API_URL = "https://api.anthropic.com/v1/messages";

    /** Pinned rather than "latest": a provider's newest wire version is not a thing to be surprised by. */
    private static final String API_VERSION = "2023-06-01";

    public AnthropicChatModel(ProviderSettingsSource settingsSource) {
        super(settingsSource);
    }

    public AnthropicChatModel(
            ProviderSettingsSource settingsSource, Duration connectTimeout, Duration readTimeout) {
        super(settingsSource, connectTimeout, readTimeout);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    protected String defaultApiUrl() {
        return DEFAULT_API_URL;
    }

    @Override
    protected Map<String, String> headers(ProviderSettings settings) {
        return Map.of(
                "x-api-key",         settings.apiKey(),
                "anthropic-version", API_VERSION);
    }

    @Override
    protected Map<String, Object> requestBody(ChatRequest request, ProviderSettings settings) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("model",      settings.model());
        body.put("max_tokens", settings.maximumTokens());
        body.put("messages",   request.messages());

        // Omitted rather than sent empty: this provider refuses an empty tools array outright, which
        // would make "a conversation with no tools" an error rather than the ordinary case.
        if (request.hasSystem()) {
            body.put("system", request.system());
        }

        if (request.hasTools()) {
            body.put("tools", request.tools());
        }

        return body;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ChatResponse parseResponse(Map<String, Object> answer) {
        List<Map<String, Object>> content = answer.get("content") instanceof List<?> blocks
                ? (List<Map<String, Object>>) blocks
                : List.of();

        Map<String, Object> usage = answer.get("usage") instanceof Map<?, ?> counted
                ? (Map<String, Object>) counted
                : Map.of();

        return new ChatResponse(
                StopReason.of((String) answer.get("stop_reason")),
                content,
                new TokenUsage(
                        intValue(usage.get("input_tokens")),
                        intValue(usage.get("output_tokens"))));
    }

    private static int intValue(Object counted) {
        return counted instanceof Number number ? number.intValue() : 0;
    }
}
