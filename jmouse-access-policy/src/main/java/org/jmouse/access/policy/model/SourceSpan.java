package org.jmouse.access.policy.model;

/**
 * Where something was written: which file, which line, which column. Line and column are 1-based.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * 1 | role SPACE_ADMIN {
 * 2 |     {@literal @}SPACE  space:write
 *   |     ^ column 5
 * </pre>
 *
 * <p>The bundle entry on line 2 carries {@code new SourceSpan(2, 5)}. Point at the construct, not at
 * the token that failed — binding reports "no scope called SPCE is registered", and the reader needs
 * to find the line, not the character.
 *
 * <p>On every node, and not optional. A policy file whose failure message is "invalid policy" is a
 * policy file people stop editing.
 *
 * <h2>⚠️ Which file, and why it is not the parser's to say</h2>
 *
 * <p>A parser reads one text and has no opinion about where it came from — the same characters are a
 * file on the classpath, a row in a table, or something somebody pasted into the control room. So
 * {@link #document} is null as parsed and filled in afterwards, by whoever knows: the loader, when it
 * merges several files into one document.
 *
 * <p>It has to be filled in, because merging is where the answer would otherwise be lost. Before the
 * merge each document knows its own name; after it every declaration belongs to one document with one
 * name, and *"which file grants this"* — the question the control room exists to answer — has no
 * answer left. Carrying it here rather than on every record keeps it beside the line number it
 * belongs with.
 *
 * @param document which file, or null while nothing has said
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

    /** Whether anybody has said which file this came from. */
    public boolean namesADocument() {
        return document != null && !document.isBlank();
    }

    @Override
    public String toString() {
        return namesADocument() ? document + " " + line + ":" + column : line + ":" + column;
    }
}
