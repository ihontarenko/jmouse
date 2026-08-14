package org.jmouse.ai.sandbox.conversation;

import org.jmouse.ai.provider.ChatModel;
import org.jmouse.ai.provider.ChatRequest;
import org.jmouse.ai.provider.ChatResponse;
import org.jmouse.ai.provider.StopReason;
import org.jmouse.ai.provider.TokenUsage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * A model that says what it was told to say, in order.
 *
 * <p>Which is the only way to exercise a loop. A real model is not reproducible, needs a key, and —
 * worst of all for this — cannot be made to ask for the awkward thing on purpose. A script can: two
 * tool calls in one turn, a call that will be refused, a call over a confirmation threshold, and a turn
 * that never ends.
 *
 * <p>⚠️ It also <strong>records every request it was given</strong>, because half of what the runner
 * does is invisible in its result: whether the tool definitions were rendered, whether the assistant
 * turn was appended before the results, whether the results carry the identifiers of the calls they
 * answer. The conversation the model was handed on round three is the evidence for all of it.
 *
 * <p>This is also what makes the loop testable in ticket 13 without a network, which is the second
 * reason it exists here rather than in a test.
 */
public final class ScriptedChatModel implements ChatModel {

    /** One turn the model will take, in order. */
    public record Turn(StopReason stopReason, List<Map<String, Object>> content) {

        /** A turn that says something and stops. */
        public static Turn saying(String text) {
            return new Turn(StopReason.END_TURN, List.of(Map.of("type", "text", "text", text)));
        }

        /** A turn that says something and asks for tools, which the loop must run and come back with. */
        @SafeVarargs
        public static Turn calling(String text, Map<String, Object>... calls) {
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", text));
            content.addAll(List.of(calls));

            return new Turn(StopReason.TOOL_USE, content);
        }

        /** One call inside such a turn. */
        public static Map<String, Object> call(String id, String tool, Map<String, Object> arguments) {
            return Map.of("type", "tool_use", "id", id, "name", tool, "input", arguments);
        }
    }

    private static final TokenUsage PER_TURN = new TokenUsage(400, 40);

    private final Deque<Turn>        script  = new ArrayDeque<>();
    private final List<ChatRequest>  asked   = new ArrayList<>();
    private final Turn               whenTheScriptRunsOut;

    public ScriptedChatModel(List<Turn> turns) {
        this(turns, Turn.saying("(the script ran out)"));
    }

    /**
     * @param whenTheScriptRunsOut what to say once the script is spent — a turn that <em>keeps calling
     *                             tools</em> is how a runaway conversation is made to happen on purpose
     */
    public ScriptedChatModel(List<Turn> turns, Turn whenTheScriptRunsOut) {
        this.script.addAll(turns);
        this.whenTheScriptRunsOut = whenTheScriptRunsOut;
    }

    @Override
    public String providerName() {
        return "scripted";
    }

    @Override
    public ChatResponse converse(ChatRequest request) {
        asked.add(request);

        Turn turn = script.isEmpty() ? whenTheScriptRunsOut : script.removeFirst();

        return new ChatResponse(turn.stopReason(), turn.content(), PER_TURN);
    }

    /** Every request the loop made, in order. The evidence for what the runner actually did. */
    public List<ChatRequest> asked() {
        return List.copyOf(asked);
    }

    public ChatRequest lastRequest() {
        return asked.getLast();
    }
}
