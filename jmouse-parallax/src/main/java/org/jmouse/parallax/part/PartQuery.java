package org.jmouse.parallax.part;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * What a reader typed, as words every one of which a part must contain somewhere — its name, its keywords, its
 * description — the way the Library's own search reads a query. A part matched in its name ranks above one matched
 * only in its description.
 */
public final class PartQuery {

    /** Where a word was found, the strongest first. */
    private static final int IN_NAME = 4;
    private static final int IN_KEYWORDS = 2;
    private static final int IN_DESCRIPTION = 1;
    private static final int WHOLE_NAME = 8;

    private final String text;
    private final List<String> words;

    private PartQuery(String text, List<String> words) {
        this.text = text;
        this.words = words;
    }

    public static PartQuery of(String typed) {
        String text = typed == null ? "" : typed.strip().toLowerCase(Locale.ROOT);
        return new PartQuery(text, Arrays.stream(text.split("\\s+")).filter(word -> !word.isEmpty()).toList());
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }

    /**
     * How well a part matches, or 0 when some word is in none of its texts.
     *
     * @param name        its name, as {@code Device:R} or {@code NE555}
     * @param keywords    its keywords and tags, space separated
     * @param description what it says of itself
     */
    public int score(String name, String keywords, String description) {
        String loweredName = lowered(name);
        String loweredKeywords = lowered(keywords);
        String loweredDescription = lowered(description);
        int score = 0;
        for (String word : words) {
            int found = loweredName.contains(word) ? IN_NAME
                    : loweredKeywords.contains(word) ? IN_KEYWORDS
                    : loweredDescription.contains(word) ? IN_DESCRIPTION : 0;
            if (found == 0) {
                return 0;
            }
            score += found;
        }
        return loweredName.equals(text) || loweredName.endsWith(":" + text) ? score + WHOLE_NAME : score;
    }

    private static String lowered(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
