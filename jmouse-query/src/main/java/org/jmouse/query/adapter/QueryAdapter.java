package org.jmouse.query.adapter;

import org.jmouse.query.el.node.QueryBlockNode;

/**
 * A backend jMQ can be compiled for — SQL, an in-memory evaluator, anything else.
 *
 * <p>⚠️ The compiled form is deliberately {@code <R>} rather than a shape this interface names: SQL
 * produces text and bound values, an in-memory evaluator produces a predicate, a document store would
 * produce a filter object. A common return type would have to be the union of all of them, which is a
 * type nobody can use without asking what it really is.</p>
 *
 * <p>Every adapter is asked to {@link #capabilities()} first, and refusing what it cannot do is part of
 * the contract — see {@link Capabilities}.</p>
 *
 * @param <R> what compiling produces for this backend
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface QueryAdapter<R> {

    /** What this backend can honour. */
    Capabilities capabilities();

    /**
     * Compiles a block for this backend, refusing anything the capabilities do not cover.
     *
     * @param block a checked view or function body
     * @return the compiled form
     */
    R compile(QueryBlockNode block);

    /**
     * Refuses every clause this adapter cannot honour, before compiling any of them.
     *
     * <p>⚠️ Checked up front rather than as each clause is reached: a document asking for two things the
     * adapter lacks should be told about the first one and not have half a statement built for it.</p>
     *
     * @param block what was asked for
     */
    default void requireSupport(QueryBlockNode block) {
        Capabilities capabilities = capabilities();

        if (block.getWhere().isPresent()) {
            capabilities.require(Capabilities.Feature.FILTER, "where");
        }

        if (block.getOrder().isPresent()) {
            capabilities.require(Capabilities.Feature.SORT, "order");
        }

        if (block.getColumns().isPresent()) {
            capabilities.require(Capabilities.Feature.PROJECT, "columns");
        }

        if (block.getGroup().isPresent()) {
            capabilities.require(Capabilities.Feature.AGGREGATE, "group");
        }

        if (block.getHaving().isPresent()) {
            capabilities.require(Capabilities.Feature.AGGREGATE, "having");
        }
    }
}
