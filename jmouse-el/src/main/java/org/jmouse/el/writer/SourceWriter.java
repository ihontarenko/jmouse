package org.jmouse.el.writer;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * What a node writes its source into — the one way text is made from a tree, for every language on jME. A node says
 * what it is made of: words, the marks between them, texts, numbers, the pieces it holds, its lines and its blocks. The
 * writer decides how they are laid out, by its {@link SourceLayout}: the separator between words, the indentation of a
 * block, its braces, the quote a text takes. A node never concatenates text of its own.
 *
 * <p>Within a line, a {@linkplain #word word} is separated from what came before by the layout's separator; a
 * {@linkplain #join joined} mark is written right after what came before — {@code *}, {@code )}, {@code ,}; an
 * {@linkplain #opening opening} mark is joined too, and the word after it is joined to it — {@code (} in
 * {@code SIN(0 1 1k)}. A line is ended by {@link #endLine()}; ending a line with nothing on it writes nothing, and
 * {@link #blankLine()} writes an empty one.</p>
 */
public interface SourceWriter {

    /** A word; null or empty writes nothing. */
    SourceWriter word(String word);

    /** A mark joined to what came before, with no separator. */
    SourceWriter join(String mark);

    /** A mark joined to what came before, the word after it joined to it: {@code (} in {@code SIN(}. */
    SourceWriter opening(String mark);

    /** A mark that stands as a word, the word after it joined to it: {@code {} in {@code R1 a b {2*R}}. */
    SourceWriter openingWord(String mark);

    /** A text, quoted so the lexer reads it back whole: the layout's quote, or the other one when it holds that. */
    SourceWriter text(String text);

    /** A number as a word, in as few digits as read it back the same: {@code 0.25}, {@code 2}, {@code -1500}. */
    SourceWriter number(BigDecimal number);

    SourceWriter number(double number);

    SourceWriter number(long number);

    /** A piece that writes itself: a node, a value it holds. */
    SourceWriter write(SourceWritable piece);

    /** Pieces, each written in turn. */
    SourceWriter each(Iterable<? extends SourceWritable> pieces);

    /** Pieces with a mark joined between them: {@code a, b, c}. */
    SourceWriter each(Iterable<? extends SourceWritable> pieces, String mark);

    /** Ends the line; nothing is written when it holds nothing. */
    SourceWriter endLine();

    /** An empty line. */
    SourceWriter blankLine();

    /**
     * A block: the layout's opening word ends the line in hand, the body is written a level further in, and the closing
     * stands on a line of its own.
     */
    SourceWriter block(Consumer<SourceWriter> body);

    /** Text written as it is — a script, a body a node keeps verbatim —, each of its lines at the indentation in hand. */
    SourceWriter verbatim(String text);

    /** What has been written; a line not yet ended is in it, without a line break after it. */
    String text();
}
