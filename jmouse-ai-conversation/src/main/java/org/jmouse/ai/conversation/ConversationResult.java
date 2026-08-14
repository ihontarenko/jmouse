package org.jmouse.ai.conversation;

import org.jmouse.ai.provider.TokenUsage;

import java.util.List;
import java.util.Map;

/**
 * What the loop produced, and what it cost to produce it.
 *
 * <p>The messages come back because a conversation that continues has to continue from somewhere, and
 * the loop is the only thing that knows what was added to it — the assistant's turns, and the tool
 * results that answered them. Handing back the whole array is what lets persistence stay entirely the
 * caller's business.
 *
 * <p>⚠️ <strong>{@code text} is not always an answer.</strong> When the ending is not
 * {@link ConversationEnding#MODEL_FINISHED} it is whatever the model happened to have said before it
 * was stopped, which may be nothing at all. {@link ConversationEnding#describe()} is what a caller
 * shows instead.
 *
 * @param text      everything the model said on its last turn
 * @param messages  the whole conversation, ready to be sent again
 * @param ending    why the loop stopped, and whether the work is finished
 * @param rounds    how many times the model was asked
 * @param usage     what all of those rounds cost together
 * @param toolCalls how many tool calls were dispatched, across every round
 */
public record ConversationResult(
        String                    text,
        List<Map<String, Object>> messages,
        ConversationEnding        ending,
        int                       rounds,
        TokenUsage                usage,
        int                       toolCalls
) {

    public ConversationResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
        usage    = usage    == null ? TokenUsage.none() : usage;
    }

    /** Whether the model got to the end of what it was doing. */
    public boolean finished() {
        return ending.finished();
    }

    /** The model's answer, or the reason there is not one. */
    public String describe() {
        return finished() && !text.isBlank() ? text : ending.describe();
    }
}
