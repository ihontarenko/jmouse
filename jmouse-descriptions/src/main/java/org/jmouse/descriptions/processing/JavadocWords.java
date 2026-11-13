package org.jmouse.descriptions.processing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Javadoc comment as words a reader is shown: its main description, and each {@code @param}'s. An inline tag becomes its text
 * — a code or literal tag what it holds, a link its label, else the name it links to —, HTML markup goes, an entity becomes its
 * character, and white space folds to one space.
 */
final class JavadocWords {

    private static final Pattern BLOCK_TAG = Pattern.compile("^@(\\w+)\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern PARAMETER = Pattern.compile("^(\\S+)\\s*(.*)$", Pattern.DOTALL);
    /** Markup that breaks a line or a block: a space where it stood. */
    private static final Pattern BLOCK_MARKUP = Pattern.compile("</?(p|br|li|ul|ol|dl|dt|dd|div|pre|blockquote|table|tr|td|th|h[1-6])\\b[^>]*>",
            Pattern.CASE_INSENSITIVE);
    /** Any other markup: nothing where it stood. */
    private static final Pattern MARKUP = Pattern.compile("</?[A-Za-z][^>]*>");
    private static final Map<String, String> ENTITIES = Map.of("&lt;", "<", "&gt;", ">", "&quot;", "\"", "&#39;", "'", "&nbsp;", " ",
            "&amp;", "&");

    /** What stands for a code tag's angle brackets while the markup goes: they are its words, not markup. */
    private static final char OPENING = '';
    private static final char CLOSING = '';

    private final String summary;
    private final Map<String, String> parameters;

    private JavadocWords(String summary, Map<String, String> parameters) {
        this.summary = summary;
        this.parameters = parameters;
    }

    /** A comment as {@code Elements.getDocComment} gives it; an absent one says nothing. */
    static JavadocWords of(String comment) {
        if (comment == null) {
            return new JavadocWords("", Map.of());
        }
        StringBuilder main = new StringBuilder();
        Map<String, String> parameters = new LinkedHashMap<>();
        StringBuilder tag = null;
        for (String line : comment.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("@") && BLOCK_TAG.matcher(trimmed).matches()) {
                addParameter(tag, parameters);
                tag = new StringBuilder(trimmed);
                continue;
            }
            (tag == null ? main : tag).append('\n').append(line);
        }
        addParameter(tag, parameters);
        return new JavadocWords(words(main.toString()), parameters);
    }

    /** What the type or member is; blank for nothing. */
    String summary() {
        return summary;
    }

    /** Each {@code @param}'s words, by the name it describes. */
    Map<String, String> parameters() {
        return parameters;
    }

    private static void addParameter(StringBuilder tag, Map<String, String> parameters) {
        if (tag == null) {
            return;
        }
        Matcher block = BLOCK_TAG.matcher(tag.toString());
        if (!block.matches() || !block.group(1).equals("param")) {
            return;
        }
        Matcher parameter = PARAMETER.matcher(block.group(2));
        if (parameter.matches()) {
            parameters.put(parameter.group(1), words(parameter.group(2)));
        }
    }

    /** Javadoc text as plain words. */
    static String words(String text) {
        String plain = MARKUP.matcher(BLOCK_MARKUP.matcher(inlineTags(text)).replaceAll(" ")).replaceAll("");
        for (Map.Entry<String, String> entity : ENTITIES.entrySet()) {
            plain = plain.replace(entity.getKey(), entity.getValue());
        }
        return plain.replace(OPENING, '<').replace(CLOSING, '>').replaceAll("\\s+", " ").strip();
    }

    /** Every inline tag replaced by its text, braces inside it counted, so {@code {@code Map<K, V>}} keeps its own. */
    private static String inlineTags(String text) {
        StringBuilder written = new StringBuilder();
        int at = 0;
        while (at < text.length()) {
            int start = text.indexOf("{@", at);
            if (start < 0) {
                written.append(text, at, text.length());
                break;
            }
            written.append(text, at, start);
            int depth = 0;
            int end = start;
            for (; end < text.length(); end++) {
                char character = text.charAt(end);
                if (character == '{') {
                    depth++;
                } else if (character == '}' && --depth == 0) {
                    break;
                }
            }
            if (end >= text.length()) {
                written.append(text, start, text.length());
                break;
            }
            written.append(tagText(text.substring(start + 2, end)));
            at = end + 1;
        }
        return written.toString();
    }

    /** One inline tag's text, its name and what it holds given. */
    private static String tagText(String tag) {
        String[] parts = tag.strip().split("\\s+", 2);
        String name = parts[0];
        String held = parts.length > 1 ? parts[1].strip() : "";
        return switch (name) {
            case "code", "literal" -> held.replace('<', OPENING).replace('>', CLOSING);
            case "link", "linkplain", "value" -> linkText(held);
            default -> "";
        };
    }

    /** A link's label, else the last name it reaches: {@code Type#member(int)} is {@code member}, {@code a.b.Type} is {@code Type}. */
    private static String linkText(String held) {
        String[] parts = held.split("\\s+", 2);
        if (parts.length > 1 && !parts[1].isBlank()) {
            return parts[1].strip();
        }
        String reference = parts[0];
        int parenthesis = reference.indexOf('(');
        if (parenthesis >= 0) {
            reference = reference.substring(0, parenthesis);
        }
        int member = reference.lastIndexOf('#');
        if (member >= 0) {
            return reference.substring(member + 1);
        }
        return reference.substring(reference.lastIndexOf('.') + 1);
    }
}
