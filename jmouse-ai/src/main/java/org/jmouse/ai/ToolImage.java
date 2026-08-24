package org.jmouse.ai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🖼️ A picture in a conversation, so the model can <strong>look at it</strong> rather than read a
 * sentence about it.
 *
 * <h3>⚠️ Both directions, despite the name</h3>
 *
 * <p>This began as the answer to <em>a tool is handing a picture back</em> and is still named for it.
 * It carries a picture going the other way too — one somebody attached to a question — because the two
 * are the same three things (what the bytes are, the bytes, and what to say about them) and a second
 * record differing in nothing but its name would be a second place to fix anything either of them got
 * wrong. The name is history; the type is a picture.
 *
 * <h3>Why a type of its own</h3>
 *
 * <p>Every tool result so far has been text and a structured body. That is the whole of what a model can
 * be told, and it turns "this record has a screenshot of the fault" into a filename in a list. The
 * protocol has always been able to carry an image; nothing in this library could produce one.</p>
 *
 * <p>So a handler returns one of these as its payload, and the transport turns it into an image content
 * block. Nothing else changes: the same guards run, the same audit line is written, the same refusals
 * come back.</p>
 *
 * <h3>⚠️ The bytes are NOT in the structured body</h3>
 *
 * <p>{@link ToolOutcome#asStructuredContent()} publishes {@link #about()} and never the bytes. Base64 in
 * the JSON as well as in the content block would send the image twice — and the second copy lands
 * somewhere no model looks at it and every model pays for it.</p>
 *
 * <h3>⚠️ Size is the product's to refuse, and it must refuse rather than truncate</h3>
 *
 * <p>This library sets no ceiling, because what is reasonable depends on what the product stores. What
 * it will not do is let a large one through quietly: a 20 MB photograph is roughly 27 MB of base64, which
 * does not fail — it fills the conversation and leaves no room for the answer. A product exposing this
 * declares a limit and refuses over it, with a sentence saying so.</p>
 *
 * @param mimeType what the bytes are, e.g. {@code image/png}
 * @param bytes    the image itself
 * @param about    what the caller should know about it — name, size, where it came from
 */
public record ToolImage(String mimeType, byte[] bytes, Map<String, Object> about) {

    /**
     * 🏗️ An image with a description beside it.
     *
     * @param mimeType what the bytes are
     * @param bytes    the image itself
     * @param about    the metadata, copied so the record cannot be changed from outside
     */
    public ToolImage {
        about = about == null ? Map.of() : Map.copyOf(about);
    }

    /**
     * 🏗️ An image described by one detail at a time.
     *
     * @param mimeType what the bytes are
     * @param bytes    the image itself
     * @param name     what it is called
     * @return the image
     */
    public static ToolImage named(String mimeType, byte[] bytes, String name) {
        Map<String, Object> about = new LinkedHashMap<>();

        about.put("name", name);
        about.put("mimeType", mimeType);
        about.put("sizeBytes", bytes.length);

        return new ToolImage(mimeType, bytes, about);
    }
}
