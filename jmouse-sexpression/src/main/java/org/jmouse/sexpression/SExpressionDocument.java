package org.jmouse.sexpression;

import org.jmouse.sexpression.node.SExpressionList;

/**
 * An S-expression file read: its one root list — {@code (kicad_sch …)}, {@code (kicad_symbol_lib …)} — and what was written
 * after it.
 *
 * @param name     what the file is called, for a refusal to name
 * @param root     its root list
 * @param trailing the blanks after the root list; null for a document a program built
 */
public record SExpressionDocument(String name, SExpressionList root, String trailing) {

    /** A document a program built. */
    public static SExpressionDocument of(String name, SExpressionList root) {
        return new SExpressionDocument(name, root, null);
    }

    /** What kind of file it is: its root's name — {@code kicad_sch}, {@code kicad_symbol_lib}. */
    public String kind() {
        return root.name();
    }
}
