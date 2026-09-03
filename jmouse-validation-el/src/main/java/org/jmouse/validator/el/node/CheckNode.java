package org.jmouse.validator.el.node;

import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One check on one field — {@code size(3, 32)}, {@code required stop}, {@code url(host: 'mouser.com')}.
 *
 * <h2>⚠️ A named call, never a bare boolean</h2>
 *
 * <p>This is the property the whole language was chosen for. A check carries a <em>name</em> and its
 * <em>arguments</em>, so anything reading the tree can enumerate it: a form-builder draws
 * {@code size(3, 32)} as two number inputs because it can see there are two of them and what they are
 * called. The alternative shape — {@code value.length &gt;= 3} — is expressible, universal, and can only
 * ever be drawn as a text box, which is the same as not being drawn at all.</p>
 *
 * <p>⚠️ <strong>Arguments are kept as written, not evaluated here.</strong> An argument is an
 * expression, and compiling it needs a plain {@link org.jmouse.el.ExpressionLanguage} whose lexer has
 * never heard of this language's keywords — see {@code ExpressionSlice} for why a value read with the
 * document's own cursor cannot be parsed with the document's own parser.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class CheckNode extends AbstractExpression {

    private final List<String>        positional = new ArrayList<>();
    private final Map<String, String> named      = new LinkedHashMap<>();

    private String  name;
    private String  message;
    private boolean stop;

    /** @return the check's name, as the registry knows it — {@code size}, {@code pattern}, {@code oneOf} */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return arguments given by position, each as it was written; never {@code null}
     */
    public List<String> getPositional() {
        return positional;
    }

    /**
     * Adds an argument given by position.
     *
     * @param expression the argument, as it was written
     */
    public void addPositional(String expression) {
        positional.add(expression);
    }

    /**
     * @return arguments given by name, each as it was written, in the order they were given
     */
    public Map<String, String> getNamed() {
        return named;
    }

    /**
     * Adds an argument given by name.
     *
     * @param key        what it is called
     * @param expression the argument, as it was written
     * @return what was already under that name, or {@code null}
     */
    public String addNamed(String key, String expression) {
        return named.put(key, expression);
    }

    /**
     * @return what to say when this check fails, as an expression, or {@code null} to take the line's
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Whether a failure here silences the rest of its own field's checks.
     *
     * <p>⚠️ Its own field's, and no further. A failed precondition makes its followers noise; silencing
     * a sibling <em>field</em> would make somebody fix a form one round trip at a time.</p>
     *
     * @return whether the check was written {@code stop}
     */
    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    @Override
    public String toString() {
        return name + (positional.isEmpty() && named.isEmpty() ? "" : "(…)") + (stop ? " stop" : "");
    }
}
