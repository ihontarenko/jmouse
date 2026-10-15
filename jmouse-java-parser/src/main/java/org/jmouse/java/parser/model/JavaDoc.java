package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Optional;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A documentation comment, structured, and the declaration it documents.
 *
 * <p>Nothing here is resolved: {@code {@inheritDoc}} stays an {@link InlineTag} marked
 * {@link ReferenceState#UNRESOLVED} and no inherited text is copied in; link targets are kept as written.
 *
 * @param span        the comment itself, from {@code /**} to the closing delimiter
 * @param rawText     the comment exactly as written, delimiters included
 * @param description the main description — the text before the first block tag, with line prefixes
 *                    removed and inline tags left as written; empty when there is none
 * @param blockTags   block tags in order
 * @param inlineTags  every inline tag in the comment, in the description and in block tags, in order
 * @param owner       the declaration the comment documents, or {@code null} for a comment that precedes
 *                    no declaration (a stray comment inside a body is not reported at all — bodies are opaque)
 */
public record JavaDoc(SourceSpan span, String rawText, String description, List<BlockTag> blockTags,
                      List<InlineTag> inlineTags, JavaDocOwner owner) {

    /**
     * Copies the lists.
     */
    public JavaDoc {
        if (span == null || rawText == null || description == null) {
            throw new IllegalArgumentException("A JavaDoc needs a span, its raw text and a description.");
        }
        blockTags = List.copyOf(blockTags);
        inlineTags = List.copyOf(inlineTags);
    }

    /**
     * Returns this comment attached to an owner.
     *
     * @param newOwner the documented declaration
     * @return a copy with that owner
     */
    public JavaDoc withOwner(JavaDocOwner newOwner) {
        return new JavaDoc(span, rawText, description, blockTags, inlineTags, newOwner);
    }

    /**
     * Returns the block tags of one kind.
     *
     * @param kind the kind
     * @return matching tags in order
     */
    public List<BlockTag> blockTags(BlockTag.Kind kind) {
        return blockTags.stream().filter(tag -> tag.kind() == kind).toList();
    }

    /**
     * Finds the {@code @param} tag for a parameter.
     *
     * @param parameterName the name, or {@code <T>} for a type parameter
     * @return the tag, if documented
     */
    public Optional<BlockTag> parameter(String parameterName) {
        return blockTags.stream()
                .filter(tag -> tag.kind() == BlockTag.Kind.PARAM && parameterName.equals(tag.argument()))
                .findFirst();
    }

    /**
     * Returns whether any part of this comment defers to the supertype's documentation.
     *
     * @return {@code true} when it contains {@code {@inheritDoc}}
     */
    public boolean inheritsDocumentation() {
        return inlineTags.stream().anyMatch(tag -> tag.kind() == InlineTag.Kind.INHERIT_DOC);
    }
}
