package org.jmouse.ai.conversation;

import org.jmouse.ai.RefusalRendering;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.ToolOutcome;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.provider.ChatModel;
import org.jmouse.ai.provider.ChatRequest;
import org.jmouse.ai.provider.ChatResponse;
import org.jmouse.ai.provider.ContentBlock;
import org.jmouse.ai.provider.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ask, run what was asked for, ask again — and stop.
 *
 * <p>The loop, and only the loop. Everything it might be tempted to do itself is already somewhere
 * better: finding a tool, checking a permission, resolving a scope, bounding a call and writing a
 * refusal are all {@link ToolDispatcher}'s, and talking to a model is {@link ChatModel}'s.
 *
 * <h2>Why step three is the whole point</h2>
 *
 * <p>A tool call from the model becomes {@code dispatcher.dispatch(name, arguments)} — <strong>the same
 * method a Model Context Protocol client reaches from outside the process, on the same thread, under
 * the same caller, through the same guards.</strong> Not a similar one. The alternative every
 * implementation of this reaches for — a small lookup here, a try/catch, an {@code execute} — is a
 * second dispatcher, and a second dispatcher is where an in-app assistant and an external client
 * quietly stop agreeing about what a caller may do.
 *
 * <p>⚠️ Which is why this class does no tool lookup, no permission check and no exception wrapping
 * beyond turning one into the envelope a model reads. If the loop ever needs something the dispatcher
 * does not expose, <em>that is a finding about the dispatcher</em>.
 *
 * <h2>Who is calling</h2>
 *
 * <p>⚠️ <strong>The caller is not a parameter.</strong> It is resolved inside the dispatcher, by the
 * product's own {@code CallerResolver}, exactly as it is for every other transport. A caller passed in
 * here would be a second answer to a question that already has one, and the interesting case is not the
 * two agreeing — it is the day they do not, when a loop runs tools as somebody the dispatcher has never
 * heard of.
 *
 * <p>⚠️ <strong>The same caller decides what the model is even shown.</strong> The tool list is
 * {@code dispatcher.reachable()} rather than the whole catalogue, so a model is never offered an action
 * that would be refused the moment it called it — which costs a round, and reads to whoever is watching
 * as the product being broken rather than as a permission they do not hold.
 *
 * <h2>What is not here</h2>
 *
 * <p>No system prompt, no persistence, no product vocabulary. A prompt is a product's voice, a stored
 * conversation is a product's table, and both arrive through {@link ConversationRequest} and leave
 * through {@link ConversationResult}. English in this module that is not a refusal or a stated ending is
 * a bug.
 */
public final class ConversationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationRunner.class);

    /** The block a tool result travels in, and the flag that says the model should read it and retry. */
    private static final String TOOL_RESULT = "tool_result";
    private static final String IS_ERROR    = "is_error";

    private final ChatModel          model;
    private final ToolDispatcher     dispatcher;
    private final ConversationBudget budget;

    public ConversationRunner(ChatModel model, ToolDispatcher dispatcher) {
        this(model, dispatcher, ConversationBudget.defaults());
    }

    public ConversationRunner(ChatModel model, ToolDispatcher dispatcher, ConversationBudget budget) {
        this.model      = model;
        this.dispatcher = dispatcher;
        this.budget     = budget;
    }

    /**
     * Runs one conversation to its end, or to its budget.
     *
     * @throws org.jmouse.ai.provider.ProviderException when the model itself cannot be reached; a tool
     *         that refuses is not an exception here, it is a result the model reads and retries against
     */
    public ConversationResult run(ConversationRequest request) {
        List<Map<String, Object>> messages  = new ArrayList<>(request.messages());
        // ⚠️ What this caller may run, not what the installation has. Resolved once: the caller cannot
        // change mid-conversation, so asking an authorization engine about every action again on every
        // round is a cost with the same answer on the other side of it.
        List<Map<String, Object>> tools     = ProviderTools.from(dispatcher.reachable());

        TokenUsage spent     = TokenUsage.none();
        int        rounds    = 0;
        int        toolCalls = 0;

        while (true) {
            rounds++;

            ChatResponse answer = model.converse(new ChatRequest(request.system(), messages, tools));
            spent = spent.plus(answer.usage());

            // The model's turn goes in whether or not it asked for anything: the next round has to see
            // what it said, and a tool result with no call above it is a conversation nobody can read.
            messages.add(assistantTurn(answer));

            if (!answer.wantsTools()) {
                LOGGER.debug("Conversation finished after {} round(s), {} tool call(s), {} tokens",
                        rounds, toolCalls, spent.totalTokens());

                return new ConversationResult(
                        answer.text(), messages, ConversationEnding.MODEL_FINISHED, rounds, spent, toolCalls);
            }

            messages.add(resultsOf(answer));
            toolCalls += answer.toolCalls().size();

            // ⚠️ Checked after the results are appended, so a conversation stopped by its budget can be
            // continued rather than restarted: what the tools produced is in the array, unsent.
            ConversationEnding reached = budget.reachedAfter(rounds, spent);

            if (reached != null) {
                LOGGER.info("Conversation stopped: {} after {} round(s) and {} tokens",
                        reached, rounds, spent.totalTokens());

                return new ConversationResult(answer.text(), messages, reached, rounds, spent, toolCalls);
            }
        }
    }

    // ── One round's two messages ─────────────────────────────────────────────────

    /** What the model said, kept exactly as it arrived — including anything this library cannot read. */
    private Map<String, Object> assistantTurn(ChatResponse answer) {
        return Map.of("role", "assistant", "content", answer.content());
    }

    private Map<String, Object> resultsOf(ChatResponse answer) {
        List<Map<String, Object>> results = answer.toolCalls().stream().map(this::runOne).toList();

        return Map.of("role", "user", "content", results);
    }

    /**
     * One tool call, and the three things that can come back from it.
     *
     * <p>A refusal and a failure both come back as an error result carrying a sentence, never as a thrown
     * exception. That is the whole difference between a tool that works and one that appears broken: a
     * model told <em>why</em> corrects itself and calls again, while a loop that threw would end the
     * conversation and report "something went wrong" to somebody who could have been told what.
     *
     * <p>The sentences come from {@link RefusalRendering} rather than from here, because a Model Context
     * Protocol client must read the identical words. Two implementations of one paragraph is how one of
     * them ends up saying less.
     */
    private Map<String, Object> runOne(ContentBlock call) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("type",        TOOL_RESULT);
        result.put("tool_use_id", call.toolUseId());

        try {
            ToolOutcome outcome = dispatcher.dispatch(call.toolName(), call.input());

            // The structured form, so the scope it ran in travels with the answer rather than being
            // buried in prose the model may not read.
            result.put("content", ToolResultJson.write(outcome.asStructuredContent()));

        } catch (ToolRefusedException refusal) {
            result.put("content", RefusalRendering.render(refusal));
            result.put(IS_ERROR,  true);

        } catch (RuntimeException failure) {
            // Reached the domain and stopped inside it. Deliberately says the opposite of a refusal:
            // part of it may have happened, so a retry is not obviously safe.
            LOGGER.warn("{} failed inside the domain: {}", call.toolName(), failure.getMessage(), failure);

            result.put("content", RefusalRendering.renderFailure(call.toolName(), failure));
            result.put(IS_ERROR,  true);
        }

        return result;
    }
}
