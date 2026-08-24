package org.jmouse.query.el.node;

import org.jmouse.query.el.SourceWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code function name(parameters) { … }} — a named, parameterised query.
 *
 * <pre>
 *   function low_stock(threshold : 5) {
 *     where entry[quantity] | int &lt; threshold
 *   }
 * </pre>
 *
 * <h2>⚠️ A body is one query, not a procedure</h2>
 *
 * <p>No loops, no recursion, no assignment — the parser admits clauses and nothing else, so the
 * language stays <strong>total</strong>: every expression terminates. That is not tidiness. A body that
 * could loop would need a sandbox with timeouts and memory limits around every evaluation, and the
 * price of the whole feature would jump by an order of magnitude. Refusing the shape is what removes
 * the sandbox from the design.</p>
 *
 * <h2>⚠️ Scope is never a parameter</h2>
 *
 * <p>A function cannot take a workspace, a tenant, or an owner-to-act-as. Those are injected by
 * whatever invokes the query, from the caller's own context. A stored function that could name its own
 * scope would be a way to read somebody else's data, and no validation at the entry point catches it —
 * the parameter list looks perfectly ordinary.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FunctionNode extends QueryBlockNode {

    private static final String INDENT = "  ";

    private final List<ParameterDeclarationNode> parameters = new ArrayList<>();

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addParameter(ParameterDeclarationNode parameter) {
        parameters.add(parameter);
    }

    public List<ParameterDeclarationNode> getParameters() {
        return List.copyOf(parameters);
    }

    @Override
    public String toSource() {
        String written = parameters.stream()
                .map(ParameterDeclarationNode::toSource)
                .collect(Collectors.joining(", "));
        String body = clausesToSource(INDENT);

        if (body.isEmpty()) {
            return "function %s(%s) { }".formatted(SourceWriter.name(name), written);
        }

        return "function %s(%s) {\n%s\n}".formatted(SourceWriter.name(name), written, body);
    }

    @Override
    public String toString() {
        return "function %s/%d".formatted(name, parameters.size());
    }
}
