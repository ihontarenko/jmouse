package org.jmouse.script.el;

/**
 * Where something was written: which file, which line, which column. Line and column are 1-based.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * 8 | on unload when building.kind == 'dropoff' do
 *   | ^ column 5
 * </pre>
 *
 * <p>The handler on line 8 carries {@code new SourceSpan(8, 5)}. Point at the construction, not at the
 * token that failed — binding reports <em>"no event called {@code unlod} is declared"</em>, and the
 * reader has to find the line, not the character.</p>
 *
 * <p>On every node, and not optional. A script whose failure message is "invalid script" is a script
 * people stop writing.</p>
 *
 * <h2>⚠️ Which file, and why it is not the parser's to say</h2>
 *
 * <p>A parser reads one text and has no opinion about where it came from — the same characters are a
 * file on disk, a row in a table, or something a level editor produced in memory. So {@link #document}
 * is null as parsed and filled in afterwards, by whoever knows: the loader, when it follows an
 * {@code include} and merges what it found.</p>
 *
 * <p>It has to be filled in, because the merge is where the answer would otherwise be lost. Before it
 * each document knows its own name; after it every handler belongs to one document with one name, and
 * <em>"which file declared this"</em> — the first question anybody debugging a script asks — has no
 * answer left.</p>
 *
 * @param document which file, or {@code null} while nothing has said
 * @param line     the 1-based line
 * @param column   the 1-based column within that line
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record SourceSpan(String document, int line, int column) {

    /** A position in a text that has not been attributed to a file yet — what a parser produces. */
    public SourceSpan(int line, int column) {
        this(null, line, column);
    }

    /** For a document assembled in code rather than read from a file. */
    public static SourceSpan none() {
        return new SourceSpan(null, 0, 0);
    }

    /**
     * The same position, attributed to a file.
     *
     * @param document what the file is called
     * @return this position, in that file
     */
    public SourceSpan in(String document) {
        return new SourceSpan(document, line, column);
    }

    /**
     * Whether this points at a line somebody can open.
     *
     * <p>Not every node carries a span — the expression language's own nodes do not — so a caller
     * reporting a failure inside one falls back to the position of the construction that holds it.
     * A line in the right handler beats no line at all, and beats {@code 0:0} by a mile.</p>
     *
     * @return {@code true} when a line number was recorded
     */
    public boolean isKnown() {
        return line > 0;
    }

    /**
     * Whether anybody has said which file this came from.
     *
     * @return {@code true} when the position names a document
     */
    public boolean namesADocument() {
        return document != null && !document.isBlank();
    }

    @Override
    public String toString() {
        return namesADocument() ? document + " " + line + ":" + column : line + ":" + column;
    }
}
