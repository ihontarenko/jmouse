package org.jmouse.query.el.function;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.node.expression.ArrayNode;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.query.el.node.ParameterDeclarationNode;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.WhereNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Replaces a call to a declared function with that function's condition, arguments bound.
 *
 * <pre>{@code
 * function low_stock(threshold : 5) { where entry[quantity] | int < threshold }
 *
 * where low_stock(3) and entry[location] == "склад-А"
 *    ↓
 * where (entry[quantity] | int < 3) and entry[location] == "склад-А"
 * }</pre>
 *
 * <h2>⚠️ Inlined BEFORE the schema is checked, and the order is not arbitrary</h2>
 *
 * <p>A body refers to its parameters by name — {@code threshold} — and a checker asked about
 * {@code threshold} would refuse it as an attribute nothing declares. Inlining first means everything
 * downstream sees one ordinary condition and needs to know nothing about functions at all: the checker,
 * the compiler and any future backend are unchanged by this feature existing.</p>
 *
 * <h2>⚠️ Arguments bind by NAME, not by position, once a default is skipped</h2>
 *
 * <p>Positional binding is right while every parameter is given. The moment one with a default is
 * skipped it stops being right, and stops silently: {@code recent(userIds)} against
 * {@code recent(userIds as int[], days as int : 7)} would bind the ids to {@code days} if positions were
 * counted naively. So a parameter that receives no argument takes its default, and one with neither is
 * refused by name.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FunctionInliner {

    private final QueryDocumentNode document;
    private final Set<String>       contributed;

    public FunctionInliner(QueryDocumentNode document) {
        this(document, Set.of());
    }

    /**
     * @param contributed names a product or backend answers to — {@code now()}, {@code currentUser()}.
     *                    ⚠️ Anything outside this set and outside the document is refused by name rather
     *                    than passed on, because an unresolved call is nearly always a mistyped one.
     */
    public FunctionInliner(QueryDocumentNode document, Set<String> contributed) {
        this.document = document;
        this.contributed = Set.copyOf(contributed);
    }

    /**
     * Inlines every call in every clause of a block.
     *
     * @param block the block to rewrite
     */
    public void inline(QueryBlockNode block) {
        block.getWhere().ifPresent(where -> where.setCondition(expand(where.getCondition(), List.of())));
    }

    /**
     * Inlines every call in one expression.
     *
     * @param expression the expression to rewrite
     * @return the same condition with calls replaced
     */
    public Expression inline(Expression expression) {
        return expand(expression, List.of());
    }

    /**
     * @param chain the functions currently being expanded, outermost first — how recursion is caught
     */
    private Expression expand(Expression expression, List<String> chain) {
        return new Rewriter() {

            @Override
            public Expression visitCall(FunctionNode call) {
                Optional<org.jmouse.query.el.node.FunctionNode> declared = document.getFunction(call.getName());

                if (declared.isEmpty()) {
                    // ⚠️ A name nothing answers to is refused HERE, by name, with the declared ones listed
                    // — because the overwhelmingly common cause is a typo in a function name, and the
                    // alternative was letting it fall through to the compiler, which could only say
                    // "FunctionNode cannot be handled". That sentence tells somebody they were wrong
                    // without telling them what they wrote or what they could have written.
                    if (!contributed.contains(call.getName())) {
                        throw FunctionCallException.unknown(call.getName(), document.getFunctions().stream()
                                .map(org.jmouse.query.el.node.FunctionNode::getName).toList());
                    }

                    // A name a product contributed — `now()`, `currentUser()` — belongs to whatever
                    // evaluates it, and passing it through is what keeps this from being a language that
                    // can only use what it declared itself.
                    return super.visitCall(call);
                }

                return apply(declared.get(), call, chain);
            }
        }.rewrite(expression);
    }

    private Expression apply(org.jmouse.query.el.node.FunctionNode declared, FunctionNode call,
                             List<String> chain) {
        String name = declared.getName();

        if (chain.contains(name)) {
            List<String> cycle = new ArrayList<>(chain);

            cycle.add(name);

            throw FunctionCallException.recursive(cycle);
        }

        WhereNode body = declared.getWhere().orElseThrow(
                () -> FunctionCallException.nothingToInline(name));

        Map<String, Expression> bound = bind(declared, call);
        List<String> deeper = new ArrayList<>(chain);

        deeper.add(name);

        // The body may itself call something, so it is expanded with this function on the chain — which
        // is what makes a mutual cycle (a → b → a) as impossible as a direct one.
        Expression substituted = new Substitution(bound).rewrite(body.getCondition());

        return expand(substituted, deeper);
    }

    /**
     * Matches arguments to parameters, filling defaults.
     */
    private Map<String, Expression> bind(org.jmouse.query.el.node.FunctionNode declared, FunctionNode call) {
        List<ParameterDeclarationNode> parameters = declared.getParameters();
        List<Expression> arguments = arguments(call);

        if (arguments.size() > parameters.size()) {
            throw FunctionCallException.tooManyArguments(
                    declared.getName(), arguments.size(), parameters.size());
        }

        Map<String, Expression> bound = new LinkedHashMap<>();

        for (int index = 0; index < parameters.size(); index++) {
            ParameterDeclarationNode parameter = parameters.get(index);
            Expression value = index < arguments.size() ? arguments.get(index) : parameter.getDefaultValue();

            if (value == null) {
                throw FunctionCallException.missingArgument(declared.getName(), parameter.getName());
            }

            if (index < arguments.size()) {
                checkType(declared.getName(), parameter, value);
            }

            bound.put(parameter.getName(), value);
        }

        return bound;
    }

    /**
     * ⚠️ Checked at the call site, in the caller's own vocabulary, and only where it can be known.
     *
     * <p>A literal argument can be judged now; an expression cannot, and pretending otherwise would mean
     * either refusing valid queries or inventing a type system this language does not have. So the check
     * is deliberately partial — it catches the mistake somebody actually makes, which is passing a word
     * where a list of numbers was declared.</p>
     */
    private void checkType(String function, ParameterDeclarationNode parameter, Expression argument) {
        if (!parameter.hasType()) {
            return;
        }

        String declared = parameter.getType() + (parameter.isCollection() ? "[]" : "");

        if (parameter.isCollection() && !(argument instanceof ArrayNode)) {
            throw FunctionCallException.wrongType(function, parameter.getName(), declared, describe(argument));
        }

        if (!parameter.isCollection() && argument instanceof ArrayNode) {
            throw FunctionCallException.wrongType(function, parameter.getName(), declared, "a list");
        }

        boolean numeric = List.of("int", "long", "short", "byte", "double", "float", "decimal")
                .contains(parameter.getType());

        if (numeric && !parameter.isCollection() && argument instanceof StringLiteralNode) {
            throw FunctionCallException.wrongType(function, parameter.getName(), declared, "a word");
        }
    }

    private String describe(Expression argument) {
        if (argument instanceof StringLiteralNode) {
            return "a word";
        }

        return argument instanceof LiteralNode<?> ? "a single value" : "an expression";
    }

    private List<Expression> arguments(FunctionNode call) {
        List<Expression> collected = new ArrayList<>();

        if (call.getArguments() instanceof ArgumentsNode node) {
            for (Node child : node.getChildren()) {
                if (child instanceof Expression argument) {
                    collected.add(argument);
                }
            }
        } else if (call.getArguments() != null) {
            collected.add(call.getArguments());
        }

        return collected;
    }

    /** Replaces a reference to a parameter with the expression bound to it. */
    private static final class Substitution extends Rewriter {

        private final Map<String, Expression> bound;

        private Substitution(Map<String, Expression> bound) {
            this.bound = bound;
        }

        /**
         * ⚠️ A parameter arrives as a {@link PropertyNode} — the parser cannot tell a parameter from an
         * attribute, and does not have to: whichever names the binding map holds are parameters, and
         * everything else is an attribute the checker will judge afterwards.
         */
        @Override
        public Expression visitProperty(PropertyNode property) {
            Expression value = bound.get(property.getPath());

            return value == null ? property : value;
        }
    }
}
