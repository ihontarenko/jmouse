package org.jmouse.ai.conversation;

import org.jmouse.ai.ToolImage;
import org.jmouse.ai.provider.ContentBlock;

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
        return opening(question, List.of());
    }

    /** The same, with something to look at while answering. */
    public static ConversationRequest opening(String question, List<ToolImage> pictures) {
        return new ConversationRequest(null, List.of(userMessage(question, pictures)));
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
        return asking(question, List.of());
    }

    /** The same, with photographs to look at while answering it. */
    public ConversationRequest asking(String question, List<ToolImage> pictures) {
        List<Map<String, Object>> grown = new ArrayList<>(messages);
        grown.add(userMessage(question, pictures));

        return new ConversationRequest(system, grown);
    }

    /**
     * What somebody just said, and anything they put in front of the model to say it about.
     *
     * <p>⚠️ <strong>A plain string where there are no pictures.</strong> A one-element block list would
     * be equally correct and would rewrite every message in every stored conversation into a shape a
     * person reading a transcript has to decode — for the sake of the one turn in fifty that carries an
     * image. The shape follows the content.
     *
     * <p>⚠️ <strong>Pictures first, then the words.</strong> Every provider's own guidance agrees, and
     * the reason is not arbitrary: "what is this component?" is a question about the photograph above it
     * and a question about nothing at all below it — and the model reads the turn in the order it is
     * written, exactly as a person would.
     */
    private static Map<String, Object> userMessage(String text, List<ToolImage> pictures) {
        if (pictures == null || pictures.isEmpty()) {
            return Map.of("role", "user", "content", text);
        }

        List<Map<String, Object>> blocks = new ArrayList<>();

        // ⚠️ The library's one encoder, shared with the tool-result path below it. A second one here
        // would be a second opinion about a thing there is one right answer to.
        pictures.forEach(picture -> blocks.add(ContentBlock.image(picture.mimeType(), picture.bytes())));
        blocks.add(Map.of("type", ContentBlock.TEXT, "text", text));

        return Map.of("role", "user", "content", blocks);
    }
}
