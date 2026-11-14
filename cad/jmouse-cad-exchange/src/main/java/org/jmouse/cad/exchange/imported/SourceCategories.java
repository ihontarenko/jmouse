package org.jmouse.cad.exchange.imported;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Where a part another product brings is filed among our categories — by what it is, never by whose it is: the product's own word
 * for its kind — a KiCad library's name ({@code Resistor_SMD}), a LibrePCB category's UUID — matched against the filing table's
 * entries ({@code resistors-surface "…" { sources "Resistor_SMD" }}), a {@code *} standing for any run of characters but a
 * {@code :} — a library's pattern ({@code Resistor_*}) never takes a part's {@code LIBRARY:NAME} ({@code Resistor_SMD:R_0805}). The words are
 * tried most specific first, each against the entries in the table's order; the first match files it. What matches nothing is
 * filed in {@link #unfiled}. Whose it is stays in its provenance.
 *
 * @param bySource our category for each of the product's words, in the table's order
 * @param unfiled  where a part whose words match nothing is filed
 */
public record SourceCategories(Map<String, String> bySource, String unfiled) {

    /** The field of a filing table's entry naming the product's words filed in that entry's category. */
    public static final String SOURCES = "sources";

    /** The field naming where a part matching nothing is filed. */
    public static final String UNFILED = "unfiled-category";

    public SourceCategories {
        // The table's order decides which entry files a word: kept as written.
        bySource = Collections.unmodifiableMap(new LinkedHashMap<>(bySource));
    }

    public static SourceCategories of(FilingTable table) {
        return new SourceCategories(table.ours(SOURCES), table.text(UNFILED));
    }

    /**
     * The category of a part the product calls by these words, the most specific first — a category, then its parent.
     *
     * @param words the product's words for what the part is; none files it as unfiled
     */
    public String categoryOf(List<String> words) {
        for (String word : words) {
            for (Map.Entry<String, String> rule : bySource.entrySet()) {
                if (matches(rule.getKey(), word)) {
                    return rule.getValue();
                }
            }
        }
        return unfiled;
    }

    /** Whether a word is the one written, a {@code *} in it standing for any run of characters but a {@code :}; case does not decide. */
    static boolean matches(String written, String word) {
        if (!written.contains("*")) {
            return written.equalsIgnoreCase(word);
        }
        String expression = Pattern.quote(written).replace("*", "\\E[^:]*\\Q");
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(word).matches();
    }
}
