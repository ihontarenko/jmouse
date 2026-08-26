package org.jmouse.mapper.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.mapper.el.node.LetNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * One block's {@code let} bindings, compiled once and asked by name. 🏷️
 *
 * <h2>⚠️ Compiled here because the alternative was compiling them per mapped object</h2>
 *
 * <p>Every rule's own value is compiled when the file is read. The bindings were not: they were compiled
 * inside the lambda the engine calls for each property of each object, so a block with two bindings and
 * four computed rules paid <strong>eight full lex-parse-build cycles per object mapped</strong>. Nothing
 * in the language asks for that — a binding's text cannot change between two objects.</p>
 *
 * <p>So the compiling happens where the rest of the file's compiling happens, once per block, and what
 * travels to the mapping path is a list of expressions with names.</p>
 *
 * <h2>⚠️ It also answers what a bare name means</h2>
 *
 * <p>{@code buyerName : full} looks exactly like a source path and is not one. {@link #names()} is what
 * {@link SourcePath} consults to tell the two apart — the binder to decide whether a rule can skip the
 * expression machinery entirely, and the validator to decide whether a name is its business to check.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class BlockBindings {

    /** What a block that binds nothing carries, which is most blocks. */
    static final BlockBindings NONE = new BlockBindings(List.of());

    private final List<Bound> bound;
    private final Set<String> names;

    private BlockBindings(List<Bound> bound) {
        Set<String> names = new LinkedHashSet<>();

        for (Bound binding : bound) {
            names.add(binding.name());
        }

        this.bound = bound;
        this.names = names;
    }

    /**
     * Compiles a block's bindings, in the order they were declared.
     *
     * @param declared the {@code let} lines
     * @param compiler what turns text into an expression — the binder's own, so a failure is reported the
     *                 way every other compilation failure in the file is
     * @return the bindings, or {@link #NONE} when there are none
     */
    static BlockBindings of(List<LetNode> declared, Function<LetNode, Expression> compiler) {
        if (declared.isEmpty()) {
            return NONE;
        }

        List<Bound> bound = new ArrayList<>(declared.size());

        // ⚠️ The whole node is handed to the compiler, not its text: a binding that will not compile has
        // to name the line it was written on, and only the node knows that.
        for (LetNode binding : declared) {
            bound.add(new Bound(binding.getName(), compiler.apply(binding)));
        }

        return new BlockBindings(List.copyOf(bound));
    }

    /**
     * What this block binds.
     *
     * @return the names, in declaration order
     */
    Set<String> names() {
        return names;
    }

    /**
     * Evaluates every binding into a context.
     *
     * <p>⚠️ In declaration order, and that is not tidiness: a binding may reference an earlier one, so
     * each is set before the next is evaluated. Sorting them, or evaluating them in parallel, would break
     * a shape the reference document explicitly allows.</p>
     *
     * @param context the context being prepared for one evaluation
     */
    void into(EvaluationContext context) {
        for (Bound binding : bound) {
            context.setValue(binding.name(), binding.expression().evaluate(context));
        }
    }

    /**
     * One binding, past the point where it was text.
     *
     * @param name       what the block called it
     * @param expression what it evaluates to
     */
    private record Bound(String name, Expression expression) {
    }
}
