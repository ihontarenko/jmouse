package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockNode;

import org.jmouse.el.writer.SourceWriter;

/**
 * A block whose body is jMS — {@code behaviour}, {@code run} —: the script's statements as jMS reads them, and its body as
 * the author wrote it, which is what it writes back, since jMS's nodes do not write themselves.
 */
public class ScriptBlockNode extends BlockNode {

    private final String word;
    private String written = "";

    public ScriptBlockNode(String word) {
        this.word = word;
    }

    @Override
    public String word() {
        return word;
    }

    /** The body between its braces, as written, dedented. */
    public String written() {
        return written;
    }

    void written(String written) {
        this.written = written;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(word);
        writer.block(body -> body.verbatim(written));
    }
}
