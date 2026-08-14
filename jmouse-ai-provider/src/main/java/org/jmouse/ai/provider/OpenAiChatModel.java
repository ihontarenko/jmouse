package org.jmouse.ai.provider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Chat Completions API — the translation that proves the canonical shape is canonical.
 *
 * <p>Shaped differently enough at every point that matters. The system prompt is a message in the array
 * rather than a field beside it. Tool definitions nest under {@code function}. A tool call is not a
 * content block but a sibling field of the content, and its arguments arrive as a <em>string</em>
 * containing JSON rather than as an object. A tool result is not a block either — it is a message with
 * a role of its own. And the answer lives at {@code choices[0].message}.
 *
 * <p><strong>Both directions are translated, and that is what makes this module a port rather than a
 * proxy.</strong> The implementation this was learned from translated only the response, which is
 * enough for a conversation with no tools and breaks on the second round of one with them: the
 * assistant message it had just been sent back was in the other provider's shape. A model that can only
 * be used for one round is not an alternative to anything.
 *
 * <p>⚠️ {@code max_tokens} is what this sends. A model that insists on {@code max_completion_tokens}
 * needs {@link #requestBody} overridden; nothing here can tell which, and guessing from a model name is
 * a rule that expires.
 */
public final class OpenAiChatModel extends HttpChatModel {

    public static final String PROVIDER_NAME = "openai";

    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";

    /** What this provider calls a message carrying the result of a tool it asked for. */
    private static final String TOOL_ROLE = "tool";

    /** What the canonical shape calls the same thing, as a content block. */
    private static final String TOOL_RESULT_BLOCK = "tool_result";

    public OpenAiChatModel(ProviderSettingsSource settingsSource) {
        super(settingsSource);
    }

    public OpenAiChatModel(
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
        return Map.of("authorization", "Bearer " + settings.apiKey());
    }

    @Override
    protected Map<String, Object> requestBody(ChatRequest request, ProviderSettings settings) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("model",      settings.model());
        body.put("max_tokens", settings.maximumTokens());
        body.put("messages",   translateMessages(request));

        if (request.hasTools()) {
            body.put("tools", request.tools().stream().map(OpenAiChatModel::translateTool).toList());
        }

        return body;
    }

    // ── Outward: the canonical conversation, in this provider's shape ────────────

    /**
     * Every message, plus the system prompt at the front of the array where this provider keeps it.
     *
     * <p>One canonical message can become several here — a turn carrying three tool results is three
     * messages — which is why this returns a list rather than mapping one to one.
     */
    private List<Map<String, Object>> translateMessages(ChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();

        if (request.hasSystem()) {
            messages.add(messageOf("system", request.system()));
        }

        request.messages().forEach(message -> appendTranslated(messages, message));

        return messages;
    }

    private void appendTranslated(List<Map<String, Object>> messages, Map<String, Object> message) {
        String role    = stringOf(message.get("role"));
        Object content = message.get("content");

        // Plain text is the one place the two providers already agree, and it is most of a conversation.
        if (content instanceof String text) {
            messages.add(messageOf(role, text));
            return;
        }

        List<ContentBlock> blocks = ContentBlock.allOf(blocksOf(content));

        if ("assistant".equals(role)) {
            messages.add(translateAssistantTurn(blocks));
            return;
        }

        appendUserTurn(messages, role, blocks);
    }

    /**
     * What the model said and what it asked for, which this provider keeps in two different fields.
     *
     * <p>The content stays null rather than empty when a turn was nothing but tool calls: an empty
     * string is a message with nothing in it, and this provider is entitled to say so.
     */
    private Map<String, Object> translateAssistantTurn(List<ContentBlock> blocks) {
        Map<String, Object> message = new LinkedHashMap<>();

        String said = blocks.stream()
                .filter(ContentBlock::isText)
                .map(ContentBlock::text)
                .collect(Collectors.joining("\n\n"));

        List<Map<String, Object>> toolCalls = blocks.stream()
                .filter(ContentBlock::isToolUse)
                .map(OpenAiChatModel::translateToolCall)
                .toList();

        message.put("role",    "assistant");
        message.put("content", said.isBlank() ? null : said);

        if (!toolCalls.isEmpty()) {
            message.put("tool_calls", toolCalls);
        }

        return message;
    }

    /**
     * Tool results first, then whatever the user actually said.
     *
     * <p>⚠️ The order is not cosmetic: this provider requires the messages answering a tool call to
     * follow the assistant turn that asked for it, with nothing in between. The canonical shape puts
     * them all in one turn, which is why one turn becomes several messages here.
     *
     * <p>An {@code is_error} flag has nowhere to go — this provider has no equivalent field. Nothing is
     * lost that matters: a refusal's content is a sentence saying it was refused and why, which is what
     * a model reads either way.
     */
    private void appendUserTurn(List<Map<String, Object>> messages, String role, List<ContentBlock> blocks) {
        blocks.stream()
                .filter(block -> TOOL_RESULT_BLOCK.equals(block.type()))
                .forEach(block -> {
                    Map<String, Object> result = new LinkedHashMap<>();

                    result.put("role",         TOOL_ROLE);
                    result.put("tool_call_id", block.map().get("tool_use_id"));
                    result.put("content",      textOf(block.map().get("content")));

                    messages.add(result);
                });

        String said = blocks.stream()
                .filter(ContentBlock::isText)
                .map(ContentBlock::text)
                .collect(Collectors.joining("\n\n"));

        if (!said.isBlank()) {
            messages.add(messageOf(role, said));
        }
    }

    /** {@code {id, name, input}} as a block, becomes {@code {id, type, function:{name, arguments}}}. */
    private static Map<String, Object> translateToolCall(ContentBlock block) {
        Map<String, Object> function = new LinkedHashMap<>();

        function.put("name", block.toolName());
        // A string containing JSON, not an object. Every provider that does this makes somebody's
        // afternoon worse, and this is the line where it stops mattering to anyone else.
        function.put("arguments", Json.write(block.input()));

        Map<String, Object> toolCall = new LinkedHashMap<>();

        toolCall.put("id",       block.toolUseId());
        toolCall.put("type",     "function");
        toolCall.put("function", function);

        return toolCall;
    }

    /** {@code {name, description, input_schema}} becomes {@code {type, function:{…, parameters}}}. */
    private static Map<String, Object> translateTool(Map<String, Object> tool) {
        Map<String, Object> function = new LinkedHashMap<>();

        function.put("name",        tool.get("name"));
        function.put("description", tool.get("description"));
        function.put("parameters",  tool.get("input_schema"));

        return Map.of("type", "function", "function", function);
    }

    // ── Inward: this provider's answer, as content blocks ────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    protected ChatResponse parseResponse(Map<String, Object> answer) {
        List<Map<String, Object>> choices = answer.get("choices") instanceof List<?> given
                ? (List<Map<String, Object>>) given
                : List.of();

        if (choices.isEmpty()) {
            throw new ProviderException(
                    PROVIDER_NAME + " answered without any choices, so there is nothing to read. This is "
                    + "a well-formed response that says nothing; nothing was changed by asking.");
        }

        Map<String, Object> choice  = choices.getFirst();
        Map<String, Object> message = choice.get("message") instanceof Map<?, ?> given
                ? (Map<String, Object>) given
                : Map.of();

        return new ChatResponse(
                StopReason.of(stringOf(choice.get("finish_reason"))),
                translateContent(message),
                usageIn(answer));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> translateContent(Map<String, Object> message) {
        List<Map<String, Object>> content = new ArrayList<>();

        if (message.get("content") instanceof String said && !said.isBlank()) {
            content.add(Map.of("type", ContentBlock.TEXT, "text", said));
        }

        // A declined answer arrives in a field of its own rather than as content, and a caller shown
        // an empty response has no idea it was declined rather than empty.
        if (message.get("refusal") instanceof String declined && !declined.isBlank()) {
            content.add(Map.of("type", ContentBlock.TEXT, "text", declined));
        }

        if (message.get("tool_calls") instanceof List<?> toolCalls) {
            ((List<Map<String, Object>>) toolCalls).forEach(
                    toolCall -> content.add(translateToolCallBack(toolCall)));
        }

        return content;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> translateToolCallBack(Map<String, Object> toolCall) {
        Map<String, Object> function = toolCall.get("function") instanceof Map<?, ?> given
                ? (Map<String, Object>) given
                : Map.of();

        // A mutable map rather than Map.of: an identifier or a name this provider left out would
        // otherwise be a NullPointerException here instead of a missing field somewhere readable.
        Map<String, Object> block = new LinkedHashMap<>();

        block.put("type",  ContentBlock.TOOL_USE);
        block.put("id",    toolCall.get("id"));
        block.put("name",  function.get("name"));
        block.put("input", parseArguments(stringOf(function.get("arguments"))));

        return block;
    }

    /**
     * The arguments string, back into the object every other part of this path expects.
     *
     * <p>Failing loudly rather than passing an empty map on: a tool run with no arguments because they
     * could not be read is a tool run wrongly, and it would succeed at doing the wrong thing.
     */
    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }

        return Json.tryReadMap(arguments).orElseThrow(() -> new ProviderException(
                PROVIDER_NAME + " asked for a tool with arguments that are not valid JSON: "
                + Json.truncate(arguments) + ". Nothing was run."));
    }

    @SuppressWarnings("unchecked")
    private TokenUsage usageIn(Map<String, Object> answer) {
        Map<String, Object> usage = answer.get("usage") instanceof Map<?, ?> counted
                ? (Map<String, Object>) counted
                : Map.of();

        return new TokenUsage(
                intValue(usage.get("prompt_tokens")),
                intValue(usage.get("completion_tokens")));
    }

    // ── Reading what arrived ─────────────────────────────────────────────────────

    private static Map<String, Object> messageOf(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();

        message.put("role",    role);
        message.put("content", content);

        return message;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> blocksOf(Object content) {
        return content instanceof List<?> blocks ? (List<Map<String, Object>>) blocks : List.of();
    }

    /**
     * A tool result's content, which the canonical shape allows to be text or a list of blocks.
     *
     * <p>Anything else is written back as JSON rather than dropped — a structured result is still
     * something a model can read, and an empty tool result is a model told its tool did nothing.
     */
    private static String textOf(Object content) {
        if (content == null) {
            return "";
        }

        if (content instanceof String text) {
            return text;
        }

        if (content instanceof List<?> blocks) {
            return ContentBlock.allOf(blocksOf(blocks)).stream()
                    .filter(ContentBlock::isText)
                    .map(ContentBlock::text)
                    .collect(Collectors.joining("\n\n"));
        }

        return Json.write(content);
    }

    private static String stringOf(Object value) {
        return value instanceof String text ? text : null;
    }

    private static int intValue(Object counted) {
        return counted instanceof Number number ? number.intValue() : 0;
    }
}
