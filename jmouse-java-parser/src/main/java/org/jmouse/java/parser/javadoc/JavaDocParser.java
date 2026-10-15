package org.jmouse.java.parser.javadoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jmouse.java.parser.model.BlockTag;
import org.jmouse.java.parser.model.InlineTag;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.JavaDocOwner;
import org.jmouse.java.parser.model.ReferenceState;
import org.jmouse.java.parser.source.LineIndex;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * Structures one documentation comment into a description, block tags and inline tags.
 *
 * <p>The comment is first reduced to its content: the delimiters go, and on every line the leading
 * whitespace, the run of asterisks and one following space are removed. Everything is then read from
 * that content, while each tag's span is mapped back to exact offsets in the original.
 *
 * <p>A block tag is an {@code @} that starts a content line outside any inline tag. An inline tag is
 * {@code {@name …}} with nested braces balanced, so {@code {@code Map<K, V> map = {}}} closes where it
 * should. Nothing is resolved and no text is invented: {@code {@inheritDoc}} and every reference stay
 * {@link ReferenceState#UNRESOLVED}.
 *
 * <p>Instances are stateless and safe to share.
 */
public final class JavaDocParser {

    /**
     * Parses a documentation comment.
     *
     * @param rawText     the comment exactly as written, from {@code /**} to its closing delimiter
     * @param startOffset the comment's offset in the original text
     * @param lines       the original text's line index
     * @param owner       the documented declaration, or {@code null}
     * @return the structured comment
     */
    public JavaDoc parse(String rawText, int startOffset, LineIndex lines, JavaDocOwner owner) {
        Content content = Content.of(rawText, startOffset);
        String  text    = content.text();

        List<Integer> blockStarts      = blockTagStarts(text);
        int           descriptionEnd   = blockStarts.isEmpty() ? text.length() : blockStarts.getFirst();
        String        description      = text.substring(0, descriptionEnd).strip();
        List<InlineTag> allInlineTags  = new ArrayList<>(inlineTags(content, 0, descriptionEnd, lines));
        List<BlockTag>  blockTags      = new ArrayList<>();

        for (int index = 0; index < blockStarts.size(); index++) {
            int start = blockStarts.get(index);
            int end   = index + 1 < blockStarts.size() ? blockStarts.get(index + 1) : text.length();
            BlockTag tag = blockTag(content, start, trimmedEnd(text, start, end), lines);
            blockTags.add(tag);
            allInlineTags.addAll(tag.inlineTags());
        }

        return new JavaDoc(lines.span(startOffset, startOffset + rawText.length()), rawText, description, blockTags,
                allInlineTags, owner);
    }

    private static List<Integer> blockTagStarts(String text) {
        List<Integer> starts      = new ArrayList<>();
        int           inlineDepth = 0;
        boolean       lineStart   = true;

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                lineStart = true;
                continue;
            }
            if (lineStart) {
                if (Character.isWhitespace(character)) {
                    continue;
                }
                lineStart = false;
                if (character == '@' && inlineDepth == 0 && index + 1 < text.length() && Character.isLetter(text.charAt(index + 1))) {
                    starts.add(index);
                }
            }
            if (character == '{' && (inlineDepth > 0 || index + 1 < text.length() && text.charAt(index + 1) == '@')) {
                inlineDepth++;
            } else if (character == '}' && inlineDepth > 0) {
                inlineDepth--;
            }
        }

        return starts;
    }

    private static BlockTag blockTag(Content content, int start, int end, LineIndex lines) {
        String text    = content.text();
        int    nameEnd = wordEnd(text, start + 1, end);
        String name    = text.substring(start + 1, nameEnd);
        BlockTag.Kind kind = BlockTag.Kind.ofName(name);

        int            restStart = skipWhitespace(text, nameEnd, end);
        String         argument  = null;
        ReferenceState state     = ReferenceState.NOT_A_REFERENCE;
        int            textStart = restStart;

        switch (kind) {
            case PARAM, THROWS -> {
                if (restStart < end) {
                    int argumentEnd = referenceEnd(text, restStart, end);
                    argument = text.substring(restStart, argumentEnd);
                    textStart = argumentEnd;
                    if (kind == BlockTag.Kind.THROWS) {
                        state = ReferenceState.UNRESOLVED;
                    }
                }
            }
            case SEE -> {
                if (restStart < end && text.charAt(restStart) != '"' && text.charAt(restStart) != '<') {
                    int argumentEnd = referenceEnd(text, restStart, end);
                    argument = text.substring(restStart, argumentEnd);
                    textStart = argumentEnd;
                    state = ReferenceState.UNRESOLVED;
                }
            }
            default -> {
            }
        }

        String description = text.substring(Math.min(textStart, end), end).strip();
        return new BlockTag(kind, name, argument, description, state, inlineTags(content, start, end, lines),
                content.span(lines, start, end));
    }

    private static List<InlineTag> inlineTags(Content content, int from, int to, LineIndex lines) {
        String          text = content.text();
        List<InlineTag> tags = new ArrayList<>();
        int             index = from;

        while (index < to) {
            if (text.charAt(index) != '{' || index + 1 >= to || text.charAt(index + 1) != '@') {
                index++;
                continue;
            }
            int nameEnd = wordEnd(text, index + 2, to);
            int depth   = 1;
            int cursor  = nameEnd;
            while (cursor < to && depth > 0) {
                char character = text.charAt(cursor);
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    depth--;
                }
                cursor++;
            }
            int    contentEnd = depth == 0 ? cursor - 1 : cursor;
            String name       = text.substring(index + 2, nameEnd);
            String body       = text.substring(nameEnd, contentEnd).strip();
            tags.add(inlineTag(name, body, content.span(lines, index, cursor)));
            index = cursor;
        }

        return tags;
    }

    private static InlineTag inlineTag(String name, String body, SourceSpan span) {
        InlineTag.Kind kind = InlineTag.Kind.ofName(name);
        return switch (kind) {
            case LINK, LINKPLAIN, VALUE -> {
                String reference = null;
                String label     = null;
                if (!body.isEmpty()) {
                    int referenceEnd = referenceEnd(body, 0, body.length());
                    reference = body.substring(0, referenceEnd);
                    String rest = body.substring(referenceEnd).strip();
                    label = rest.isEmpty() || kind == InlineTag.Kind.VALUE ? null : rest;
                }
                yield new InlineTag(kind, name, body, reference, label, ReferenceState.UNRESOLVED, span);
            }
            case INHERIT_DOC -> new InlineTag(kind, name, body, null, null, ReferenceState.UNRESOLVED, span);
            default -> new InlineTag(kind, name, body, null, null, ReferenceState.NOT_A_REFERENCE, span);
        };
    }

    /** The end of a program reference: the first whitespace outside parentheses, so {@code #m(String, int)} stays whole. */
    private static int referenceEnd(String text, int from, int to) {
        int depth = 0;
        int index = from;
        while (index < to) {
            char character = text.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')' && depth > 0) {
                depth--;
            } else if (depth == 0 && Character.isWhitespace(character)) {
                break;
            }
            index++;
        }
        return index;
    }

    private static int wordEnd(String text, int from, int to) {
        int index = from;
        while (index < to && (Character.isLetterOrDigit(text.charAt(index)) || text.charAt(index) == '-' || text.charAt(index) == '.')) {
            index++;
        }
        return index;
    }

    private static int skipWhitespace(String text, int from, int to) {
        int index = from;
        while (index < to && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int trimmedEnd(String text, int start, int end) {
        int index = end;
        while (index > start && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    /**
     * A comment's content with every line prefix removed, and where each content character came from.
     */
    private record Content(String text, int[] originalOffsets) {

        static Content of(String rawText, int startOffset) {
            int contentStart = Math.min(3, rawText.length());
            int contentEnd   = rawText.length() >= 5 && rawText.endsWith("*/") ? rawText.length() - 2 : rawText.length();

            StringBuilder text    = new StringBuilder(contentEnd - contentStart);
            int[]         offsets = new int[Math.max(0, contentEnd - contentStart)];
            int           count   = 0;
            int           position = contentStart;
            boolean       firstLine = true;

            while (position < contentEnd) {
                int lineEnd = position;
                while (lineEnd < contentEnd && rawText.charAt(lineEnd) != '\n' && rawText.charAt(lineEnd) != '\r') {
                    lineEnd++;
                }
                int segmentStart = stripLinePrefix(rawText, position, lineEnd, firstLine);
                for (int index = segmentStart; index < lineEnd; index++) {
                    text.append(rawText.charAt(index));
                    offsets[count++] = startOffset + index;
                }
                if (lineEnd < contentEnd) {
                    text.append('\n');
                    offsets[count++] = startOffset + lineEnd;
                    boolean carriageReturnLineFeed = rawText.charAt(lineEnd) == '\r' && lineEnd + 1 < contentEnd && rawText.charAt(lineEnd + 1) == '\n';
                    position = lineEnd + (carriageReturnLineFeed ? 2 : 1);
                } else {
                    position = lineEnd;
                }
                firstLine = false;
            }

            return new Content(text.toString(), Arrays.copyOf(offsets, count));
        }

        private static int stripLinePrefix(String rawText, int start, int end, boolean firstLine) {
            int index = start;
            if (!firstLine) {
                while (index < end && (rawText.charAt(index) == ' ' || rawText.charAt(index) == '\t' || rawText.charAt(index) == '\f')) {
                    index++;
                }
            }
            if (index < end && rawText.charAt(index) == '*') {
                while (index < end && rawText.charAt(index) == '*') {
                    index++;
                }
            } else if (firstLine) {
                return start;
            } else {
                return index;
            }
            if (index < end && rawText.charAt(index) == ' ') {
                index++;
            }
            return index;
        }

        /** The original span of content characters {@code [from, to)}; both must lie on content characters. */
        SourceSpan span(LineIndex lines, int from, int to) {
            int start = originalOffsets[from];
            int end   = to > from ? originalOffsets[to - 1] + 1 : start;
            return lines.span(start, end);
        }
    }
}
