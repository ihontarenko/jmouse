package org.jmouse.script.el.host;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.ExpressionVisitor;
import org.jmouse.el.node.Expressions;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.*;
import org.jmouse.el.node.expression.unary.NegateUnaryOperation;
import org.jmouse.script.el.SourceSpan;

import java.util.Set;

/**
 * Walks one expression and refuses every name the host did not declare.
 *
 * <h2>⚠️ Closed by construction, not by a list of things to reject</h2>
 *
 * <p>The base visitor sends anything it was not taught to {@code visitUnsupported}, which throws. That
 * is inherited here on purpose and it is the whole safety argument: this class enumerates the
 * expression shapes a script is <strong>allowed</strong> to contain, and every other shape — a lambda,
 * a placeholder, a variable alias, and whatever the expression language grows next year — is refused
 * at load rather than passed through unexamined.</p>
 *
 * <p>An audit written the other way round, as a list of forbidden shapes, is correct exactly until
 * somebody adds a node type to jmouse-el. Then it reports a document clean because it has never heard
 * of the thing it just walked past.</p>
 *
 * <p>Positions come from the enclosing <em>statement</em> rather than from the expression itself:
 * expression nodes carry no span, and a line is what somebody editing the file needs anyway.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class CatalogueAudit implements ExpressionVisitor<Void> {

    private final ScriptCatalogue catalogue;
    private final Set<String>     functions;
    private final Set<String>     filters;
    private final Set<String>     tests;

    private SourceSpan at = SourceSpan.none();

    /**
     * Constructs an audit over one host's catalogue.
     *
     * @param catalogue what the host declared
     * @param functions every function name that may be called — the host's, the script's own, and the
     *                  dialect's built-ins, already merged
     * @param filters   the filter names the dialect registers
     * @param tests     the test names the dialect registers
     */
    CatalogueAudit(ScriptCatalogue catalogue, Set<String> functions, Set<String> filters, Set<String> tests) {
        this.catalogue = catalogue;
        this.functions = functions;
        this.filters = filters;
        this.tests = tests;
    }

    /**
     * Audits one expression, reporting anything wrong at the given position.
     *
     * @param expression the expression to walk, or {@code null} where there is none
     * @param at         where the statement holding it was written
     */
    void audit(Expression expression, SourceSpan at) {
        if (expression == null) {
            return;
        }

        this.at = at;

        Expressions.walk(expression, this);
    }

    @Override
    public Void visitBeanAccess(BeanAccessNode access) {
        if (!catalogue.declaresFacade(access.getBean())) {
            throw new ScriptBindException(at, "'@%s' is not a facade this host declared; it offers %s"
                    .formatted(access.getBean(), catalogue.facadeNames()));
        }

        if (access.getAction() instanceof BeanAccessNode.MethodCall(String ignored, Expression arguments)) {
            walk(arguments);
        }

        return null;
    }

    /**
     * ⚠️ A scoped call is <strong>not</strong> a facade call. {@code entry.distanceTo(other)} invokes a
     * method on a value the host itself put in the context for this event; the scope is a variable, not
     * an {@code @} name, so the catalogue has nothing to say about it. What such a value exposes is
     * decided by what the host chose to hand over.
     */
    @Override
    public Void visitScopedCall(ScopedCallNode call) {
        return walk(call.getArguments());
    }

    @Override
    public Void visitCall(FunctionNode call) {
        if (!functions.contains(call.getName())) {
            throw new ScriptBindException(at, "'%s' is not a function this script can call; available: %s"
                    .formatted(call.getName(), functions));
        }

        return walk(call.getArguments());
    }

    @Override
    public Void visitFilter(FilterNode filter) {
        if (!filters.contains(filter.getName())) {
            throw new ScriptBindException(at, "'%s' is not a filter this dialect provides; available: %s"
                    .formatted(filter.getName(), filters));
        }

        walk(filter.getLeft());

        return walk(filter.getArguments());
    }

    @Override
    public Void visitTest(TestNode test) {
        if (!tests.contains(test.getName())) {
            throw new ScriptBindException(at, "'%s' is not a test this dialect provides; available: %s"
                    .formatted(test.getName(), tests));
        }

        walk(test.getLeft());

        return walk(test.getArguments());
    }

    @Override
    public Void visitBinary(BinaryOperation operation) {
        walk(operation.getLeft());

        return walk(operation.getRight());
    }

    @Override
    public Void visitMembership(InOperationNode membership) {
        walk(membership.getLeft());

        return walk(membership.getRight());
    }

    @Override
    public Void visitFallback(NullSafeFallbackNode fallback) {
        walk(fallback.getNullable());

        return walk(fallback.getOtherwise());
    }

    @Override
    public Void visitTernary(TernaryNode ternary) {
        walk(ternary.getCondition());
        walk(ternary.getThenBranch());

        return walk(ternary.getElseBranch());
    }

    @Override
    public Void visitNegation(NegateUnaryOperation negation) {
        return walk(negation.getOperand());
    }

    @Override
    public Void visitUnary(UnaryOperation unary) {
        return walk(unary.getOperand());
    }

    @Override
    public Void visitKeyValue(KeyValueNode entry) {
        walk(entry.getKey());

        return walk(entry.getValue());
    }

    @Override
    public Void visitArray(ArrayNode array) {
        return walkChildren(array);
    }

    @Override
    public Void visitArguments(ArgumentsNode arguments) {
        return walkChildren(arguments);
    }

    @Override
    public Void visitMap(MapNode map) {
        return walkChildren(map);
    }

    /** A property path reads what the host put in the context. Nothing to check, and nothing to reach. */
    @Override
    public Void visitProperty(PropertyNode property) {
        return null;
    }

    /** A literal is a literal. */
    @Override
    public Void visitLiteral(LiteralNode<?> literal) {
        return null;
    }

    /** A range is two numbers. */
    @Override
    public Void visitRange(RangeNode range) {
        return null;
    }

    /**
     * ⚠️ Everything this class was not taught, refused with a position.
     *
     * <p>Inherited behaviour would throw an {@link UnsupportedOperationException}, which is right about
     * the situation and useless to whoever is holding the file — so it is restated here as the load
     * failure it actually is.</p>
     */
    @Override
    public Void visitUnsupported(Expression expression) {
        throw new ScriptBindException(at, "a script may not contain a '%s'; the expressions a script is "
                .formatted(expression == null ? "nothing" : expression.getClass().getSimpleName())
                + "made of are a closed set, and this is not one of them");
    }

    private Void walk(Expression expression) {
        if (expression != null) {
            Expressions.walk(expression, this);
        }

        return null;
    }

    private Void walkChildren(Node node) {
        for (Node child : node.getChildren()) {
            if (child instanceof Expression expression) {
                walk(expression);
            }
        }

        return null;
    }
}
