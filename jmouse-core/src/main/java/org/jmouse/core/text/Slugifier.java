package org.jmouse.core.text;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 🏷️ A name a person typed, turned into something that can be an address.
 *
 * <p>
 * URLs, storage keys, path segments and log lines all want the same shape — lower-case ASCII, words
 * joined by a hyphen, no surprises — and every tree that grows one writes the same four regular
 * expressions. This is that rule, once.
 * </p>
 *
 * <pre>{@code
 * Slugifier addresses = Slugifier.standard();
 *
 * addresses.slug("Тіні забутих предків");   // tini-zabutykh-predkiv
 * addresses.slug("Zoë & Łódź");             // zoe-lodz
 * addresses.slug("日本語", "7");             // 7   ← nothing survived; the caller's fallback stands
 * }</pre>
 *
 * <h2>⚠️ TRANSLITERATION FIRST, and the order is not a preference</h2>
 *
 * <p>
 * Squeezing to {@code [a-z0-9]} before a script table has run throws the script away: «Приватне»
 * becomes empty and falls to a fallback, so a folder ends up addressed as {@code directory} and the
 * next one as {@code directory-2}. A table that runs first answers {@code pryvatne}.
 * </p>
 *
 * <p>
 * ⚠️ And the diacritic strip runs <strong>after</strong> every table, never before. Decomposition
 * splits {@code й} into {@code и} plus a breve, and a single-character lookup then misses the
 * letter entirely.
 * </p>
 *
 * <h2>⚠️ IT DOES NOT MAKE ANYTHING UNIQUE</h2>
 *
 * <p>
 * Whether a slug is free is knowable only where the rows are — a repository for one caller, a
 * parent's children for another. This produces a token; {@link #slug(String, Object)} is for
 * appending something the caller already has, and it exists here only because both known callers
 * would otherwise append <em>past</em> the length limit rather than inside it.
 * </p>
 *
 * <h2>⚠️ A SLUG IS AN ADDRESS, so it is minted once</h2>
 *
 * <p>
 * Nothing here knows that, and nothing here can enforce it — but a caller that re-derives a slug
 * when a name is corrected has broken every link anybody kept. Mint it on creation and store it.
 * </p>
 */
public final class Slugifier {

    /** What separates words. Not configurable: every consumer of a slug assumes it. */
    private static final char SEPARATOR = '-';

    /** ⚠️ No limit. A caller with a column to fit says so; a caller with none must not be truncated. */
    public static final int UNLIMITED = -1;

    private final List<Transliterator> transliterators;
    private final int                  maximumLength;
    private final String               fallback;

    private Slugifier(List<Transliterator> transliterators, int maximumLength, String fallback) {
        this.transliterators = List.copyOf(transliterators);
        this.maximumLength   = maximumLength;
        this.fallback        = fallback;
    }

    /**
     * The tables this tree ships: Cyrillic, then the Latin letters a decomposition cannot take apart.
     *
     * <p>
     * ⚠️ No length limit and an <strong>empty</strong> fallback, so a caller that forgets to say what
     * an unrepresentable name should become gets an empty string rather than a plausible-looking
     * token it never chose.
     * </p>
     */
    public static Slugifier standard() {
        return new Slugifier(List.of(new CyrillicTransliterator(), new ExtendedLatinTransliterator()),
                             UNLIMITED, "");
    }

    /** Caps the result — a column width, a path segment. The distinguisher fits inside it. */
    public Slugifier limitedTo(int maximumLength) {
        return new Slugifier(transliterators, maximumLength, fallback);
    }

    /** What to answer when nothing survives. ⚠️ Say something a person can connect to the name. */
    public Slugifier fallingBackTo(String fallback) {
        return new Slugifier(transliterators, maximumLength, fallback);
    }

    /**
     * Adds a table — a script this tree has none for yet.
     *
     * <p>
     * ⚠️ Appended after the shipped ones and always before the diacritic strip, which this class
     * applies itself. A table added to the end of a chain that had already decomposed its input
     * would be looking for letters that are no longer there.
     * </p>
     */
    public Slugifier transliteratedAlsoBy(Transliterator transliterator) {
        List<Transliterator> extended = new ArrayList<>(transliterators);
        extended.add(transliterator);

        return new Slugifier(extended, maximumLength, fallback);
    }

    /**
     * The same text in Latin letters, with the marks off — before it is squeezed into an address.
     *
     * <p>
     * ⚠️ For a caller that compares rather than addresses: a filename matcher wants «Володар» and
     * {@code Volodar} to meet, and it does its own folding afterwards. Punctuation and spacing are
     * left exactly where they were, which is what separates this from {@link #slug(String)}.
     * </p>
     */
    public String latin(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String working = text;

        for (Transliterator transliterator : transliterators) {
            working = transliterator.toLatin(working);
        }

        // ⚠️ Last, for the reason in the class note: NFD splits `й` and a table would then miss it.
        return Normalizer.normalize(working, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    /**
     * The address for a name.
     *
     * @return the slug, or the configured fallback when nothing of the name survives
     */
    public String slug(String text) {
        String squeezed = latin(text)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", String.valueOf(SEPARATOR))
                .replaceAll("^" + SEPARATOR + "+|" + SEPARATOR + "+$", "");

        if (squeezed.isEmpty()) {
            return fallback;
        }

        return maximumLength == UNLIMITED || squeezed.length() <= maximumLength
               ? squeezed
               : trimmed(squeezed, maximumLength);
    }

    /**
     * The address for a name, made distinct by something the caller already has.
     *
     * <h2>⚠️ The suffix fits INSIDE the limit, which is the whole reason this is here</h2>
     *
     * <p>
     * Appending after a capped slug overflows the column it was capped for, and the failure is a
     * database error about a length rather than anything about a name. The slug is shortened to make
     * room instead.
     * </p>
     *
     * @param distinguisher a number, an identifier — anything unique among the siblings
     */
    public String slug(String text, Object distinguisher) {
        String base   = slug(text);
        String alone  = String.valueOf(distinguisher);
        String suffix = SEPARATOR + alone;

        /*
          ⚠️ NEVER A LEADING SEPARATOR.

          A caller with no fallback configured gets an empty base when nothing of the name survives,
          and appending blindly answers `-1965` — a legal slug, an address that starts with a hyphen,
          and one no caller ever asked for. The distinguisher stands on its own instead, which is also
          the honest answer: it is the only part left.
         */
        if (base.isEmpty()) {
            return alone;
        }

        if (maximumLength == UNLIMITED) {
            return base + suffix;
        }

        int room = maximumLength - suffix.length();

        /*
          ⚠️ A distinguisher longer than the whole limit is the caller's mistake, and answering with a
          truncated identifier would be worse than answering with an overlong string: the truncation
          silently makes two different rows agree.
         */
        if (room <= 0) {
            return alone;
        }

        return trimmed(base, room) + suffix;
    }

    /** ⚠️ Never ends on the separator — a trailing hyphen reads as a name that was cut off. */
    private static String trimmed(String slug, int room) {
        String cut = slug.length() <= room ? slug : slug.substring(0, room);

        return cut.replaceAll(SEPARATOR + "+$", "");
    }
}
