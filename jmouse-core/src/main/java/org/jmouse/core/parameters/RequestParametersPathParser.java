package org.jmouse.core.parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧭 A parameter's name read into the path it stands at: a property after a dot ({@code papers.width}), an index in brackets
 * ({@code papers[0]}), an append as empty brackets ({@code groups[]}), and any other word in brackets a property
 * ({@code filter[admin]}, {@code looks[pcb.look]}). A bracket that is never closed is read as a letter of the name.
 * {@link RequestParametersPathWriter} writes a path back in this grammar.
 */
public final class RequestParametersPathParser {

    /** The most digits an index is written with: nine always fit an {@code int}. */
    private static final int MOST_INDEX_DIGITS = 9;

    private static final RequestParametersPathWriter PATH_WRITER = new RequestParametersPathWriter();

    /**
     * A name's path, held to a spelling: {@link RequestParametersSpelling#ONE} takes only the way {@link RequestParametersPathWriter}
     * writes it, and only a place.
     *
     * @throws IllegalArgumentException where the spelling is {@code ONE} and the name is written another way, or names no place
     */
    public List<RequestParametersPathToken> parse(String rawParameterName, RequestParametersSpelling spelling) {
        List<RequestParametersPathToken> tokens = parse(rawParameterName);
        if (spelling == RequestParametersSpelling.ANY) {
            return tokens;
        }
        if (tokens.stream().anyMatch(RequestParametersPathToken.ArrayAppendToken.class::isInstance)) {
            throw new IllegalArgumentException("'" + rawParameterName + "' names no place: a list's item is named by its index.");
        }
        String written = PATH_WRITER.write(tokens);
        if (!written.equals(rawParameterName)) {
            throw new IllegalArgumentException("'" + rawParameterName + "' is written '" + written + "'.");
        }
        return tokens;
    }

    /** A name's path, in any spelling the grammar reads; none for a blank name. */
    public List<RequestParametersPathToken> parse(String rawParameterName) {
        if (rawParameterName == null || rawParameterName.isBlank()) {
            return List.of();
        }

        List<RequestParametersPathToken> tokens = new ArrayList<>();
        StringBuilder                    buffer = new StringBuilder();
        int                              index  = 0;

        while (index < rawParameterName.length()) {
            char character = rawParameterName.charAt(index);

            if (character == '.') {
                flushPropertyName(buffer, tokens);
                index++;
                continue;
            }

            if (character == '[') {
                flushPropertyName(buffer, tokens);

                int closingBracketIndex = rawParameterName.indexOf(']', index);
                if (closingBracketIndex < 0) {
                    // A bracket never closed is a letter of the name.
                    buffer.append(character);
                    index++;
                    continue;
                }

                String inside = rawParameterName.substring(index + 1, closingBracketIndex);

                if (inside.isEmpty()) {
                    tokens.add(RequestParametersPathToken.ArrayAppendToken.instance());
                } else if (isIndex(inside)) {
                    tokens.add(new RequestParametersPathToken.ArrayIndexToken(Integer.parseInt(inside)));
                } else {
                    tokens.add(new RequestParametersPathToken.PropertyNameToken(inside));
                }

                index = closingBracketIndex + 1;
                continue;
            }

            buffer.append(character);
            index++;
        }

        flushPropertyName(buffer, tokens);
        return tokens;
    }

    private static void flushPropertyName(StringBuilder buffer, List<RequestParametersPathToken> tokens) {
        if (buffer.isEmpty()) {
            return;
        }

        String name = buffer.toString();
        buffer.setLength(0);

        if (!name.isBlank()) {
            tokens.add(new RequestParametersPathToken.PropertyNameToken(name));
        }
    }

    /** Digits that make an index: no more of them than an index can hold, so a long run of digits is a name, never a wrong place. */
    private static boolean isIndex(String text) {
        return text.length() <= MOST_INDEX_DIGITS && isAllDigits(text);
    }

    private static boolean isAllDigits(String text) {
        return !text.isEmpty() && text.chars().allMatch(character -> character >= '0' && character <= '9');
    }
}
