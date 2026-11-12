package org.jmouse.core.text;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🔤 Cyrillic in Latin letters — Ukrainian and Russian, in one table.
 *
 * <h2>⚠️ ONE table on purpose</h2>
 *
 * <p>
 * Telling the two languages apart from an arbitrary string is not possible, and the letters that
 * genuinely differ — {@code и}, {@code г}, {@code е} — do not differ in a way an address or a
 * filename comparison cares about. Two tables would be two chances to disagree, for no gain.
 * </p>
 *
 * <h2>⚠️ Digraphs first</h2>
 *
 * <p>
 * The table is iterated in insertion order and {@code щ} must be consumed before {@code ш}, or the
 * shorter rule eats the first half of the longer one. That is the whole reason this is a
 * {@link LinkedHashMap} and not a {@code Map.of}.
 * </p>
 *
 * <h2>⚠️ The soft and hard signs become NOTHING</h2>
 *
 * <p>
 * They are silent, and no transliteration scheme in ordinary use writes them. Leaving an apostrophe
 * behind would put a character into an address that nothing else ever produces.
 * </p>
 */
public final class CyrillicTransliterator implements Transliterator {

    private static final Map<String, String> TABLE = new LinkedHashMap<>();

    static {
        // Digraphs, longest first.
        TABLE.put("щ", "shch");
        TABLE.put("ш", "sh");
        TABLE.put("ч", "ch");
        TABLE.put("ц", "ts");
        TABLE.put("ж", "zh");
        TABLE.put("х", "kh");
        TABLE.put("ю", "yu");
        TABLE.put("я", "ya");
        TABLE.put("є", "ye");
        TABLE.put("ї", "yi");
        TABLE.put("ё", "yo");

        TABLE.put("а", "a");
        TABLE.put("б", "b");
        TABLE.put("в", "v");
        TABLE.put("г", "h");
        TABLE.put("ґ", "g");
        TABLE.put("д", "d");
        TABLE.put("е", "e");
        TABLE.put("з", "z");
        TABLE.put("и", "y");
        TABLE.put("і", "i");
        TABLE.put("й", "y");
        TABLE.put("к", "k");
        TABLE.put("л", "l");
        TABLE.put("м", "m");
        TABLE.put("н", "n");
        TABLE.put("о", "o");
        TABLE.put("п", "p");
        TABLE.put("р", "r");
        TABLE.put("с", "s");
        TABLE.put("т", "t");
        TABLE.put("у", "u");
        TABLE.put("ф", "f");
        TABLE.put("ы", "y");
        TABLE.put("э", "e");

        TABLE.put("ь", "");
        TABLE.put("ъ", "");
    }

    private static final LetterTable LETTERS = new LetterTable(TABLE);

    /** Each letter of the table, its case kept ({@link LetterTable}). */
    @Override
    public String toLatin(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return LETTERS.toLatin(text);
    }
}
