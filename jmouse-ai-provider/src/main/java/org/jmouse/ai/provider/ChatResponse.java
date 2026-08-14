package org.jmouse.ai.provider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What came back from one turn.
 *
 * <p>The content is the provider's own blocks, untouched — they are appended to the conversation and
 * sent back on the next round, so anything this library did not understand has to survive the trip.
 * {@link #blocks()} is how a caller reads them without casting; see {@link ContentBlock}.
 *
 * @param stopReason why the model stopped, as a value rather than a provider's spelling
 * @param content    the blocks it produced, in order and exactly as they arrived
 * @param usage      what the turn cost
 */
public record ChatResponse(StopReason stopReason, List<Map<String, Object>> content, TokenUsage usage) {

    public ChatResponse {
        stopReason = stopReason == null ? StopReason.UNKNOWN : stopReason;
        content    = content    == null ? List.of() : List.copyOf(content);
        usage      = usage      == null ? TokenUsage.none() : usage;
    }

    public List<ContentBlock> blocks() {
        return ContentBlock.allOf(content);
    }

    /** The tool calls to run before coming back, in the order the model asked for them. */
    public List<ContentBlock> toolCalls() {
        return blocks().stream().filter(ContentBlock::isToolUse).toList();
    }

    /**
     * Everything the model said, as one string.
     *
     * <p>Joined with a blank line rather than concatenated: a model producing two text blocks meant
     * them as two paragraphs, and running them together is how an answer arrives with a sentence
     * welded to the next one's first word.
     */
    public String text() {
        return blocks().stream()
                .filter(ContentBlock::isText)
                .map(ContentBlock::text)
                .collect(Collectors.joining("\n\n"));
    }

    /** Whether the caller is expected to run something and send the results back. */
    public boolean wantsTools() {
        return stopReason.wantsTools();
    }
}
