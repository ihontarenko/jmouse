package org.jmouse.datamap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Where in the original document a value was read: {@code source page 7 table 6.3}. A citation is pairs of a word and
 * what it names, in the order written; which words a citation uses is the author's, not this language's.
 */
public record DataMapSource(Map<String, String> citation) {

    public DataMapSource {
        citation = Collections.unmodifiableMap(new LinkedHashMap<>(citation));
    }

    /** What the citation names by that word: {@code table} gives {@code 6.3}. */
    public Optional<String> part(String word) {
        return Optional.ofNullable(citation.get(word));
    }

    /** The page, when the citation names one as a number. */
    public Optional<Integer> page() {
        return part("page").filter(page -> page.chars().allMatch(Character::isDigit)).map(Integer::valueOf);
    }

    /** The citation as jMD writes it, the word {@code source} included. */
    public String written() {
        StringBuilder written = new StringBuilder("source");
        citation.forEach((word, named) -> written.append(' ').append(word).append(' ')
                .append(bare(named) ? named : DataMapValue.quoted(named)));
        return written.toString();
    }

    private static boolean bare(String named) {
        return !named.isEmpty() && named.chars().noneMatch(character -> Character.isWhitespace(character)
                || character == '"' || character == '{' || character == '}' || character == ';' || character == '#');
    }
}
