package org.jmouse.ai;

import java.util.Base64;

/**
 * The bytes, carried in the call itself.
 *
 * <h2>⚠️ The one source every installation has, and the one to prefer</h2>
 *
 * <p>A person drops a photograph into a conversation and the client is holding its bytes — there is no
 * address anywhere and no file on the server, so nothing else can answer. It needs no configuration and
 * no filesystem, which is why it is the fallback the other sources' refusals point back at.
 *
 * <p>It costs what it costs: base64 inflates by about a third, so a photograph of a component is a few
 * hundred kilobytes of conversation. That is a real cost and a reasonable one; a source that avoids it
 * is an optimisation over this rather than a replacement for it.
 */
public final class EncodedFileSource implements ToolFileSource {

    /** The argument name, published so a tool's own wording cannot drift from the schema's. */
    public static final String ARGUMENT = "base64";

    @Override
    public String argument() {
        return ARGUMENT;
    }

    @Override
    public String description() {
        return "The bytes, base64-encoded. This works everywhere and is the one to use unless you know "
             + "another is enabled.";
    }

    @Override
    public boolean carriedBy(ToolInvocation invocation) {
        return invocation.optionalString(ARGUMENT).filter(sent -> !sent.isBlank()).isPresent();
    }

    @Override
    public byte[] read(ToolInvocation invocation) {
        String encoded = invocation.requiredString(ARGUMENT).trim();

        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException malformed) {
            // ⚠️ The data: prefix is named because it is what a client sends when it has copied an
            // image out of a browser, and the decoder's own message ("Illegal base64 character 3a")
            // is one nobody can act on.
            throw new ToolRefusedException(RefusalReason.UNPARSEABLE_VALUE,
                    "'" + ARGUMENT + "' is not base64. Send the file's bytes encoded, with no data: "
                    + "prefix and no line breaks.");
        }
    }
}
