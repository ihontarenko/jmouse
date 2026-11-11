package org.jmouse.cad.document;

import org.jmouse.el.grammar.StatementWriting;
import org.jmouse.el.grammar.TermWriter;
import org.jmouse.el.node.Expression;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;

import java.util.List;
import java.util.function.Consumer;

/**
 * Writes a document of the jM language as terms — {@code component:diode("Diode", type: diode) { … }} — from the same tree
 * its own writing comes from: the grammar library's {@link TermWriter} for every line a grammar reads, and the document's
 * own statements — its head, its declarations, its {@code meta} — here. Equations and jMS bodies are written as they are:
 * mathematics and behaviour are no terms.
 */
public final class TermDocumentWriter extends TermWriter {

    public TermDocumentWriter() {
        super();
    }

    /** @param flat whether a block that only gathers terms is left out, its terms standing in the document's body */
    public TermDocumentWriter(boolean flat) {
        super(flat);
    }

    /** A document as terms. */
    public String write(DocumentNode document) {
        StringBuilder text = new StringBuilder();
        TextSourceWriter writer = new TextSourceWriter(SourceLayout.WORDS);
        StatementWriting.leading(writer, document);
        term(writer, document.kind().word(), document.slug(), head(document));
        writer.block(body -> statements(body, document.getExpressions()));
        text.append(writer.text());
        return text.toString().stripTrailing() + "\n";
    }

    private List<Consumer<SourceWriter>> head(DocumentNode document) {
        Consumer<SourceWriter> title = writer -> writer.text(document.title());
        return document.type().<List<Consumer<SourceWriter>>>map(role -> List.of(title, writer -> writer.word("type")
                .join(COLON).word(role))).orElse(List.of(title));
    }

    @Override
    protected void other(SourceWriter writer, Expression statement) {
        switch (statement) {
            case DeclarationNode declaration -> declaration(writer, declaration);
            case MetaNode meta -> meta(writer, meta);
            default -> super.other(writer, statement);
        }
    }

    /** {@code extends:diode}, {@code use:[smooth-transitions, junctions]}. */
    private void declaration(SourceWriter writer, DeclarationNode declaration) {
        List<String> names = declaration.names();
        writer.word(declaration.word()).join(COLON);
        if (names.size() == 1) {
            writer.join(names.getFirst());
            return;
        }
        writer.join("[");
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                writer.join(",").word(names.get(index));
            } else {
                writer.join(names.get(index));
            }
        }
        writer.join("]");
    }

    /** {@code meta { summary: "…" }}: each field a named value, a group its lines in braces. */
    private void meta(SourceWriter writer, MetaNode meta) {
        writer.word(meta.word());
        writer.block(body -> {
            for (MetaLineNode line : meta.lines()) {
                StatementWriting.leading(body, line);
                body.word(line.key()).join(COLON);
                if (line.group().isEmpty()) {
                    body.word(line.written());
                } else {
                    body.block(group -> line.group().forEach(member -> group.word(member).endLine()));
                }
                body.endLine();
            }
        });
    }
}
