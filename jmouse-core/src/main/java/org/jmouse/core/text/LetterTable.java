package org.jmouse.core.text;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 🔤 A transliteration table applied to a text: each letter it owns rewritten, every other character left as it is.
 *
 * <h2>⚠️ One entry a letter, and the case kept</h2>
 *
 * <p>
 * The table lists a letter once, in lower case; a capital is looked up by its lower case and written with a capital:
 * {@code ж} is {@code zh}, {@code Ж} is {@code Zh}. Whatever folds case downstream — a slug, a comparison key — folds
 * it itself, and a caller that keeps it — an identifier, a name a person reads — gets it.
 * </p>
 *
 * <h2>⚠️ Character by character, never a chain of {@code replace} calls</h2>
 *
 * <p>
 * A sequence of replacements re-reads its own output: {@code ц} becomes {@code ts}, and a later rule for {@code с}
 * would then find the {@code s} this rule just wrote. One pass over the input cannot do that.
 * </p>
 */
final class LetterTable {

    private final Map<String, String> letters;

    LetterTable(Map<String, String> letters) {
        this.letters = new LinkedHashMap<>(letters);
    }

    String toLatin(String text) {
        StringBuilder latin = new StringBuilder(text.length() * 2);
        for (int at = 0; at < text.length(); at++) {
            String single = text.substring(at, at + 1);
            String lower = single.toLowerCase(Locale.ROOT);
            String mapped = letters.get(lower);
            if (mapped == null) {
                latin.append(single);
            } else if (single.equals(lower) || mapped.isEmpty()) {
                latin.append(mapped);
            } else {
                latin.append(mapped.substring(0, 1).toUpperCase(Locale.ROOT)).append(mapped.substring(1));
            }
        }
        return latin.toString();
    }
}
