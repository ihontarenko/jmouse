package org.jmouse.java.parser.model;

/**
 * The stable, textual identity of a declaration within its source file's package.
 *
 * <p>The scheme follows the JavaDoc reference syntax, so an id can be pasted into a {@code {@link}}:
 * <table>
 *     <caption>Symbol id scheme</caption>
 *     <tr><th>Declaration</th><th>Id</th></tr>
 *     <tr><td>package</td><td>{@code org.example}</td></tr>
 *     <tr><td>type (nested types joined by a dot)</td><td>{@code org.example.Outer.Inner}</td></tr>
 *     <tr><td>field, enum constant, record component</td><td>{@code org.example.Type#name}</td></tr>
 *     <tr><td>method</td><td>{@code org.example.Type#method(String,int[],T...)}</td></tr>
 *     <tr><td>constructor, compact constructor</td><td>{@code org.example.Type#Type(String)}</td></tr>
 *     <tr><td>initializer, numbered from 1 per kind in source order</td><td>{@code org.example.Type#<initializer>[1]},
 *     {@code org.example.Type#<static-initializer>[1]}</td></tr>
 * </table>
 *
 * <p>Rules that make the id stable:
 * <ul>
 *     <li>Parameter types are written as in the source, never resolved: annotations, {@code final},
 *     type arguments and whitespace are dropped, array brackets and {@code ...} are kept. So
 *     {@code List<String>} and {@code java.util.List<String>} give {@code List} and
 *     {@code java.util.List} — two different ids for what may be one type.</li>
 *     <li>A compact constructor is identified by its record's component types, the canonical
 *     constructor it abbreviates.</li>
 *     <li>A file in the default package has ids without a package prefix.</li>
 *     <li>When malformed or duplicate source produces the same id twice in one file, the later ones get
 *     {@code ~2}, {@code ~3}, … appended, so an id is unique within a file.</li>
 * </ul>
 *
 * @param value the id text
 */
public record SymbolId(String value) {

    /**
     * Validates the text.
     */
    public SymbolId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A symbol id is never blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
