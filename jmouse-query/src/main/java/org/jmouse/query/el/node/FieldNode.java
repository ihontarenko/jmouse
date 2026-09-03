package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.query.el.SourceWriter;

import java.util.List;

/**
 * One attribute of a {@code structure} — a name, a type, and sometimes a default.
 *
 * <pre>
 *   key:      string
 *   priority: int, default: 3
 *   tags:     string[]
 * </pre>
 *
 * <h2>⚠️ The type is a promise, and {@code unknown} is an admission</h2>
 *
 * <p>{@code text}, {@code int}, {@code number}, {@code temporal} and {@code boolean} are believed and
 * compared accordingly. {@code unknown} says nobody knows what is in there — and an ordered comparison
 * over it is refused until a converter says how to read it, because as words {@code "900"} is greater
 * than {@code "1000"}.</p>
 *
 * <h2>⚠️ A default belongs here and never to a mapping</h2>
 *
 * <p>It is part of what the shape promises. Two mappings of one structure disagreeing about a default
 * would be a difference nobody could see from either file.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FieldNode extends AbstractExpression {

    /** What may be written after the colon. {@code string} and {@code text} are one type, spelled twice. */
    public static final List<String> TYPES =
            List.of("string", "text", "int", "number", "boolean", "temporal", "unknown");

    private String     name;
    private String     label;
    private String     type;
    private boolean    collection;
    private Expression defaultValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * What a PERSON reads where a query writes {@link #getName()} — or {@code null} where they are the
     * same word.
     *
     * <h2>⚠️ It belongs to the structure, for the same reason the type does</h2>
     *
     * <p>A label is a fact about the shape, not about where the shape is stored. Two mappings of one
     * structure disagreeing about what an attribute is CALLED would be a difference visible from
     * neither file — which is precisely the argument this class already makes about a default.</p>
     *
     * <p>⚠️ <strong>Never queryable.</strong> A query writes the name; this is what a builder shows.
     * Accepting it as a second spelling would mean a saved query stops parsing the day somebody
     * improves the wording.</p>
     */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** The type as it was written — {@code string} stays {@code string} so a document round-trips. */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * The type the rest of the engine works in.
     *
     * <p>⚠️ {@code string} and {@code int} are the standard's spellings; the compiler has always known
     * {@code text} and {@code number}. Normalising here rather than at a dozen call sites is what keeps
     * one spelling from becoming a second type.</p>
     */
    public String getCanonicalType() {
        return switch (type) {
            case "string" -> "text";
            case "int" -> "number";
            default -> type;
        };
    }

    /** Whether it holds many values — {@code tags: string[]}. */
    public boolean isCollection() {
        return collection;
    }

    public void setCollection(boolean collection) {
        this.collection = collection;
    }

    public Expression getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Expression defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * ⚠️ The trailing facts are written in a fixed order — label, then default — so a document that is
     * read and written back out is byte-identical to one somebody typed in the same order. Emitting them
     * in whatever order they were parsed would make a round trip depend on the input, which is the one
     * thing a stored document must not do.
     */
    @Override
    public String toSource() {
        StringBuilder written = new StringBuilder(
                "%s: %s%s".formatted(SourceWriter.name(name), type, collection ? "[]" : ""));

        if (label != null && !label.isBlank()) {
            written.append(", label: ").append(SourceWriter.literal(label));
        }

        if (defaultValue != null) {
            written.append(", default: ").append(defaultValue.toSource());
        }

        return written.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
