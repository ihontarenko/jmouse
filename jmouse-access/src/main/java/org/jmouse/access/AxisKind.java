package org.jmouse.access;

/**
 * One of the questions a request is asked, as something the product declares rather than something the
 * engine contains.
 *
 * <p>{@code AccessAxis} used to be the whole answer: a closed enum of five, two of which are what
 * "may I" means anywhere and three of which might be one product's readings of a question about a
 * <em>place</em>. A library cannot ship those three, and a product with a sixth question had nowhere
 * to put it except in the middle of the other five.
 *
 * <p>So the set of axes is a registration and this is what one looks like. The engine collects the
 * beans that answer them, runs them in {@link #order()} and stops at the first refusal; which axes
 * exist is {@link AxisCatalog}.
 *
 * <p><strong>Implement it with an enum</strong>, for the same reason {@link ScopeKind} is: the
 * declaration order is the running order, so writing the axes down in sequence is writing the sequence
 * down, and {@code ordinal()} is {@link #order()}.
 */
public interface AxisKind {

    /** What this axis is called — in a refusal, in the debug line and in the control room. */
    String name();

    /**
     * Where this axis runs: smaller is earlier, and earlier means outermost.
     *
     * <p>The whole value of a verdict naming an axis is that it names the <em>outermost</em> reason.
     * Somebody without a permission, in a workspace whose plan does not include the module, must be
     * told about the plan, because telling them to ask for a permission sends them to somebody who
     * cannot help. An order that emerged from bean discovery would make that a coincidence, which is
     * why it is declared here and not with {@code @Order}.
     */
    int order();

    /**
     * Whether an installation with no bean for this axis should refuse to start.
     *
     * <p>True for the axes that are the model's. A missing identity or permission axis is a question
     * silently answered yes, which is the one failure mode the registry exists to prevent; a missing
     * module axis is a product that has no modules, which is a legitimate thing to be.
     */
    boolean required();
}
