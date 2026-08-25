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
    private String     type;
    private boolean    collection;
    private Expression defaultValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toSource() {
        String written = "%s: %s%s".formatted(SourceWriter.name(name), type, collection ? "[]" : "");

        return defaultValue == null ? written : written + ", default: " + defaultValue.toSource();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
