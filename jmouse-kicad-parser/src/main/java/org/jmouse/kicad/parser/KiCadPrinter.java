package org.jmouse.kicad.parser;

import org.jmouse.kicad.parser.node.KiCadAtom;
import org.jmouse.kicad.parser.node.KiCadList;
import org.jmouse.kicad.parser.node.KiCadNode;
import org.jmouse.kicad.parser.node.KiCadString;

/**
 * Writes a KiCad tree back as text. A node read from a file is written with the blanks it was read with, so an
 * untouched file comes back byte for byte; a node a program built is laid out as KiCad 8 lays its files out:
 *
 * <ul>
 *     <li>every list on a line of its own, indented a tab a level — {@code (at 1.27 2.54 0)};</li>
 *     <li>except an {@code xy} after another, which stays on its line: {@code (xy 0 0) (xy 2.54 0)};</li>
 *     <li>a list holding lists closes on a line of its own; one holding only words closes where they end.</li>
 * </ul>
 */
public final class KiCadPrinter {

    private static final String INDENT = "\t";
    private static final String POINT = "xy";

    /** The file's text. */
    public String print(KiCadDocument document) {
        StringBuilder text = new StringBuilder();
        list(document.root(), null, 0, text);
        text.append(document.trailing() != null ? document.trailing() : "\n");
        return text.toString();
    }

    /** A node's text alone, as it would stand at the top of a file — for a message, a test, a clipboard. */
    public String print(KiCadNode node) {
        StringBuilder text = new StringBuilder();
        node(node, null, 0, text);
        return text.toString();
    }

    private void node(KiCadNode node, KiCadNode before, int depth, StringBuilder text) {
        switch (node) {
            case KiCadList list -> list(list, before, depth, text);
            case KiCadAtom atom -> text.append(wordLeading(atom.leading(), before)).append(atom.value());
            case KiCadString string -> text.append(wordLeading(string.leading(), before)).append(string.writtenForm());
        }
    }

    /** Before a word: what was written, else a blank between it and the item before it, else nothing. */
    private static String wordLeading(String leading, KiCadNode before) {
        if (leading != null) {
            return leading;
        }
        return before == null ? "" : " ";
    }

    private void list(KiCadList list, KiCadNode before, int depth, StringBuilder text) {
        if (list.leading() != null) {
            text.append(list.leading());
        } else if (before != null) {
            boolean sameLine = list.name().equals(POINT) && before instanceof KiCadList previous && previous.name().equals(POINT);
            text.append(sameLine ? " " : "\n" + INDENT.repeat(depth));
        }
        text.append('(');
        KiCadNode previous = null;
        for (KiCadNode item : list.items()) {
            node(item, previous, depth + 1, text);
            previous = item;
        }
        if (list.closingLeading() != null) {
            text.append(list.closingLeading());
        } else if (list.items().stream().anyMatch(KiCadList.class::isInstance)) {
            text.append('\n').append(INDENT.repeat(depth));
        }
        text.append(')');
    }
}
