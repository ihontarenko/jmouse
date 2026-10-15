package org.jmouse.parallax.script.engine;

import java.util.List;

/**
 * One block of a document being written: {@code name { lines }}, or a statement that is a single line of its own —
 * {@code axiom "F--F--F"}, {@code analysis transient for 0.01 s}, a symbol's {@code path "M…"}.
 *
 * @param opening what comes before the braces, or the whole statement
 * @param lines   the block's lines, each without indentation; a line may span several, which keep their own
 */
record TextBlock(String opening, List<String> lines, boolean braced) {

    TextBlock {
        lines = List.copyOf(lines);
    }

    static TextBlock braced(String opening, List<String> lines) {
        return new TextBlock(opening, lines, true);
    }

    static TextBlock statement(String line) {
        return new TextBlock(line, List.of(), false);
    }

    /** The block at {@code indentation}, its lines one level deeper, ending with a line break. */
    String render(String indentation) {
        if (!braced) {
            return indentation + opening + "\n";
        }
        StringBuilder text = new StringBuilder(indentation).append(opening).append(" {\n");
        String inner = indentation + "    ";
        for (String line : lines) {
            for (String physical : line.split("\n", -1)) {
                text.append(physical.isBlank() ? "" : inner + physical).append('\n');
            }
        }
        return text.append(indentation).append("}\n").toString();
    }
}
