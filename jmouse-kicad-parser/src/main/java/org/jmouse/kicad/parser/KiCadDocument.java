package org.jmouse.kicad.parser;

import org.jmouse.kicad.parser.node.KiCadList;

/**
 * A KiCad file read: its one root list — {@code (kicad_sch …)}, {@code (kicad_symbol_lib …)} — and what was written
 * after it.
 *
 * @param name     what the file is called, for a refusal to name
 * @param root     its root list
 * @param trailing the blanks after the root list; null for a document a program built
 */
public record KiCadDocument(String name, KiCadList root, String trailing) {

    /** A document a program built. */
    public static KiCadDocument of(String name, KiCadList root) {
        return new KiCadDocument(name, root, null);
    }

    /** What kind of file it is: its root's name — {@code kicad_sch}, {@code kicad_symbol_lib}. */
    public String kind() {
        return root.name();
    }
}
