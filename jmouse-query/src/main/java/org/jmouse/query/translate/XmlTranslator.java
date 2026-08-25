package org.jmouse.query.translate;

import org.jmouse.el.node.Node;

/**
 * The tree as XML — the same shape as {@link JsonTranslator}, for a reader who prefers it that way.
 *
 * <h2>⚠️ It exists because it costs one class, and that is a fact about the seam</h2>
 *
 * <p>Nothing about the language is repeated here: the walk is {@link Outline}'s and the text each node
 * stands for is the node's own. What differs between this and the JSON one is punctuation. A second
 * destination that cost more than punctuation would mean the seam had stopped being a seam.</p>
 *
 * <p>⚠️ For looking at, never for reading back — see {@link Outline}. And bindings are ignored, like
 * every rendering that is not aimed at a backend.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class XmlTranslator implements Translator<String> {

    private static final Capabilities CAPABILITIES = Capabilities.everything("xml");

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public String translate(Node node, Bindings bindings) {
        StringBuilder written = new StringBuilder();

        write(Outline.of(node), written, 0);

        return written.toString();
    }

    private void write(Outline outline, StringBuilder written, int depth) {
        String indent = "  ".repeat(depth);

        written.append(indent).append('<').append(outline.kind());

        // ⚠️ The source goes in an ATTRIBUTE rather than as text, so a node that has both a source and
        // children stays one element. As text it would sit as a sibling of its own children, and an
        // element whose first child is a fragment of itself reads as a parsing mistake.
        if (outline.source() != null) {
            written.append(" source=\"").append(escaped(outline.source())).append('"');
        }

        if (outline.children().isEmpty()) {
            written.append("/>");
            return;
        }

        written.append(">\n");

        for (Outline child : outline.children()) {
            write(child, written, depth + 1);
            written.append('\n');
        }

        written.append(indent).append("</").append(outline.kind()).append('>');
    }

    /**
     * ⚠️ All five predefined entities, including the two that are only strictly required in some
     * positions. A query is full of {@code <}, {@code >} and quotes, and escaping "the ones that matter
     * here" is how an escaper becomes wrong when its output moves.
     *
     * <p>⚠️ <strong>And the line breaks, as character references.</strong> A raw newline in an attribute
     * value is legal and every parser <em>normalises it to a space</em> — so a multi-line source would
     * come back as one long line, silently, with nothing malformed to notice. The whole point of this
     * rendering is that a person can read the shape; collapsing it is exactly the damage.</p>
     */
    private String escaped(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace("\r", "&#13;")
                .replace("\n", "&#10;")
                .replace("\t", "&#9;");
    }
}
