package org.jmouse.query.translate;

import org.jmouse.el.translate.Translator;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Bindings;

import org.jmouse.el.node.Node;

/**
 * The tree as JSON — for reading, and for a screen that wants to draw it.
 *
 * <h2>⚠️ A destination on the same seam, which is the whole reason it is cheap</h2>
 *
 * <p>It implements {@link Translator} like SQL and jMQ do, so it costs a class rather than a mechanism.
 * That is also what it demonstrates: if compiling for a vendor and writing back out as jMQ really are
 * one operation with a different destination, then a third destination should be exactly this small.
 * The day it is not, something has grown that should not have.</p>
 *
 * <h2>⚠️ Nothing reads it back, and nothing will</h2>
 *
 * <p>This is a view of the shape — see {@link Outline}. A reader for it would be a second front end for
 * the language, competing with the one that decides what a query means. The machine-readable form of a
 * query is jMQ, which already round-trips.</p>
 *
 * <p>⚠️ Bindings are ignored for the same reason {@link JmqTranslator} ignores them: substituting a
 * caller's value into a rendering of the tree produces a document that is correct for one person and
 * wrong for everybody else.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonTranslator implements Translator<String> {

    /** ⚠️ Honest: this writes down whatever tree it is handed, so there is no clause it must refuse. */
    private static final Capabilities CAPABILITIES = Capabilities.everything("json");

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
        String inner  = "  ".repeat(depth + 1);

        written.append("{\n")
                .append(inner).append("\"kind\": ").append(quoted(outline.kind()));

        if (outline.source() != null) {
            written.append(",\n").append(inner).append("\"source\": ").append(quoted(outline.source()));
        }

        if (!outline.children().isEmpty()) {
            written.append(",\n").append(inner).append("\"children\": [\n");

            for (int index = 0; index < outline.children().size(); index++) {
                written.append("  ".repeat(depth + 2));
                write(outline.children().get(index), written, depth + 2);
                written.append(index == outline.children().size() - 1 ? "\n" : ",\n");
            }

            written.append(inner).append("]");
        }

        written.append("\n").append(indent).append("}");
    }

    /**
     * ⚠️ Escaped by hand rather than by a JSON library, because this module has none and acquiring one
     * to quote two fields would put a serialisation dependency into the language for the sake of a
     * viewer. The five characters JSON requires are the five handled here; a sixth would be a bug and is
     * why this is one method rather than a format string at each call site.
     */
    private String quoted(String value) {
        StringBuilder written = new StringBuilder("\"");

        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> written.append("\\\"");
                case '\\' -> written.append("\\\\");
                case '\n' -> written.append("\\n");
                case '\r' -> written.append("\\r");
                case '\t' -> written.append("\\t");
                default -> {
                    if (character < 0x20) {
                        written.append("\\u%04x".formatted((int) character));
                    } else {
                        written.append(character);
                    }
                }
            }
        }

        return written.append('"').toString();
    }
}
