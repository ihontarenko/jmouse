package org.jmouse.java.parser.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jmouse.el.lexer.TokenizableSource.Entry;
import org.jmouse.java.parser.source.LineIndex;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

/** Immutable original text and lexical provenance, without exposing a mutable token cursor. */
public final class JavaLexicalSnapshot {
    private final String name;
    private final String text;
    private final List<Entry> tokens;
    private final List<JavaComment> comments;
    private final List<ParseProblem> problems;
    private final LineIndex lines;

    public JavaLexicalSnapshot(JavaSourceText source) {
        Objects.requireNonNull(source, "source");
        name = source.getName();
        text = source.toString();
        List<Entry> entries = new ArrayList<>(source.size());
        for (Entry entry : source) {
            entries.add(entry);
        }
        tokens = List.copyOf(entries);
        comments = source.comments();
        problems = source.problems();
        lines = source.lines();
    }

    public String name() {
        return name;
    }

    public String text() {
        return text;
    }

    public List<Entry> tokens() {
        return tokens;
    }

    public List<JavaComment> comments() {
        return comments;
    }

    public List<ParseProblem> problems() {
        return problems;
    }

    public LineIndex lines() {
        return lines;
    }

    public SourceSpan span(int start, int end) {
        return lines.span(start, end);
    }

    /** First token beginning at or after an original-text offset; EOF is a real final entry. */
    public int tokenIndexAtOrAfter(int offset) {
        Objects.checkFromToIndex(offset, offset, text.length());
        int lower = 0;
        int upper = tokens.size();
        while (lower < upper) {
            int middle = (lower + upper) >>> 1;
            if (tokens.get(middle).offset() < offset) {
                lower = middle + 1;
            } else {
                upper = middle;
            }
        }
        return lower;
    }

    public int endBefore(int offset) {
        int index = tokenIndexAtOrAfter(offset);
        if (index == 0) {
            return 0;
        }
        Entry entry = tokens.get(index - 1);
        return entry.offset() + entry.length();
    }
}
