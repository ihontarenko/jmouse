package org.jmouse.sexpression.node;

/**
 * One node of an S-expression file's S-expressions: a list, an atom or a text.
 *
 * <p>A node read from a file keeps the blanks written before it — {@link #leading()} —, so a file nobody changed is
 * printed back byte for byte. A node a program built has none, and the printer lays it out as KiCad does.</p>
 */
public sealed interface SExpressionNode permits SExpressionList, SExpressionAtom, SExpressionString {

    /** Where it was read; {@link SExpressionPosition#NONE} for a node a program built. */
    SExpressionPosition position();

    /** The blanks written before it, as they were; null for a node a program built. */
    String leading();
}
