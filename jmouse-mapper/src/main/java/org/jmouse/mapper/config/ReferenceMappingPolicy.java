package org.jmouse.mapper.config;

/**
 * What to do when a source object is reached again while it is still being mapped. 🔄
 *
 * <p>A parent holding its children while each child holds its parent is the ordinary shape of an
 * entity graph, and following it naively costs the stack. Only the <em>ancestor chain</em> counts as a
 * cycle here: two siblings pointing at one shared address are not one, and each is mapped on its own.</p>
 */
public enum ReferenceMappingPolicy {

    /**
     * Write {@code null} where the graph turns back on itself, producing a tree.
     *
     * <p>The default, because a mapped object is usually on its way to being serialized, and the
     * back-reference is redundant - it points at something the result already carries. A cyclic result
     * would simply move the failure to whatever writes it out.</p>
     */
    BREAK,

    /**
     * Reuse the target already being built, so the result has the same shape as the source, cycle
     * included.
     *
     * <p>Correct when the graph itself is the point, and the caller is not going to serialize it.</p>
     *
     * <p>⚠️ Where the same source is revisited as a <em>different</em> target type, there is nothing to
     * reuse and the reference is broken as under {@link #BREAK}.</p>
     */
    PRESERVE,

    /**
     * Refuse, with the property path and the two types that close the loop.
     */
    FAIL
}
