package org.jmouse.query.translate;

import org.jmouse.el.node.Node;
import org.jmouse.query.el.node.ClauseKind;
import org.jmouse.query.el.node.ClauseNode;
import org.jmouse.query.el.node.QueryBlockNode;

/**
 * One way out of the tree — into SQL for a vendor, into a pipeline over rows, or back into jMQ itself.
 *
 * <h2>⚠️ There is one seam, not two</h2>
 *
 * <p>This was called an adapter, and a second thing called a translator was very nearly added beside it:
 * an adapter compiled a block for a backend, a translator would have rendered a tree into text. They are
 * the same operation with a different destination, and two names for one seam is exactly how an un-parse
 * and a compiler drift apart until a query written by one and read by the other means two things.</p>
 *
 * <h2>⚠️ Any node, not only a block</h2>
 *
 * <p>Hand it a whole {@code view} and it renders a statement. Hand it one {@code where} and it renders a
 * predicate a product can splice into a statement it built elsewhere — with the values bound, so the
 * splice is a fragment and its parameters rather than a string somebody has to escape. That is what
 * "translate only part of it" means, and taking {@link Node} rather than {@link QueryBlockNode} is the
 * whole of what it costs.</p>
 *
 * <h2>⚠️ The result type belongs to the translator</h2>
 *
 * <p>{@code <T>} is deliberately not a shape this interface names: SQL produces text and bound values,
 * a row backend produces a pipeline, rendering back into the language produces a string. A common return
 * type would have to be the union of all of them — which is a type nobody can use without first asking
 * what it really is, and a cast at that boundary is a design failure rather than a convenience.</p>
 *
 * <h2>⚠️ Translating is not running</h2>
 *
 * <p>A translator produces something the caller executes. It holds no connection, no transaction and no
 * cursor, and it never opens one. A query engine that owns a connection is a query engine that owns
 * pooling, timeouts, cancellation and streaming, none of which is anybody's idea of a language.</p>
 *
 * @param <T> what translating produces for this destination
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Translator<T> {

    /** What this destination can honour — see {@link Capabilities}. */
    Capabilities capabilities();

    /**
     * Translates a node, refusing anything the capabilities do not cover.
     *
     * @param node     a document, a view, a function body, or one clause of one
     * @param bindings what the caller supplies by name
     * @return the translated form
     */
    T translate(Node node, Bindings bindings);

    /**
     * Translates a node that needs nothing supplied.
     *
     * @param node what to translate
     * @return the translated form
     */
    default T translate(Node node) {
        return translate(node, Bindings.none());
    }

    /**
     * Refuses every clause this translator cannot honour, before translating any of them.
     *
     * <h2>⚠️ Up front, and never silently</h2>
     *
     * <p>Checked before anything is built, so a document asking for two things this destination lacks is
     * told about the first rather than handed half a statement. And <strong>refused</strong> rather than
     * ignored: quietly dropping a {@code group} and returning ungrouped rows is the bug this whole area
     * exists to make impossible to write by accident.</p>
     *
     * <p>⚠️ It asks each clause what it needs rather than knowing the clauses itself, so a clause added
     * to the language costs no edit here and none in any translator.</p>
     *
     * @param block what was asked for
     */
    default void requireSupport(QueryBlockNode block) {
        Capabilities capabilities = capabilities();

        for (ClauseNode clause : block.getClauses()) {
            ClauseKind kind = clause.kind();

            capabilities.require(kind.capability(), kind.keyword());
        }
    }
}
