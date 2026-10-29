package org.jmouse.el.writer;

import org.jmouse.el.lexer.support.SourceWriting;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.function.Consumer;

/** The {@link SourceWriter} that makes text, laid out as its {@link SourceLayout} says. */
public final class TextSourceWriter implements SourceWriter {

    private static final char LINE_BREAK = '\n';

    private final SourceLayout  layout;
    private final StringBuilder written = new StringBuilder();
    private final StringBuilder line    = new StringBuilder();
    private       int           depth;
    /** Whether the next word is joined to what came before: at the start of a line, and after an opening mark. */
    private       boolean       joinsNext = true;

    public TextSourceWriter(SourceLayout layout) {
        this.layout = layout;
    }

    /** A piece's source, laid out by this layout. */
    public static String render(SourceWritable piece, SourceLayout layout) {
        return new TextSourceWriter(layout).write(piece).text();
    }

    @Override
    public SourceWriter word(String word) {
        if (word == null || word.isEmpty()) {
            return this;
        }
        if (!joinsNext) {
            line.append(layout.wordSeparator());
        }
        line.append(word);
        joinsNext = false;
        return this;
    }

    @Override
    public SourceWriter join(String mark) {
        line.append(mark);
        joinsNext = false;
        return this;
    }

    @Override
    public SourceWriter opening(String mark) {
        line.append(mark);
        joinsNext = true;
        return this;
    }

    @Override
    public SourceWriter openingWord(String mark) {
        word(mark);
        joinsNext = true;
        return this;
    }

    @Override
    public SourceWriter text(String text) {
        return word(SourceWriting.literal(text, layout.quote()));
    }

    @Override
    public SourceWriter number(BigDecimal number) {
        BigDecimal stripped = number.stripTrailingZeros();
        return word(stripped.signum() == 0 ? "0" : stripped.toPlainString());
    }

    @Override
    public SourceWriter number(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            throw new IllegalArgumentException("A number written as source is finite: " + number);
        }
        // BigDecimal.valueOf goes through Double.toString: the shortest digits that read back as the same double.
        return number(BigDecimal.valueOf(number));
    }

    @Override
    public SourceWriter number(long number) {
        return word(Long.toString(number));
    }

    @Override
    public SourceWriter write(SourceWritable piece) {
        piece.writeSource(this);
        return this;
    }

    @Override
    public SourceWriter each(Iterable<? extends SourceWritable> pieces) {
        pieces.forEach(this::write);
        return this;
    }

    @Override
    public SourceWriter each(Iterable<? extends SourceWritable> pieces, String mark) {
        Iterator<? extends SourceWritable> iterator = pieces.iterator();
        while (iterator.hasNext()) {
            write(iterator.next());
            if (iterator.hasNext()) {
                join(mark);
            }
        }
        return this;
    }

    @Override
    public SourceWriter endLine() {
        if (!line.isEmpty()) {
            written.append(layout.indentation().repeat(depth)).append(line).append(LINE_BREAK);
            line.setLength(0);
        }
        joinsNext = true;
        return this;
    }

    @Override
    public SourceWriter blankLine() {
        endLine();
        written.append(LINE_BREAK);
        return this;
    }

    @Override
    public SourceWriter block(Consumer<SourceWriter> body) {
        word(layout.blockOpening()).endLine();
        depth++;
        body.accept(this);
        endLine();
        depth--;
        return word(layout.blockClosing()).endLine();
    }

    @Override
    public SourceWriter verbatim(String text) {
        endLine();
        for (String physical : text.split("\n", -1)) {
            String stripped = physical.endsWith("\r") ? physical.substring(0, physical.length() - 1) : physical;
            if (stripped.isEmpty()) {
                written.append(LINE_BREAK);
            } else {
                written.append(layout.indentation().repeat(depth)).append(stripped).append(LINE_BREAK);
            }
        }
        joinsNext = true;
        return this;
    }

    @Override
    public String text() {
        if (line.isEmpty()) {
            return written.toString();
        }
        return written + layout.indentation().repeat(depth) + line;
    }
}
