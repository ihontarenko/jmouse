package org.jmouse.ai.provider;

import java.util.Base64;
import java.util.LinkedHashMap;
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
     * A block carrying a picture, for a model to <strong>look at</strong> rather than read a sentence
     * about.
     *
     * <p>⚠️ <strong>This is the canonical shape, and it is Anthropic's</strong> — for the same reason
     * everything else here is, which {@link AnthropicChatModel} spells out. A block is
     * {@code {"type":"image","source":{"type":"base64","media_type":…,"data":…}}}, that provider forwards
     * it untouched, and every other adapter translates it exactly as it already translates a tool call.
     */
    public static final String IMAGE = "image";

    /** Where an image block keeps what it is and the bytes themselves. */
    private static final String SOURCE     = "source";
    private static final String BASE64     = "base64";
    private static final String MEDIA_TYPE = "media_type";
    private static final String DATA       = "data";

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

    public boolean isImage() {
        return IMAGE.equals(type());
    }

    /**
     * 🖼️ <strong>The one place in this library that turns bytes into something a model can see.</strong>
     *
     * <p>Written once on purpose. A picture reaches a conversation from two directions — somebody
     * attaches one to a question, and a tool hands one back — and those are two callers, not two shapes.
     * A second encoder would be a second opinion about a thing there is one right answer to, and the
     * copies would agree until the day a provider wanted the media type spelled differently.
     *
     * <p>⚠️ <strong>No size ceiling here, deliberately.</strong> What is reasonable depends on what the
     * product stores. A library that guessed would either refuse a legitimate photograph or wave a 20 MB
     * one through — which does not fail, it fills the conversation and leaves no room for the answer.
     * The product declares a limit and refuses over it, with a sentence saying so.
     *
     * @param mimeType what the bytes are, e.g. {@code image/png}
     * @param bytes    the image itself
     * @return the canonical block
     */
    public static Map<String, Object> image(String mimeType, byte[] bytes) {
        Map<String, Object> source = new LinkedHashMap<>();

        source.put("type",     BASE64);
        source.put(MEDIA_TYPE, mimeType);
        source.put(DATA,       Base64.getEncoder().encodeToString(bytes));

        Map<String, Object> block = new LinkedHashMap<>();

        block.put("type", IMAGE);
        block.put(SOURCE, source);

        return block;
    }

    /** What the picture is, or empty for a block that is not one. */
    public Optional<String> imageMediaType() {
        return isImage() && source().get(MEDIA_TYPE) instanceof String type
                ? Optional.of(type)
                : Optional.empty();
    }

    /**
     * The picture as an inline address, for the providers that take one instead of a source object.
     *
     * <p>⚠️ {@code Optional} rather than a null string. A block that is not an image and one whose source
     * this library cannot read are both <em>there is nothing to send here</em> — and an adapter handed
     * {@code "data:null;base64,null"} would send a perfectly well-formed address to a picture that does
     * not exist, which fails at the provider as a content error naming nothing recognisable.
     */
    public Optional<String> imageDataUri() {
        if (!isImage() || !(source().get(DATA) instanceof String encoded) || encoded.isBlank()) {
            return Optional.empty();
        }

        return imageMediaType().map(type -> "data:" + type + ";" + BASE64 + "," + encoded);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> source() {
        return map.get(SOURCE) instanceof Map<?, ?> given ? (Map<String, Object>) given : Map.of();
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
