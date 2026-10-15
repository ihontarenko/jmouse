package org.jmouse.java.parser.lexer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.java.parser.source.LineIndex;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * One Java source text as a jME {@link TokenizableSource}: the original characters, unchanged, plus what
 * tokenising them recorded.
 *
 * <p>The tokenizer records one {@link TokenizableSource.Entry entry} per token, in token order, so entry
 * {@code n} belongs to the token with {@link Token#ordinal() ordinal} {@code n}. The entry's length is
 * the token's length in the <em>original</em> text, which can differ from its value's length when the
 * token contains Unicode escapes — that is where a token's end offset comes from, since {@link Token}
 * carries only its start.
 *
 * <p>Comments and lexical problems are recorded here too: the jME {@link org.jmouse.el.lexer.Lexer}
 * contract returns a cursor over tokens only, and the source is the one object that travels with it.
 *
 * <p>Lines and columns come from a {@link LineIndex} built once, so each lookup is a binary search.
 * Tokenise a source once: it refuses a second tokenizer run.
 */
public final class JavaSourceText implements TokenizableSource {

    private final String             name;
    private final CharSequence       original;
    private final LineIndex          lines;
    private final List<Entry>        entries  = new ArrayList<>();
    private final List<JavaComment>  comments = new ArrayList<>();
    private final List<ParseProblem> problems = new ArrayList<>();

    /**
     * Wraps a text.
     *
     * @param name     a name for messages — a path or file name
     * @param original the text exactly as stored
     */
    public JavaSourceText(String name, CharSequence original) {
        this.name = name;
        this.original = original;
        this.lines = LineIndex.of(original);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLineNumber(int offset) {
        return lines.position(Math.max(0, Math.min(offset, original.length()))).line();
    }

    @Override
    public int getColumnNumber(int offset) {
        return lines.position(Math.max(0, Math.min(offset, original.length()))).column();
    }

    @Override
    public void entry(int offset, int length, Token.Type token) {
        entries.add(new Entry(offset, length, token, original.subSequence(offset, offset + length).toString()));
    }

    @Override
    public Entry get(int index) {
        return entries.get(index);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public Iterator<Entry> iterator() {
        return List.copyOf(entries).iterator();
    }

    @Override
    public int length() {
        return original.length();
    }

    @Override
    public char charAt(int index) {
        return original.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return original.subSequence(start, end);
    }

    @Override
    public String toString() {
        return original.toString();
    }

    /**
     * Returns the line index of the original text.
     *
     * @return the index
     */
    public LineIndex lines() {
        return lines;
    }

    /**
     * Returns where a token ends in the original text.
     *
     * @param token a token this source's tokenizer produced
     * @return the exclusive end offset
     */
    public int endOffset(Token token) {
        Entry entry = entries.get(token.ordinal());
        return entry.offset() + entry.length();
    }

    /**
     * Returns the span a token covers in the original text.
     *
     * @param token a token this source's tokenizer produced
     * @return its span
     */
    public SourceSpan span(Token token) {
        return lines.span(token.offset(), endOffset(token));
    }

    /**
     * Returns the comments found while tokenising, in order.
     *
     * @return the comments
     */
    public List<JavaComment> comments() {
        return List.copyOf(comments);
    }

    /**
     * Returns the lexical problems found while tokenising, in order.
     *
     * @return the problems
     */
    public List<ParseProblem> problems() {
        return List.copyOf(problems);
    }

    void addComment(JavaComment comment) {
        comments.add(comment);
    }

    void addProblem(ParseProblem problem) {
        problems.add(problem);
    }

    void requireUntokenized() {
        if (!entries.isEmpty()) {
            throw new IllegalStateException("A source text is tokenised once; create a new one to tokenise again.");
        }
    }
}
