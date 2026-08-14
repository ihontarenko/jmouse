package org.jmouse.ai.conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What to ask, and what was said before it.
 *
 * <p>⚠️ <strong>The system text is carried, never written here.</strong> A prompt is a product's voice —
 * what the assistant is for, what it may assume, how it addresses somebody — and a library that shipped
 * one would be shipping an opinion every adopter has to fight. This module holds no English except the
 * reasons a conversation ended.
 *
 * <p>The messages are the conversation so far, in the same shape a {@code ChatRequest} carries: a
 * caller that wants a conversation to continue passes back what
 * {@link ConversationResult#messages()} returned. <strong>Storing them is nobody's business here</strong>
 * — a product persists a conversation, or does not, and the loop works identically either way.
 *
 * @param system   standing instructions, supplied by the caller; null or blank for none
 * @param messages the conversation so far, oldest first, ending with whatever is being asked now
 */
public record ConversationRequest(String system, List<Map<String, Object>> messages) {

    public ConversationRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    /** A conversation that starts with somebody asking something. */
    public static ConversationRequest opening(String question) {
        return new ConversationRequest(null, List.of(userMessage(question)));
    }

    /** One that carries on from where {@link ConversationResult#messages()} left off. */
    public static ConversationRequest continuing(List<Map<String, Object>> messages) {
        return new ConversationRequest(null, messages);
    }

    public ConversationRequest withSystem(String system) {
        return new ConversationRequest(system, messages);
    }

    /** The same conversation with one more thing said in it. */
    public ConversationRequest asking(String question) {
        List<Map<String, Object>> grown = new ArrayList<>(messages);
        grown.add(userMessage(question));

        return new ConversationRequest(system, grown);
    }

    private static Map<String, Object> userMessage(String text) {
        return Map.of("role", "user", "content", text);
    }
}
