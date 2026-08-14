package org.jmouse.ai.provider;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A model that lives behind somebody else's HTTP endpoint.
 *
 * <p>Not an adapter for a provider — an adapter for <strong>an address that speaks the canonical
 * shape</strong>. It sends {@link ChatRequest} as JSON and reads {@link ChatResponse} back, which means
 * a service holding the keys, the model choice and the rate budget for a whole installation can sit in
 * front of two providers and every application talks to it with the same code it would use to talk to
 * them directly.
 *
 * <p>That is the payoff of the port being what a <em>caller</em> holds. A conversation loop given one of
 * these cannot tell that the model is two network hops away, and a deployment switches between local
 * and shared by changing configuration.
 *
 * <h2>The shape</h2>
 *
 * <pre>{@code
 * POST <apiUrl>
 * {"system": "…", "messages": [ … ], "tools": [ … ]}
 *
 * 200
 * {"stopReason": "tool_use",
 *  "content":    [ … ],
 *  "usage":      {"inputTokens": 0, "outputTokens": 0}}
 * }</pre>
 *
 * <p>The blocks in {@code content} and {@code messages} are whatever the provider behind the gateway
 * produced, untouched — a gateway forwards them, and translating them twice is how a shape drifts.
 *
 * <p>⚠️ <strong>No default address.</strong> A gateway is somewhere in particular and there is nothing
 * sensible to guess; settings without an api url are refused with a sentence saying so.
 *
 * <p>⚠️ <strong>No application field.</strong> A gateway that needs to know which application is asking
 * learns it from the credential, not from a field every caller has to remember to fill in — a request
 * body is not the place to put a lookup key, and doing so is what stops a service being usable as a
 * library.
 */
public final class GatewayChatModel extends HttpChatModel {

    public static final String PROVIDER_NAME = "gateway";

    public GatewayChatModel(ProviderSettingsSource settingsSource) {
        super(settingsSource);
    }

    public GatewayChatModel(
            ProviderSettingsSource settingsSource, Duration connectTimeout, Duration readTimeout) {
        super(settingsSource, connectTimeout, readTimeout);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    protected String defaultApiUrl() {
        return null;
    }

    /**
     * A gateway inside a trust boundary may take no credential at all, which is the one legitimate case
     * for this in the whole module. A key is still sent when there is one.
     */
    @Override
    protected boolean requiresApiKey() {
        return false;
    }

    @Override
    protected Map<String, String> headers(ProviderSettings settings) {
        return settings.hasApiKey()
                ? Map.of("authorization", "Bearer " + settings.apiKey())
                : Map.of();
    }

    /**
     * ⚠️ The model is sent, and the gateway is free to ignore it.
     *
     * <p>Both readings are legitimate — a gateway that lets callers choose, and one that decides for the
     * whole installation and treats this as a hint. Leaving it out would remove the first; the second
     * costs nothing, because ignoring a field is easy and inventing one is not.
     */
    @Override
    protected Map<String, Object> requestBody(ChatRequest request, ProviderSettings settings) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("model",     settings.model());
        body.put("maxTokens", settings.maximumTokens());
        body.put("messages",  request.messages());

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
                StopReason.of(stringOf(answer.get("stopReason"))),
                content,
                new TokenUsage(
                        intValue(usage.get("inputTokens")),
                        intValue(usage.get("outputTokens"))));
    }

    private static String stringOf(Object value) {
        return value instanceof String text ? text : null;
    }

    private static int intValue(Object counted) {
        return counted instanceof Number number ? number.intValue() : 0;
    }
}
