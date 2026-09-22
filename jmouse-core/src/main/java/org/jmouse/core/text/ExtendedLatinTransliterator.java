package org.jmouse.core.text;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 🔤 The Latin letters a Unicode decomposition cannot take apart.
 *
 * <h2>⚠️ THE GAP IN "STRIP THE ACCENTS", AND IT IS INVISIBLE UNTIL IT BITES</h2>
 *
 * <p>
 * Removing diacritics works by decomposing a letter into a base plus a combining mark and dropping
 * the mark: {@code é} → {@code e} + ́ → {@code e}. Some letters have no such base. {@code ł},
 * {@code ø} and {@code đ} carry the stroke <em>inside</em> the codepoint; {@code ß}, {@code æ} and
 * {@code þ} are letters in their own right. Unicode leaves every one of them exactly as it found
 * them.
 * </p>
 *
 * <p>
 * So a normalisation that only strips marks turns «Łódź» into {@code łodz} — and the very next step,
 * which keeps {@code [a-z0-9]}, throws the {@code ł} away and answers {@code odz}. A Polish name
 * loses its first letter, silently, in the one place that is supposed to make it safe.
 * </p>
 *
 * <p>
 * ⚠️ These are expansions, not approximations: {@code ß} is {@code ss} and {@code æ} is {@code ae}
 * because that is how they are written where the letter is unavailable, which is exactly the
 * situation an address is in.
 * </p>
 */
public final class ExtendedLatinTransliterator implements Transliterator {

    private static final Map<String, String> TABLE = new LinkedHashMap<>();

    static {
        TABLE.put("ß", "ss");
        TABLE.put("æ", "ae");
        TABLE.put("œ", "oe");
        TABLE.put("ł", "l");
        TABLE.put("ø", "o");
        TABLE.put("đ", "d");
        TABLE.put("ð", "d");
        TABLE.put("þ", "th");
        TABLE.put("ħ", "h");
        TABLE.put("ŧ", "t");
        TABLE.put("ı", "i");
    }

    /** ⚠️ Lower-cased first, for the same reason {@link CyrillicTransliterator} does it. */
    @Override
    public String toLatin(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String        lowered = text.toLowerCase(Locale.ROOT);
        StringBuilder latin   = new StringBuilder(lowered.length() + 4);

        for (int at = 0; at < lowered.length(); at++) {
            String single = lowered.substring(at, at + 1);
            String mapped = TABLE.get(single);

            latin.append(mapped != null ? mapped : single);
        }

        return latin.toString();
    }
}
