package org.jmouse.search.text;

import org.jmouse.search.SearchTerms;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * ✂️ The passage a search matched, taken out of the document around it.
 *
 * <h3>⚠️ Why a result carries this and not the thing's summary</h3>
 *
 * <p>A summary is the opening lines of a document and says what it is about; it says nothing about why
 * it is in front of you. Twenty-five results, each described by its own first sentence, is a list a
 * reader cannot choose from — and a model, given that list, opens all of them. A hit that shows the
 * sentence carrying the words is one somebody can accept or discard without reading the document.</p>
 *
 * <h3>⚠️ No highlight markers in the string</h3>
 *
 * <p>It is tempting to wrap the match in something a screen can style. It is also wrong: the same string
 * is read by a person, rendered on a page and handed to a model through a tool, and only the first of
 * those wants markup. Every caller already knows the terms — marking them is a two-line job where the
 * marking is wanted, and a plain passage everywhere else.</p>
 *
 * <h3>⚠️ It works on the window, not on the document</h3>
 *
 * <p>Cleaning a whole document to find a hundred characters of it is work proportional to the document
 * rather than to the answer, on every row of every search. So the position is found in the raw text and
 * only the slice around it is tidied.</p>
 */
public final class Snippets {

    /** How much of the document to keep on either side of the match. */
    private static final int BEFORE = 80;
    private static final int AFTER  = 140;

    /**
     * ⚠️ Three, not one per term. One passage is often not enough to tell two documents apart, and eight
     * would be the document — the point is a glance, not a read.
     */
    private static final int MOST_PASSAGES = 3;

    /** Two matches closer than this are one passage, so a repeated word does not repeat the snippet. */
    private static final int SAME_PASSAGE_WITHIN = BEFORE + AFTER;

    private Snippets() {
    }

    /** Up to three passages carrying the terms, with Markdown decoration taken off. */
    public static List<String> from(String markdown, SearchTerms terms) {
        return from(markdown, terms, Snippets::withoutMarkdown);
    }

    /**
     * The same, over text that is not Markdown.
     *
     * <h3>⚠️ The cleaner is a parameter because the punctuation is not always noise</h3>
     *
     * <p>{@link #withoutMarkdown} drops the characters that carry no words <em>in a document</em> —
     * {@code # > * _ ` | ~}. In a document they are decoration. Elsewhere they are the value: a composite
     * measurement is stored as {@code 100|nF}, and a cleaner that removes the bar hands back a passage
     * that no longer says what was found. Whoever knows what the text is says how to tidy it, and
     * {@link #asItIs} is there for whoever knows it needs nothing.
     */
    public static List<String> from(String text, SearchTerms terms, UnaryOperator<String> clean) {
        if (text == null || text.isBlank() || terms.empty()) {
            return List.of();
        }

        String        folded    = text.toLowerCase(Locale.ROOT);
        List<Integer> positions = new ArrayList<>();

        // ⚠️ Term by term rather than by first occurrence overall: a document matching three terms
        // should show all three, and the first two hundred characters may carry only one of them.
        for (String term : terms.terms()) {
            int at = folded.indexOf(term);

            if (at >= 0 && isNewPassage(positions, at)) {
                positions.add(at);
            }

            if (positions.size() == MOST_PASSAGES) {
                break;
            }
        }

        return positions.stream()
                .sorted()
                .map(position -> passageAround(text, position, clean))
                .filter(passage -> !passage.isBlank())
                .toList();
    }

    private static boolean isNewPassage(List<Integer> taken, int position) {
        return taken.stream().noneMatch(other -> Math.abs(other - position) < SAME_PASSAGE_WITHIN);
    }

    /**
     * The slice around one match, cut at word boundaries and elided where it was cut.
     *
     * <p>The ellipses are not decoration: without them a passage starting mid-sentence reads as a
     * sentence somebody wrote badly rather than as an extract.
     */
    private static String passageAround(String text, int position, UnaryOperator<String> clean) {
        int start = wordBoundaryAfter(text, Math.max(0, position - BEFORE));
        int end   = wordBoundaryBefore(text, Math.min(text.length(), position + AFTER));

        if (end <= start) {
            return "";
        }

        // ⚠️ Whitespace is collapsed here rather than by the cleaner, because it is the one tidy-up true
        // of every kind of text — a passage cut out of a document must read as one line whatever the
        // document was.
        String passage = clean.apply(text.substring(start, end)).replaceAll("\\s+", " ").trim();

        if (passage.isBlank()) {
            return "";
        }

        return (start > 0 ? "…" : "") + passage + (end < text.length() ? "…" : "");
    }

    private static int wordBoundaryAfter(String text, int from) {
        int at = from;

        while (at > 0 && at < text.length() && !Character.isWhitespace(text.charAt(at))) {
            at++;
        }

        return at;
    }

    private static int wordBoundaryBefore(String text, int until) {
        int at = until;

        while (at > 0 && at < text.length() && !Character.isWhitespace(text.charAt(at))) {
            at--;
        }

        return at;
    }

    /**
     * The default cleaner: one window's Markdown decoration taken off.
     *
     * <p>⚠️ <strong>Deliberately blunt.</strong> A summariser can walk a document line by line and know
     * about fences and front matter, because it is describing the whole thing. This is a hundred
     * characters cut out of the middle of anything — a table row, half a code fence, a link — so what it
     * can honestly do is remove the punctuation that carries no words, and stop there.
     */
    public static String withoutMarkdown(String window) {
        return window
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1")
                .replaceAll("[#>*_`~|]", " ");
    }

    /** For text whose punctuation is the value. Whitespace is still collapsed. */
    public static String asItIs(String window) {
        return window;
    }

}
