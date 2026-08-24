package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.query.el.SourceWriter;

/**
 * One parameter of a {@code function} — a name, optionally a type, optionally a default.
 *
 * <pre>
 *   userIds as int[]           a type, no default
 *   threshold : 5              a default, no type
 *   days as int : 7            both
 * </pre>
 *
 * <h2>⚠️ Why the type is written with {@code as} and not with {@code :}</h2>
 *
 * <p>Core jME's {@code ParametersParser} already reads {@code name : expression}, and it reads it as a
 * <strong>default value</strong> — jMT's {@code macro} is built on exactly that. Giving this dialect
 * its own meaning for the same punctuation would produce two sibling languages in which one line means
 * two different things, and nothing would surface it until somebody copied a parameter list from a
 * template into a query.</p>
 *
 * <h2>⚠️ Why a type is worth having at all</h2>
 *
 * <p>Not for validation. A parameter's type is what lets a compiler <strong>bind</strong> rather than
 * substitute: {@code where entry[owner] in userIds} becomes {@code IN (?, ?, ?)} with one bound value
 * per element. Nothing is ever concatenated into the query text, so there is no injection surface to
 * escape — which is a stronger position than escaping one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ParameterDeclarationNode extends AbstractExpression {

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

    /**
     * The declared element type — {@code int}, {@code string}, {@code date} — or {@code null}.
     *
     * <p>⚠️ Held as the <em>element</em> type with {@link #isCollection()} beside it, rather than as the
     * string {@code "int[]"}. A compiler asks two different questions of it — how to convert a value,
     * and whether to expand it into several binds — and a caller that has to strip brackets off a name
     * to answer the second is a caller that will one day forget to.</p>
     *
     * @return the element type, or {@code null} when the parameter was left untyped
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Whether the parameter takes several values — {@code int[]} rather than {@code int}.
     *
     * @return {@code true} for an array type
     */
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

    public boolean hasType() {
        return type != null;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    @Override
    public String toSource() {
        StringBuilder written = new StringBuilder(SourceWriter.name(name));

        if (hasType()) {
            written.append(" as ").append(type);

            if (collection) {
                written.append("[]");
            }
        }

        if (hasDefaultValue()) {
            written.append(" : ").append(defaultValue.toSource());
        }

        return written.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
