package org.jmouse.ai.provider;

import java.util.List;
import java.util.Map;

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

    private String string(String name) {
        return map.get(name) instanceof String value ? value : null;
    }
}
