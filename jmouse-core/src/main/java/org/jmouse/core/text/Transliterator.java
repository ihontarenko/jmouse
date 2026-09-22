package org.jmouse.core.text;

/**
 * 🔤 Text of one writing system, rewritten in Latin letters.
 *
 * <p>
 * An implementation owns one table — Cyrillic, Greek, the Latin letters Unicode cannot decompose —
 * and leaves every character it does not own exactly where it is. That is what makes a chain of them
 * simply a sequence of calls: nothing has to be asked which one applies.
 * </p>
 *
 * <h2>⚠️ Forward only, and that is not a limitation to be lifted</h2>
 *
 * <p>
 * Turning {@code volodar} back into «Володар» cannot be done reliably: {@code y} is и, й, ю or я
 * depending on what follows, {@code h} is х or г, {@code i} is і, и or ї. One Latin string has dozens
 * of pre-images, and picking one is guessing. Where two strings in different scripts have to be
 * compared, <strong>both</strong> are pushed into Latin and compared there.
 * </p>
 *
 * <h2>⚠️ A table is not a language</h2>
 *
 * <p>
 * {@link CyrillicTransliterator} carries Ukrainian and Russian together, because telling them apart
 * from an arbitrary string is not possible and the letters that genuinely differ do not differ in a
 * way an address cares about. An implementation that needs the distinction is a different
 * implementation, not a flag on this one.
 * </p>
 */
@FunctionalInterface
public interface Transliterator {

    /**
     * Rewrites what this table owns; everything else passes through unchanged.
     *
     * @param text the text, in any script — never {@code null}
     *
     * @return the same text with this table applied
     */
    String toLatin(String text);
}
