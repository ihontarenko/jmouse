package org.jmouse.ai.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One piece of what a model said, read without casting.
 *
 * <p><strong>The map is what travels; this is only a pair of eyes on it.</strong> Modelling every
 * provider's block variants as classes would be duplicate maintenance of a shape nothing in this path
 * interprets — a block is forwarded back to the provider on the next round exactly as it arrived, and
 * anything this library did not understand has to survive the trip. What a caller actually needs is
 * narrow: is this a tool call, which tool, with what arguments.
 *
 * <p>So this is a view rather than a value. It holds the map it was made from, answers five questions
 * about it, and hands the map back untouched for the round trip.
 *
 * @param map the block as the provider sent it
 */
public record ContentBlock(Map<String, Object> map) {

    /** A block that says something in words. */
    public static final String TEXT = "text";

    /** A block that asks for a tool to be run. */
    public static final String TOOL_USE = "tool_use";

    /**
     * Where a provider's own additions to a tool call are kept, unread, for the way back.
     *
     * <p>A block arriving in the canonical shape already survives untouched — that is the whole design
     * of this type. A tool call translated <em>out of</em> another provider's shape does not: it is
     * rebuilt field by field, and anything the translation had no field for was silently gone. Gemini's
     * {@code thought_signature} is the case that proved it, and it is required back on the next round,
     * so losing it made every tool-using conversation exactly one round long.
     *
     * <p>⚠️ Opaque, and to be kept that way. What is in it belongs to the provider that sent it; the
     * only rule this library has is that it goes back where it came from.
     *
     * <p>⚠️ <strong>Which is also its limit.</strong> A conversation continued against a <em>different</em>
     * provider than the one that produced it can be refused over this field, because it is one
     * provider's private data offered to another. Nothing here can translate it, and inventing a
     * translation would be worse than the refusal. Starting a new conversation is the answer, and a
     * provider is not something that changes mid-conversation on a working installation.
     */
    public static final String PROVIDER_EXTRA = "provider_extra";

    public ContentBlock {
        map = map == null ? Map.of() : map;
    }

    public static ContentBlock of(Map<String, Object> map) {
        return new ContentBlock(map);
    }

    /** Every block of one response, in order. */
    public static List<ContentBlock> allOf(List<Map<String, Object>> content) {
        return content == null ? List.of() : content.stream().map(ContentBlock::of).toList();
    }

    public String type() {
        return string("type");
    }

    public boolean isText() {
        return TEXT.equals(type());
    }

    public boolean isToolUse() {
        return TOOL_USE.equals(type());
    }

    /** What the block says, or empty for a block that does not say anything. */
    public String text() {
        return string(TEXT) == null ? "" : string(TEXT);
    }

    /**
     * The identifier a tool call must be answered with.
     *
     * <p>⚠️ Not the tool's name and not interchangeable with it: a model may call the same tool three
     * times in one turn, and the identifier is the only thing that says which result belongs to which
     * call. A loop that matched on the name would answer all three with the first result.
     */
    public String toolUseId() {
        return string("id");
    }

    public String toolName() {
        return string("name");
    }

    /** The arguments the model wants the tool run with, exactly as it sent them. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> input() {
        return map.get("input") instanceof Map<?, ?> input
                ? (Map<String, Object>) input
                : Map.of();
    }

    /**
     * Whatever the provider hung on this tool call, if anything — see {@link #PROVIDER_EXTRA}.
     *
     * <p>{@code Optional} rather than an empty map: "the provider sent nothing" and "the provider sent
     * an empty object" are different things to put back on the wire, and only one of them is honest.
     */
    public Optional<Object> providerExtra() {
        return Optional.ofNullable(map.get(PROVIDER_EXTRA));
    }

    private String string(String name) {
        return map.get(name) instanceof String value ? value : null;
    }
}
