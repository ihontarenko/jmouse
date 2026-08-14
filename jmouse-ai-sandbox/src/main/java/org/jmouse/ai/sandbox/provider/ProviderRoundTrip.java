package org.jmouse.ai.sandbox.provider;

import org.jmouse.ai.provider.AnthropicChatModel;
import org.jmouse.ai.provider.ChatModel;
import org.jmouse.ai.provider.ChatRequest;
import org.jmouse.ai.provider.ChatResponse;
import org.jmouse.ai.provider.GatewayChatModel;
import org.jmouse.ai.provider.OpenAiChatModel;
import org.jmouse.ai.provider.ProviderException;
import org.jmouse.ai.provider.ProviderSettings;
import org.jmouse.ai.provider.ProviderSettingsSource;

import java.util.List;
import java.util.Map;

/**
 * One {@code ChatRequest}, three models, and whether the three answers agree.
 *
 * <p>Ticket 05's whole claim in one runnable form: <em>the same request runs unchanged against all
 * three implementations.</em> Claiming it is easy while only one of them is ever exercised, and the
 * translation that would prove it wrong is invisible from the source — so this sends a real request to
 * a real socket, prints what each provider was actually handed, and compares what came back.
 *
 * <p>The request is deliberately the awkward one: a system prompt, a tool definition, and a
 * conversation already two rounds deep, so it carries an assistant turn holding a tool call and a user
 * turn holding that call's result. <strong>Those are the two shapes a response-only translation gets
 * away with ignoring</strong> — a text-only exchange round-trips through anything.
 */
public final class ProviderRoundTrip {

    private static final String RULE = "-".repeat(100);

    public static void main(String[] arguments) {
        try (StubProvider provider = StubProvider.started()) {
            ChatRequest request = twoRoundsIn();

            List<ChatResponse> answers = List.of(
                    run("anthropic", new AnthropicChatModel(settingsFor("anthropic", provider)), request, provider),
                    run("openai",    new OpenAiChatModel(settingsFor("openai", provider)),       request, provider),
                    run("gateway",   new GatewayChatModel(settingsFor("gateway", provider)),     request, provider));

            compare(answers);
            refusals(provider);
        }
    }

    // ── The request every model is given ─────────────────────────────────────────

    /**
     * A conversation mid-tool-call, in the canonical shape.
     *
     * <p>Round one asked a question, the model called a tool, the caller answered with the result. What
     * goes on the wire now has to carry all three, and only one provider takes them in this form.
     */
    private static ChatRequest twoRoundsIn() {
        Map<String, Object> question = Map.of(
                "role", "user",
                "content", "What is on shelf A?");

        Map<String, Object> theModelCalledATool = Map.of(
                "role", "assistant",
                "content", List.of(
                        Map.of("type", "text", "text", "Let me look at that shelf."),
                        Map.of("type", "tool_use", "id", "call-0", "name", "parts_list",
                               "input", Map.of("shelf", "A"))));

        Map<String, Object> andHereIsTheResult = Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "tool_result", "tool_use_id", "call-0",
                               "content", "[{\"id\":\"part-1\",\"name\":\"M3 bolt\"}]")));

        Map<String, Object> tool = Map.of(
                "name", "parts_list",
                "description", "Every part in one workshop.",
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of("shelf", Map.of("type", "string")),
                        "required", List.of()));

        return ChatRequest.of(List.of(question, theModelCalledATool, andHereIsTheResult))
                .withSystem("You are looking after a workshop inventory.")
                .withTools(List.of(tool));
    }

    private static ProviderSettingsSource settingsFor(String provider, StubProvider stub) {
        return ProviderSettingsSource.fixed(
                ProviderSettings.of(provider, "a-model", "a-key", 2048).withApiUrl(stub.urlFor(provider)));
    }

    // ── Running one, and reading both halves ─────────────────────────────────────

    private static ChatResponse run(
            String provider, ChatModel model, ChatRequest request, StubProvider stub) {

        System.out.println();
        System.out.println(RULE);
        System.out.println("  " + provider.toUpperCase());
        System.out.println(RULE);

        ChatResponse answer = model.converse(request);

        System.out.println("  SENT");
        System.out.println("    " + stub.requestSentTo(provider).replace("\n", ""));
        System.out.println("  READ BACK");
        System.out.println("    stopReason  " + answer.stopReason());
        System.out.println("    text        " + answer.text());
        System.out.println("    usage       " + answer.usage().inputTokens() + " in, "
                                              + answer.usage().outputTokens() + " out");

        answer.toolCalls().forEach(call -> System.out.println(
                "    tool call   " + call.toolName() + " " + call.input()
                + "  [" + call.toolUseId() + "]"));

        return answer;
    }

    /**
     * Whether the three answers are the same answer.
     *
     * <p>Compared field by field rather than by equality on the record: the content blocks legitimately
     * differ in shape between providers, and what has to agree is what a caller reads — the stop
     * reason, the words, the calls and their arguments, and the cost.
     */
    private static void compare(List<ChatResponse> answers) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  DO THE THREE AGREE?");
        System.out.println(RULE);

        report("stop reason", answers.stream().map(answer -> answer.stopReason().name()).toList());
        report("text",        answers.stream().map(ChatResponse::text).toList());
        report("tool calls",  answers.stream().map(ProviderRoundTrip::describeToolCalls).toList());
        report("usage",       answers.stream().map(answer -> answer.usage().toString()).toList());
    }

    private static String describeToolCalls(ChatResponse answer) {
        return answer.toolCalls().stream()
                .map(call -> call.toolUseId() + ":" + call.toolName() + call.input())
                .toList()
                .toString();
    }

    private static void report(String what, List<String> readings) {
        boolean agreed = readings.stream().distinct().count() == 1;

        System.out.printf("    %-12s %-5s %s%n",
                what, agreed ? "SAME" : "DIFF", agreed ? readings.getFirst() : readings);
    }

    // ── And the three ways it fails ──────────────────────────────────────────────

    /**
     * Each failure translation, once.
     *
     * <p>The address that is not an address is the one worth seeing: it is thrown before anything is
     * sent, by the URI parser rather than by the HTTP client, and it is the one that most easily escapes
     * as an unexplained {@code IllegalArgumentException} far from the setting that caused it.
     */
    private static void refusals(StubProvider stub) {
        System.out.println();
        System.out.println(RULE);
        System.out.println("  AND WHEN IT DOES NOT WORK");
        System.out.println(RULE);

        show("no key configured", ProviderSettings.of("anthropic", "a-model", null, 2048)
                .withApiUrl(stub.urlFor("anthropic")));

        show("not an address", ProviderSettings.of("anthropic", "a-model", "a-key", 2048)
                .withApiUrl("api.anthropic.com/v1/messages"));

        show("nothing answering", ProviderSettings.of("anthropic", "a-model", "a-key", 2048)
                .withApiUrl("http://127.0.0.1:1/v1/messages"));

        show("settings for somebody else", ProviderSettings.of("openai", "a-model", "a-key", 2048)
                .withApiUrl(stub.urlFor("anthropic")));
    }

    private static void show(String what, ProviderSettings settings) {
        System.out.println();
        System.out.println("  > " + what);

        try {
            new AnthropicChatModel(ProviderSettingsSource.fixed(settings))
                    .converse(ChatRequest.of(List.of(Map.of("role", "user", "content", "hello"))));

            System.out.println("    NOT REFUSED - which it must be.");

        } catch (ProviderException refusal) {
            System.out.println("    " + refusal.getMessage());
        }
    }

    private ProviderRoundTrip() {
    }
}
