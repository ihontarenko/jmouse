package org.jmouse.ai.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * What a handler returned, as the string a tool result carries.
 *
 * <p>⚠️ <strong>The invariant this class exists to hold: nothing but JSON of a handler's return value
 * leaves this process.</strong> Never an entity, never an exception, never a stack. It is worth a stated
 * rule rather than an emergent property, because the way it breaks is somebody returning a domain
 * object from a handler that happens to serialise — and what leaves is then every field that object
 * has, including the ones nobody meant to publish.
 *
 * <p>A value that cannot be written is a bug in a handler, not a failure of the call, so it is reported
 * as text saying exactly that rather than becoming a refusal — a refusal would tell the model to try
 * something different, and there is nothing different for it to try.
 */
final class ToolResultJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolResultJson() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception unwritable) {
            return "The tool ran and its result could not be written as JSON ("
                 + unwritable.getMessage() + "). Whatever it did, it did — this is a fault in the tool, "
                 + "not in the request, and calling it again will produce the same thing.";
        }
    }
}
